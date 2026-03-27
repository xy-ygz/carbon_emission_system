from typing import Optional

from pydantic import BaseModel, Field


class AskRequest(BaseModel):
    question: str = Field(..., min_length=1, description="用户问题")
    conversation_id: Optional[str] = Field(None, description="会话 ID，不传则新建；多轮对话需回传上一轮的值")


class AskResponse(BaseModel):
    code: int = 200
    data: str = ""
    message: Optional[str] = None
    thinking: Optional[str] = Field(None, description="模型思考过程（同步接口解析）")
    conversation_id: Optional[str] = Field(None, description="本会话 ID，下次请求请回传")
