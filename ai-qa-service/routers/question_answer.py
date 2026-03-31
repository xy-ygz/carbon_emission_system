from fastapi import APIRouter, HTTPException

from schemas.ask import AskRequest, AskResponse
from services.qa_service import answer_question_sync

router = APIRouter(tags=["question-answer"])


@router.post("/ask", response_model=AskResponse)
def ask(request: AskRequest) -> AskResponse:
    question = request.question.strip()
    if not question:
        raise HTTPException(status_code=400, detail="问题不能为空")
    code, data, message, session_id, thinking = answer_question_sync(
        question, request.session_id, request.user_id
    )
    return AskResponse(code=code, data=data, message=message, session_id=session_id, thinking=thinking)


@router.get("/health")
def health():
    return {"status": "ok", "service": "ai-qa-service"}
