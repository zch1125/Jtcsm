# Jtcsm 今天吃什么

基于 AI 的智能菜谱推荐微信小程序。后端采用 Java + Spring Boot，前端采用 Vue 3 + uni-app，核心能力是 RAG 语义搜索与多轮对话 AI 厨艺导师。

## 功能特性

- 菜谱查询：图文分步教程、多条件筛选（菜系 / 难度 / 烹饪方式）
- AI 厨艺导师：基于 DeepSeek 的菜谱生成与多轮对话，结果经过 RAG 检索增强
- RAG 语义搜索：语义向量检索与关键词检索多路召回
- 个性化推荐：每日推荐、按食材匹配、用户偏好加权
- 用户体系：微信登录、偏好设置、收藏、浏览历史、搜索历史
- 管理后台：菜谱管理、用户管理、数据看板、系统监控

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5、MyBatis-Plus、Spring AI |
| 数据与存储 | MySQL 8.0、Redis 7.2、Elasticsearch 8.15 |
| AI 模型 | DeepSeek 对话模型、text-embedding-v3 文本向量模型 |
| 小程序 | uni-app、Vue 3、Pinia |
| 管理后台 | Vue 3、Vite、Element Plus |
| 部署 | Docker Compose、Nginx |

## 目录结构

```text
backend/
  jtcsm-parent/    父 POM 与依赖版本管理
  jtcsm-common/    公共模块（统一响应、JWT、Redis、MyBatis-Plus 等）
  jtcsm-api/       小程序 API，对外提供 RESTful 接口
  jtcsm-admin/     管理后台 API
  jtcsm-gateway/   网关模块
frontend/
  jtcsm-miniapp/   微信小程序（uni-app）
  jtcsm-web/       管理后台（Vue 3 + Element Plus）
docs/              数据库、接口、RAG 架构等设计文档
scripts/           数据抓取、ES 索引等辅助脚本
```

## 快速开始

### 环境要求

- JDK 21、Maven 3.9+
- Node.js 18+
- MySQL 8.0、Redis 7.2、Elasticsearch 8.15

### 初始化数据库

```bash
mysql -u root -p < docs/sql/01_init.sql
mysql -u root -p < docs/sql/02_seed.sql
```

### 配置环境变量

后端通过环境变量注入敏感配置，请勿将真实密钥写入仓库：

```bash
export MYSQL_URL=jdbc:mysql://localhost:3306/jtcsm
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your-password
export REDIS_HOST=localhost
export DEEPSEEK_API_KEY=your-deepseek-key
export EMBEDDING_API_KEY=your-embedding-key
export EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
export EMBEDDING_MODEL=text-embedding-v3
export JWT_SECRET=your-jwt-secret
```

### 启动后端

```bash
cd backend/jtcsm-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

`jtcsm-admin` 使用相同方式启动，默认端口如下：

- `jtcsm-api`：8081
- `jtcsm-admin`：8082
- `jtcsm-gateway`：8080

### 启动小程序

```bash
cd frontend/jtcsm-miniapp
npm install
npm run dev:mp-weixin
```

### 启动管理后台

```bash
cd frontend/jtcsm-web
npm install
npm run dev
```

### Docker 一键启动

```bash
docker compose up -d
```

## 接口规范

- 接口路径前缀为 `/api/v1`，采用 RESTful 风格
- 统一响应格式：`{ "code": 200, "msg": "success", "data": {} }`
- 鉴权使用 JWT，密钥通过环境变量 `JWT_SECRET` 注入

## 项目文档

- [数据库设计](docs/01_数据库设计.md)
- [接口文档](docs/02_接口文档.md)
- [RAG 架构设计](docs/03_RAG架构设计.md)

## 安全说明

- 禁止硬编码 API Key、密码等敏感信息
- 所有密钥均从环境变量或本地私有配置读取
- `.env`、`*.local.*`、`logs/` 等文件已通过 `.gitignore` 排除
