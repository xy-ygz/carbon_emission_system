# AI 智能问答微服务

北京林业大学碳排放核算与管理系统 - 智能问答 RAG 微服务（Python）

## 目录结构

代码直接放在本目录 **`ai-qa-service/`** 根下（不再套一层同名 Python 包）。

| 路径 | 说明 |
|------|------|
| `main.py` | ASGI 入口，`create_app()` / `app` |
| `api/routers/` | HTTP 路由（仅编排） |
| `services/` | 领域服务（RAG、智谱调用等） |
| `schemas/` | Pydantic 请求/响应模型 |
| `core/` | 配置等横切能力 |

路由文件：`question_answer.py`（同步）、`streaming_http.py`（HTTP 分块流式，非 WebSocket）。

## 功能

- 从主后端 API 获取学校概况、碳排放数据作为上下文
- 调用智谱 GLM 大模型生成回答
- 提供普通 `/ask` 与流式 `/ask/stream` 两种接口

## 环境变量

在 `docker` 目录下创建 `.env` 文件，或通过 `docker-compose` 传入：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| LLM_API_KEY | 智谱 API Key（必填） | - |
| LLM_ENABLED | 是否启用大模型 | true |
| LLM_BASE_URL | 智谱接口地址 | https://open.bigmodel.cn/api/paas/v4/chat/completions |
| LLM_MODEL | 模型名 | glm-4.5-air |
| LLM_MAX_TOKENS | 最大 token 数 | 2048 |
| BACKEND_BASE_URL | 主后端 API 地址 | http://carbon-backend:8080/api |
| EMBEDDING_ENABLED | 是否启用向量检索 | true |
| EMBEDDING_MODEL | 智谱 embedding 模型 | embedding-2 |
| VECTOR_TOP_K | 检索最相关的块数量 | 5 |
| VECTOR_CACHE_TTL | 块/向量缓存时间（秒） | 300 |

## 本地运行

```bash
cd ai-qa-service
pip install -r requirements.txt
export LLM_API_KEY=your-api-key
uvicorn main:app --host 0.0.0.0 --port 8000
```

## Docker 部署

由 `docker/deploy.sh` 一键部署，或手动：

```bash
cd docker
# 创建 .env 并设置 LLM_API_KEY=your-key
docker-compose up -d --build
```
