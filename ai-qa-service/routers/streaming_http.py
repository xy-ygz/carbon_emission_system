from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse

from schemas.ask import AskRequest
from services.conversation_store import conversation_store
from services.qa_service import stream_answer_markdown

router = APIRouter(tags=["streaming-http"])


@router.post("/ask/stream")
def ask_stream(request: AskRequest):
    question = request.question.strip()
    if not question:
        raise HTTPException(status_code=400, detail="问题不能为空")
    cid = (request.conversation_id or "").strip() or conversation_store.new_id()
    return StreamingResponse(
        stream_answer_markdown(question, cid),
        media_type="text/markdown; charset=utf-8",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
            "X-Conversation-Id": cid,
            "Access-Control-Expose-Headers": "X-Conversation-Id",
        },
    )
