from fastapi import APIRouter, HTTPException

from schemas.ask import AskRequest, AskResponse
from services.qa_service import answer_question_sync

router = APIRouter(tags=["question-answer"])


@router.post("/ask", response_model=AskResponse)
def ask(request: AskRequest) -> AskResponse:
    question = request.question.strip()
    if not question:
        raise HTTPException(status_code=400, detail="问题不能为空")
    code, data, message, thinking, cid = answer_question_sync(question, request.conversation_id)
    return AskResponse(code=code, data=data, message=message, thinking=thinking, conversation_id=cid)


@router.get("/health")
def health():
    return {"status": "ok", "service": "ai-qa-service"}
