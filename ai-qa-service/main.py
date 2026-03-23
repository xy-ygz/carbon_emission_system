"""
ASGI 入口：创建 FastAPI 应用实例。

运行：uvicorn main:app --host 0.0.0.0 --port 8000
（工作目录为本服务根目录 ai-qa-service/）
"""
import logging

from fastapi import FastAPI

from api.api import api_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)


def create_app() -> FastAPI:
    application = FastAPI(title="AI智能问答服务", version="1.0.0")
    application.include_router(api_router)
    return application


app = create_app()
