# Nacos 配置中心

## 1. 概述

后端三个服务（`jtcsm-api`、`jtcsm-admin`、`jtcsm-gateway`）的本地 `application*.yml` 已统一迁移到 Nacos，本地只保留 Nacos 引导配置。

选用版本：

- Nacos Server：`2.4.3`
- Spring Cloud Alibaba：`2023.0.3.4`
- 存储方式：standalone 模式，配置数据持久化到 MySQL 的 `nacos_config` 库

## 2. 部署 Nacos

```bash
docker compose up -d mysql nacos-db-init nacos
```

说明：

- `mysql` 为业务数据库，`nacos-db-init` 会自动创建 `nacos_config` 库并初始化表与默认管理员账号。
- 如果宿主机 3306 已被本机 MySQL 占用，可指定 `MYSQL_PORT` 避免端口冲突：

```bash
MYSQL_PORT=3307 docker compose up -d mysql nacos-db-init nacos
```

- Nacos 控制台：<http://localhost:8848/nacos>
- 默认管理员账号：`nacos / nacos`，首次部署后请立即修改密码。
- Nacos 已开启鉴权，可通过环境变量覆盖默认账号：

```bash
NACOS_USERNAME=admin NACOS_PASSWORD=your-password docker compose up -d nacos
```

> 注意：Nacos 客户端需要同时开放 `8848`（HTTP）和 `9848`（gRPC）端口，compose 中已配置。

## 3. 导入配置

配置源文件位于 `deploy/nacos/configs/`，修改后执行：

```bash
python deploy/nacos/import_configs.py
```

脚本默认连接 `http://localhost:8848` 并使用 `nacos / nacos` 登录；如需指定其他地址或账号：

```bash
NACOS_SERVER=http://127.0.0.1:8848 \
NACOS_USERNAME=nacos \
NACOS_PASSWORD=nacos \
python deploy/nacos/import_configs.py
```

脚本会逐条发布并回读校验，全部成功返回 0。

## 4. Data ID 清单

所有配置均使用 `DEFAULT_GROUP`，数据格式为 YAML：

| 模块 | 基础配置 | 环境配置 |
| --- | --- | --- |
| jtcsm-api | `jtcsm-api.yaml` | `jtcsm-api-dev.yaml`、`jtcsm-api-uat.yaml`、`jtcsm-api-pro.yaml` |
| jtcsm-admin | `jtcsm-admin.yaml` | `jtcsm-admin-dev.yaml`、`jtcsm-admin-uat.yaml`、`jtcsm-admin-pro.yaml` |
| jtcsm-gateway | `jtcsm-gateway.yaml` | `jtcsm-gateway-dev.yaml`、`jtcsm-gateway-uat.yaml`、`jtcsm-gateway-pro.yaml` |

## 5. 应用接入

本地 `application.yml` 仅保留：

```yaml
spring:
  application:
    name: jtcsm-api
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
  config:
    import:
      - "nacos:jtcsm-api.yaml?refreshEnabled=true&group=DEFAULT_GROUP"
```

profile 对应的 Data ID 通过 `spring.config.activate.on-profile` 分段加载。

本地启动前需要确保 Nacos 已启动并完成配置导入：

```bash
export NACOS_SERVER_ADDR=localhost:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
export SPRING_PROFILES_ACTIVE=dev
cd backend/jtcsm-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 6. 敏感信息

- Nacos 配置中只保留 `${ENV:default}` 占位符，真实密钥通过环境变量注入。
- `deploy/nacos/configs/` 是配置的版本化副本，禁止写入真实密码、Token、API Key。
- 默认管理员 `nacos / nacos` 仅用于本地开发，生产环境必须通过 `NACOS_USERNAME`、`NACOS_PASSWORD` 覆盖。
