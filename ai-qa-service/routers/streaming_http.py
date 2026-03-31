from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse

from schemas.ask import AskRequest
from services.qa_service import stream_answer_events

router = APIRouter(tags=["streaming-http"])


@router.post("/ask/stream")
def ask_stream(request: AskRequest):
    question = request.question.strip()
    if not question:
        raise HTTPException(status_code=400, detail="问题不能为空")
    return StreamingResponse(
        stream_answer_events(question, request.session_id, request.user_id),
        media_type="application/x-ndjson; charset=utf-8",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
