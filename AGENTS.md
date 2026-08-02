# AGENTS.md - 今天吃什么小程序

## 项目概述
一款基于AI的智能菜谱推荐微信小程序。后端Java + Spring Boot，前端Vue 3 + uni-app。
核心卖点：完整版RAG语义搜索 + 多轮对话AI厨艺导师 + 会员付费体系。

## 技术栈（严格遵守）
### 后端
- Java 21 LTS + Spring Boot 3.5.x
- MyBatis-Plus 3.5.x（ORM）
- Spring AI 1.0.0-M7+（AI编排）
- MySQL 8.0.x（业务数据）
- Pgvector 0.8.x（向量存储，与MySQL共用）
- Redis 7.2.x（缓存 + 会话）
- Elasticsearch 8.15.x（关键词检索）
- 微信支付V3 SDK 0.2.17

### 前端（小程序）
- uni-app（跨端，打包微信小程序）
- Vue 3 组合式API
- Pinia（状态管理）
- Element Plus（管理后台）

### AI模型
- DeepSeek-V4（对话模型，通过Codex接入）[reference:4]
- text-embedding-v3（Embedding模型）

## 编码规范
- 后端包结构：com.recipe.{module}.{controller/service/mapper/entity}
- 统一响应格式：{ code, msg, data }
- 所有API使用RESTful风格，路径前缀 /api/v1
- 数据库表名使用下划线命名，字段加前缀（如recipe_）
- 禁止使用Lombok（保持代码可读性）
- 所有AI调用必须经过RAG流水线，禁止直接让LLM生成菜谱

## 禁止事项
- 禁止修改已上线的数据库表结构（先提迁移方案）
- 禁止在Controller中写业务逻辑
- 禁止硬编码敏感信息（API Key、密钥等使用配置）
- 禁止生成单测覆盖率低于80%的代码

## 启动方式
- 后端：mvn spring-boot:run（profile: dev）
- 前端：npm run dev:mp-weixin
- 测试：mvn test

## 完成标准
- 所有接口通过Postman测试
- 单元测试覆盖率 ≥ 80%
- 小程序体验版可正常跑通核心流程
- AI响应时间 P95 < 3s