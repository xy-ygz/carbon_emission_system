"""应用配置：仅从进程环境变量读取（Docker Compose 注入），不使用 .env 文件。"""
import os

class Settings:
    BACKEND_BASE_URL = os.getenv("BACKEND_BASE_URL", "http://carbon-backend:8080/api")
    # 对外展示给用户的 API 基址（用于回答里拼“下载链接/接口地址”），与服务间调用解耦
    PUBLIC_BACKEND_BASE_URL = os.getenv("PUBLIC_BACKEND_BASE_URL", BACKEND_BASE_URL)
    LLM_ENABLED = os.getenv("LLM_ENABLED", "true").lower() == "true"
    LLM_BASE_URL = os.getenv(
        "LLM_BASE_URL",
        "https://open.bigmodel.cn/api/paas/v4/chat/completions",
    )
    LLM_API_KEY = os.getenv("LLM_API_KEY", "")
    LLM_MODEL = os.getenv("LLM_MODEL", "glm-4.5-air")
    LLM_MAX_TOKENS = int(os.getenv("LLM_MAX_TOKENS", "2048"))
    # reasoning/思考链属于“可选增强”，不同 OpenAI 兼容网关/SDK 是否支持不一致。
    # 默认关闭，避免传递不支持的参数导致调用失败。
    LLM_REASONING_ENABLED = os.getenv("LLM_REASONING_ENABLED", "false").lower() == "true"

    EMBEDDING_ENABLED = os.getenv("EMBEDDING_ENABLED", "true").lower() == "true"
    EMBEDDING_BASE_URL = os.getenv(
        "EMBEDDING_BASE_URL",
        "https://open.bigmodel.cn/api/paas/v4/embeddings",
    )
    EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "embedding-2")
    VECTOR_TOP_K = int(os.getenv("VECTOR_TOP_K", "3"))
    VECTOR_CACHE_TTL = int(os.getenv("VECTOR_CACHE_TTL", "300"))
    QA_MEMORY_ROUNDS = int(os.getenv("QA_MEMORY_ROUNDS", "5"))
    QA_SESSION_DB_PATH = os.getenv("QA_SESSION_DB_PATH", "/tmp/ai_qa_sessions.db")
    # thinking 流式展示节流，优化thinking 流式体验
    THINKING_STREAM_MIN_MS = int(os.getenv("THINKING_STREAM_MIN_MS", "1200"))
    THINKING_STREAM_CHUNK_DELAY_MS = int(os.getenv("THINKING_STREAM_CHUNK_DELAY_MS", "2"))


settings = Settings()
