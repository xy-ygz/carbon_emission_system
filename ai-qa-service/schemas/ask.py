from typing import Optional

from pydantic import BaseModel, Field


class AskRequest(BaseModel):
    question: str = Field(..., min_length=1, description="用户问题")
    session_id: Optional[str] = Field(None, description="会话ID，用于多轮记忆")
    user_id: Optional[str] = Field(
        None,
        description="用户ID（与 session_id 组合隔离记忆；建议前端传登录用户主键）",
    )


class AskResponse(BaseModel):
    code: int = 200
    data: str = ""
    message: Optional[str] = None
    session_id: Optional[str] = None
    thinking: Optional[str] = Field(
        None,
        description="模型回答前的执行步骤说明（JSON 字符串，供前端展示）",
    )
