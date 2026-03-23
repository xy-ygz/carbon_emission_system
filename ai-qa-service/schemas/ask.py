from typing import Optional

from pydantic import BaseModel, Field


class AskRequest(BaseModel):
    question: str = Field(..., min_length=1, description="用户问题")


class AskResponse(BaseModel):
    code: int = 200
    data: str = ""
    message: Optional[str] = None
