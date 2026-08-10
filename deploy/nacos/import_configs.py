#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将 deploy/nacos/configs 下的 YAML 配置批量导入 Nacos 配置中心。

用法：
    python deploy/nacos/import_configs.py
    NACOS_SERVER=http://127.0.0.1:8848 NACOS_USERNAME=nacos NACOS_PASSWORD=nacos \
        python deploy/nacos/import_configs.py
"""

import argparse
import json
import os
import sys
import urllib.parse
import urllib.request
import urllib.error
from pathlib import Path


def read_text(path):
    """以 UTF-8 读取配置文件，并统一换行为 LF，避免 CRLF 干扰校验。"""
    return Path(path).read_text(encoding="utf-8").replace("\r\n", "\n")


def request_json(url, data=None, method=None, timeout=15):
    """发送请求并返回 JSON 响应。"""
    body = urllib.parse.urlencode(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Content-Type", "application/x-www-form-urlencoded")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8")
            try:
                return resp.status, json.loads(raw)
            except json.JSONDecodeError:
                return resp.status, raw
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", "replace")
        try:
            return exc.code, json.loads(raw)
        except json.JSONDecodeError:
            return exc.code, raw


def login(server, username, password):
    """登录 Nacos，返回 accessToken。"""
    url = f"{server}/nacos/v1/auth/login"
    status, resp = request_json(url, data={"username": username, "password": password})
    if status != 200 or not isinstance(resp, dict) or not resp.get("accessToken"):
        raise RuntimeError(f"Nacos 登录失败: status={status}, body={resp}")
    return resp["accessToken"]


def publish_config(server, token, group, data_id, content):
    """发布单条配置，成功返回 True。"""
    url = f"{server}/nacos/v1/cs/configs"
    if token:
        url += f"?accessToken={urllib.parse.quote(token)}"
    data = {
        "dataId": data_id,
        "group": group,
        "content": content,
        "type": "yaml",
    }
    status, resp = request_json(url, data=data, method="POST")
    if status != 200 or resp is not True:
        raise RuntimeError(f"发布 {data_id} 失败: status={status}, body={resp}")
    return True


def read_config(server, token, group, data_id):
    """读取单条配置内容，不存在时返回 None。"""
    query = urllib.parse.urlencode({"dataId": data_id, "group": group})
    url = f"{server}/nacos/v1/cs/configs?{query}"
    if token:
        url += f"&accessToken={urllib.parse.quote(token)}"
    status, resp = request_json(url, method="GET")
    if status == 200:
        return (resp.replace("\r\n", "\n")
                if isinstance(resp, str)
                else json.dumps(resp, ensure_ascii=False))
    if status == 404:
        return None
    raise RuntimeError(f"读取 {data_id} 失败: status={status}, body={resp}")


def main():
    parser = argparse.ArgumentParser(description="批量导入 Nacos 配置")
    parser.add_argument("--server", default=os.getenv("NACOS_SERVER", "http://localhost:8848"))
    parser.add_argument("--group", default=os.getenv("NACOS_GROUP", "DEFAULT_GROUP"))
    parser.add_argument("--username", default=os.getenv("NACOS_USERNAME", "nacos"))
    parser.add_argument("--password", default=os.getenv("NACOS_PASSWORD", "nacos"))
    parser.add_argument(
        "--configs-dir",
        default=str(Path(__file__).resolve().parent / "configs"),
        help="存放 YAML 配置的目录",
    )
    args = parser.parse_args()

    server = args.server.rstrip("/")
    configs_dir = Path(args.configs_dir)
    files = sorted(configs_dir.glob("*.yaml"))
    if not files:
        print(f"未在 {configs_dir} 找到任何 *.yaml 配置")
        return 1

    # 默认尝试登录；若 Nacos 未开启鉴权，登录失败时继续以匿名方式导入
    token = None
    try:
        token = login(server, args.username, args.password)
        print(f"Nacos 登录成功: {server}")
    except RuntimeError as exc:
        print(f"警告: {exc}，将尝试匿名导入（适用于未开启鉴权的 Nacos）")

    failed = []
    for file in files:
        data_id = file.name
        content = read_text(file)
        try:
            publish_config(server, token, args.group, data_id, content)
            actual = read_config(server, token, args.group, data_id)
            if actual == content:
                print(f"OK  {data_id}")
            else:
                print(f"FAIL {data_id}: 导入后校验不一致")
                failed.append(data_id)
        except RuntimeError as exc:
            print(f"FAIL {data_id}: {exc}")
            failed.append(data_id)

    if failed:
        print(f"导入完成，失败 {len(failed)} 个: {', '.join(failed)}")
        return 1
    print(f"全部 {len(files)} 个配置导入成功")
    return 0


if __name__ == "__main__":
    sys.exit(main())
