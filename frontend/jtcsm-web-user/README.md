# 今天吃什么 · 用户端网页版

与微信小程序共用同一套后端接口与用户数据：收藏、浏览历史、搜索历史、AI 生成记录都在后端按用户存储，小程序和网页登录同一个账号即可互通。

## 启动

```bash
cd frontend/jtcsm-web-user
npm install
npm run dev
```

访问 `http://localhost:5174`。开发环境通过 Vite 代理把 `/api/v1` 和 `/static` 转发到 `http://localhost:8081`，因此后端 `jtcsm-api` 需要先启动。

## 账号说明

当前后端支持 `code=mock` 的模拟登录，网页版默认使用同一个 mock 账号，因此与小程序开发环境的模拟账号共享数据。后续接入微信真实登录时，网页版可改为账号密码或扫码登录，用户表不变即可继续互通。
