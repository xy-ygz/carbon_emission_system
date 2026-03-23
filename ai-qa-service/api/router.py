"""统一聚合 API 路由。"""

from fastapi import APIRouter

from routers import question_answer, streaming_http

api_router = APIRouter()
api_router.include_router(question_answer.router)
api_router.include_router(streaming_http.router)
