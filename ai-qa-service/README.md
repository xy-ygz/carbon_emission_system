# AI 智能问答微服务

北京林业大学碳排放核算与管理系统 - 智能问答 RAG 微服务（Python / FastAPI）

## 目录结构

代码位于仓库 **`ai-qa-service/`** 根目录（无再套一层同名包）。

| 路径 | 说明 |
|------|------|
| `main.py` | ASGI 入口，`create_app()` / `app`，挂载统一路由 |
| `api/router.py` | 聚合子路由 |
| `routers/question_answer.py` | 同步 `POST /ask`、`GET /health` |
| `routers/streaming_http.py` | 流式 `POST /ask/stream`（HTTP 分块，非 WebSocket） |
| `services/qa_service.py` | 问答编排（RAG、调用大模型等） |
| `rag/simple_rag.py` | RAG 与向量检索相关逻辑 |
| `schemas/ask.py` | Pydantic 请求/响应模型 |
| `config/config.py` | 从环境变量读取配置（`settings`） |

## 功能

- 从主后端 API 拉取学校概况、碳排放等数据作为上下文
- 智谱 Embedding + GLM 生成回答（可关 LLM / Embedding）
- 对外：`/ask`、`/ask/stream`；健康检查：`/health`

生产环境若经 **Java 后端 Nginx 代理**（`/api/ai/qa/` → Spring 再转发本服务），便于主系统审计日志，见项目根 `README.md`。

## 环境变量

由 **Docker Compose** 或进程环境注入（`config/config.py` 读取，**不使用**仓库内 `.env` 文件）。

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `LLM_API_KEY` | 智谱 API Key（启用 LLM 时必填） | 空 |
| `LLM_ENABLED` | 是否启用大模型 | true |
| `LLM_BASE_URL` | 智谱 Chat Completions 地址 | open.bigmodel.cn …/chat/completions |
| `LLM_MODEL` | 模型名 | glm-4.5-air |
| `LLM_MAX_TOKENS` | 最大 token | 2048 |
| `BACKEND_BASE_URL` | 主后端 JSON API 基址（服务间调用） | http://carbon-backend:8080/api |
| `PUBLIC_BACKEND_BASE_URL` | 回答中展示给用户的 API 基址 | 同 `BACKEND_BASE_URL` |
| `EMBEDDING_ENABLED` | 是否启用向量检索 | true |
| `EMBEDDING_BASE_URL` | 智谱 Embeddings 地址 | open.bigmodel.cn …/embeddings |
| `EMBEDDING_MODEL` | embedding 模型名 | embedding-2 |
| `VECTOR_TOP_K` | 检索条数 | 3 |
| `VECTOR_CACHE_TTL` | 向量/块缓存秒数 | 300 |

## 本地运行

```bash
cd ai-qa-service
pip install -r requirements.txt
export LLM_API_KEY=your-api-key
uvicorn main:app --host 0.0.0.0 --port 8000
```

## Docker 部署

与仓库 **`docker/docker-compose.yml`** 中服务 **`ai-qa`** 一起编排；一键脚本为 **`docker/deploy.sh`**（默认仅 **`docker-compose build ai-qa`**，再 `up -d`，并保留数据卷；勿误用 `down -v` 清空库）。

手动示例：

```bash
cd docker
docker-compose build ai-qa
docker-compose up -d
```

在 `docker-compose.yml` 中配置 `LLM_API_KEY` 等环境变量即可。
