from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse

from schemas.ask import AskRequest
from services.qa_service import stream_answer_chunks

router = APIRouter(tags=["streaming-http"])


@router.post("/ask/stream")
def ask_stream(request: AskRequest):
    question = request.question.strip()
    if not question:
        raise HTTPException(status_code=400, detail="问题不能为空")
    return StreamingResponse(
        stream_answer_chunks(question),
        media_type="text/markdown; charset=utf-8",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
