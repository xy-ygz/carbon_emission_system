"""应用配置：仅从进程环境变量读取（Docker Compose 注入），不使用 .env 文件。"""
import os

class Settings:
    BACKEND_BASE_URL = os.getenv("BACKEND_BASE_URL", "http://carbon-backend:8080/api")
    LLM_ENABLED = os.getenv("LLM_ENABLED", "true").lower() == "true"
    LLM_BASE_URL = os.getenv(
        "LLM_BASE_URL",
        "https://open.bigmodel.cn/api/paas/v4/chat/completions",
    )
    LLM_API_KEY = os.getenv("LLM_API_KEY", "")
    LLM_MODEL = os.getenv("LLM_MODEL", "glm-4.5-air")
    LLM_MAX_TOKENS = int(os.getenv("LLM_MAX_TOKENS", "2048"))

    EMBEDDING_ENABLED = os.getenv("EMBEDDING_ENABLED", "true").lower() == "true"
    EMBEDDING_BASE_URL = os.getenv(
        "EMBEDDING_BASE_URL",
        "https://open.bigmodel.cn/api/paas/v4/embeddings",
    )
    EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "embedding-2")
    VECTOR_TOP_K = int(os.getenv("VECTOR_TOP_K", "5"))
    VECTOR_CACHE_TTL = int(os.getenv("VECTOR_CACHE_TTL", "300"))


settings = Settings()
