# KnowledgeBaseChatSpringAI

基于 Spring AI + Spring Boot 4.x 的知识库聊天系统，支持多轮对话、RAG 检索增强生成、文档解析与向量检索。

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 4.1.0-RC1 |
| Spring AI | 2.0.0-M8 |
| Java | 25 |
| ORM | Jimmer 0.10.7 |
| 鉴权 | Sa-Token 1.45.0 |
| 数据库 | MySQL + Redis + Neo4j |
| 向量库 | Redis Stack |
| 前端 | Vue 3 + TypeScript + Vite |

## 快速开始

### 1. 配置 AI 供应商（OpenAI 兼容）

编辑 `application.yml`，设置 DashScope（阿里通义千问）API Key：

```yaml
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        options:
          model: qwen-max
```

### 2. 启动基础设施

- MySQL: 创建数据库 `knowledge_base`
- Redis: 需 Redis Stack（支持 Search 和 JSON 模块）
- Neo4j（可选）

### 3. 运行

```bash
mvn spring-boot:run
```

前端：

```bash
cd front-end
npm install
npm run dev
```

## 项目结构

```
src/main/java/io/github/qifan777/knowledge/
├── ai/
│   ├── message/          # 聊天消息 CRUD + AI 对话（SSE 流式）
│   └── session/          # 会话管理
├── demo/                 # 演示：文档解析、RAG、函数调用
├── infrastructure/
│   ├── config/           # Redis/Jackson/WebMVC/Sa-Token 配置
│   └── jimmer/           # 基础实体、拦截器、ID 生成
├── user/                 # 用户注册/登录
└── ServerApplication.java
```

## API 端点

| 路径 | 说明 |
|------|------|
| `POST /message/chat` | SSE 流式对话（带历史记忆） |
| `GET /demo/message/chat` | 非流式问答 |
| `GET /demo/message/chat/stream` | 流式问答 |
| `GET /demo/message/chat/stream/rag` | 流式问答 + 向量检索 |
| `GET /demo/message/chat/stream/function` | 流式问答 + 函数调用 |
| `POST /demo/document/etl/*` | 文档读取/分片/向量化 |
| `POST /demo/document/query` | 向量相似度检索 |
| `POST /demo/document/embedding` | 文本转向量 |
