"""智能问答领域服务：支持5轮记忆与流式回答。"""

import json
import logging
import time
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import urlparse, urlunparse

import httpx
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableLambda
from langchain_openai import ChatOpenAI

from config.config import settings
from rag.simple_rag import (
    build_thinking_payload_from_trace,
    get_relevant_context as rag_get_relevant_context,
    get_relevant_context_with_trace,
)
from services.memory_service import (
    compose_memory_key,
    format_rounds_for_prompt,
    get_recent_rounds,
    save_round,
)

logger = logging.getLogger(__name__)


def _normalize_base_url(url: str, endpoint_suffix: str) -> str:
    """将可能的全路径 endpoint 转换为 OpenAI 兼容 base_url。"""
    clean_url = (url or "").strip()
    if not clean_url:
        return clean_url
    if clean_url.endswith(endpoint_suffix):
        return clean_url[: -len(endpoint_suffix)]
    parsed = urlparse(clean_url)
    if parsed.path.endswith(endpoint_suffix):
        new_path = parsed.path[: -len(endpoint_suffix)]
        return urlunparse((parsed.scheme, parsed.netloc, new_path, "", "", ""))
    return clean_url


def _build_chat_model(streaming: bool = False) -> ChatOpenAI:
    """构造用于问答的 ChatOpenAI 客户端（支持流式/非流式）。"""
    base_url = _normalize_base_url(settings.LLM_BASE_URL, "/chat/completions")
    return ChatOpenAI(
        model=settings.LLM_MODEL,
        api_key=settings.LLM_API_KEY,
        base_url=base_url,
        max_tokens=settings.LLM_MAX_TOKENS,
        temperature=0,
        streaming=streaming,
    )


def get_relevant_context(question: str, top_k: Optional[int] = None) -> str:
    """通过 rag/simple_rag.py 生成本次 prompt 的 context。"""
    return rag_get_relevant_context(question=question, top_k=top_k)


def _thinking_json_from_trace(trace: Dict[str, Any], rounds: List[Dict[str, str]]) -> str:
    payload = build_thinking_payload_from_trace(trace, len(rounds))
    return json.dumps(payload, ensure_ascii=False)


def _build_prompt() -> ChatPromptTemplate:
    """构造统一问答提示词模板。"""
    return ChatPromptTemplate.from_template(
        "你是北京林业大学碳排放核算与管理系统的智能助手，只能基于我提供的系统数据进行回答，不要编造没有的数据。"
        "如果系统数据中没有相关信息，请明确向用户说明。\n\n"
        "=== 系统数据开始 ===\n"
        "{context}\n"
        "=== 系统数据结束 ===\n\n"
        "=== 最近5轮历史对话（仅当与当前问题相关时才结合） ===\n"
        "{history}\n"
        "=== 历史对话结束 ===\n\n"
        "用户问题：{question}\n"
        "请用简体中文、面向学校管理人员，使用 Markdown 格式输出（适当使用 ## 标题、- 列表、**加粗**、`代码` 等），"
        "结构清晰、便于阅读。"
    )

def _build_rag_chain(streaming: bool = False):
    """构造 RAG + LLM 链：context -> prompt -> LLM -> text。"""
    llm = _build_chat_model(streaming=streaming)
    prompt = _build_chat_model(streaming=streaming)
    return (
        RunnableLambda(
            lambda x: {
                "context": x["context"],
                "history": x["history"],
                "question": x["question"],
            }
        )
        | prompt
        | llm
        | StrOutputParser()
    )


def call_llm(question: str, context: str, history: str) -> str:
    """同步调用：返回完整答案文本。"""
    if not settings.LLM_API_KEY:
        raise ValueError("未配置 LLM_API_KEY")
    payload = {"question": question, "context": context, "history": history}
    chain = _build_rag_chain(streaming=False)
    msg = chain.invoke(payload)
    answer = getattr(msg, "content", "") or ""
    if isinstance(answer, list):
        answer = "".join([x for x in answer if isinstance(x, str)])
    return str(answer)


def call_llm_stream(question: str, context: str, history: str):
    """流式调用：逐块 yield 回答文本。"""
    if not settings.LLM_API_KEY:
        raise ValueError("未配置 LLM_API_KEY")
    payload = {"question": question, "context": context, "history": history}
    llm = _build_chat_model(streaming=True)
    chain = _build_prompt() | llm
    for chunk in chain.stream(payload):
        if not chunk:
            continue
        answer_chunk = getattr(chunk, "content", "") or ""
        if isinstance(answer_chunk, list):
            answer_chunk = "".join([x for x in answer_chunk if isinstance(x, str)])
        if answer_chunk:
            yield str(answer_chunk)


def answer_question_sync(
    question: str, session_id: Optional[str] = None, user_id: Optional[str] = None
) -> Tuple[int, str, Optional[str], str, Optional[str]]:
    """返回 (code, answer, message, session_id, thinking)。"""
    store_key, client_sid = compose_memory_key(session_id, user_id)
    rounds = get_recent_rounds(store_key, rounds=settings.QA_MEMORY_ROUNDS)
    rounds_for_prompt = format_rounds_for_prompt(rounds)
    context, trace = get_relevant_context_with_trace(question, top_k=settings.VECTOR_TOP_K)
    thinking_json = _thinking_json_from_trace(trace, rounds)

    if not settings.LLM_ENABLED:
        return (
            200,
            "当前服务未启用大模型（LLM_ENABLED=false），请联系管理员配置。\n\n系统上下文（供调试）：\n"
            + context,
            None,
            client_sid,
            thinking_json,
        )

    try:
        answer = call_llm(question, context, rounds_for_prompt)
        save_round(store_key, question, answer)
        return 200, answer, None, client_sid, thinking_json
    except httpx.TimeoutException as e:
        logger.error("调用大模型接口超时: %s", e)
        return (
            200,
            "调用大模型接口失败：大模型响应超时，请稍后重试，或简化问题内容后再次尝试。",
            None,
            client_sid,
            thinking_json,
        )
    except Exception as e:
        logger.error("调用大模型接口失败: %s", e)
        return 200, f"调用大模型接口失败：{str(e)}", None, client_sid, thinking_json


def _event_line(event_type: str, content: str = "", session_id: str = "") -> str:
    payload = {"type": event_type, "content": content}
    if session_id:
        payload["session_id"] = session_id
    return json.dumps(payload, ensure_ascii=False) + "\n"


def _iter_thinking_stream_events(thinking_json: str):
    """将 thinking payload 按「步骤字段」拆成流式事件。"""
    try:
        payload = json.loads(thinking_json or "{}")
        raw_steps = payload.get("steps") if isinstance(payload, dict) else []
    except Exception:
        raw_steps = []
    if not isinstance(raw_steps, list):
        raw_steps = []

    start_ts = time.time()
    delay_ms = int(getattr(settings, "THINKING_STREAM_CHUNK_DELAY_MS", 0) or 0)
    min_ms = int(getattr(settings, "THINKING_STREAM_MIN_MS", 0) or 0)

    empty_steps: List[Dict[str, str]] = [{"title": "", "detail": ""} for _ in raw_steps]
    init_content = json.dumps({"steps": empty_steps}, ensure_ascii=False)
    yield _event_line("thinking_init", content=init_content)

    for idx, step in enumerate(raw_steps):
        if not isinstance(step, dict):
            continue
        title = str(step.get("title") or "")
        detail = str(step.get("detail") or "")
        for ch in title:
            chunk = {"step_index": idx, "field": "title", "char": ch}
            yield _event_line("thinking_chunk", content=json.dumps(chunk, ensure_ascii=False))
            if delay_ms > 0:
                time.sleep(delay_ms / 1000.0)
        for ch in detail:
            chunk = {"step_index": idx, "field": "detail", "char": ch}
            yield _event_line("thinking_chunk", content=json.dumps(chunk, ensure_ascii=False))
            if delay_ms > 0:
                time.sleep(delay_ms / 1000.0)

    # 保证“thinking 从 init 到 done”至少持续 min_ms：避免 UI 内容一瞬间输出完毕无流式体验。
    if min_ms > 0:
        elapsed_ms = int((time.time() - start_ts) * 1000)
        remaining_ms = max(0, min_ms - elapsed_ms)
        if remaining_ms > 0:
            time.sleep(remaining_ms / 1000.0)
    yield _event_line("thinking_done")


def stream_answer_events(
    question: str, session_id: Optional[str] = None, user_id: Optional[str] = None
):
    """流式返回事件：session/thinking/answer/done。"""
    store_key, client_sid = compose_memory_key(session_id, user_id)
    rounds = get_recent_rounds(store_key, rounds=settings.QA_MEMORY_ROUNDS)
    rounds_for_prompt = format_rounds_for_prompt(rounds)
    context, trace = get_relevant_context_with_trace(question, top_k=settings.VECTOR_TOP_K)
    thinking_json = _thinking_json_from_trace(trace, rounds)

    yield _event_line("session", session_id=client_sid)
    # 后端数据进行分片返回，新协议：thinking_init/thinking_chunk/thinking_done
    for line in _iter_thinking_stream_events(thinking_json):
        yield line
    # 兼容旧前端：保留一次性 thinking 事件
    yield _event_line("thinking", content=thinking_json)
    if not settings.LLM_ENABLED:
        answer = "当前服务未启用大模型（LLM_ENABLED=false），请联系管理员配置。"
        save_round(store_key, question, answer)
        yield _event_line("answer", content=answer)
        yield _event_line("done", session_id=client_sid)
        return

    answer = ""
    try:
        for answer_chunk in call_llm_stream(question, context, rounds_for_prompt):
            if answer_chunk:
                answer += answer_chunk
                yield _event_line("answer", content=answer_chunk)
    except httpx.TimeoutException:
        chunk = "\n\n[调用大模型超时，请稍后重试或简化问题。]"
        answer += chunk
        yield _event_line("answer", content=chunk)
    except Exception as e:
        logger.error("流式调用失败: %s", e)
        chunk = f"\n\n[调用失败：{str(e)}]"
        answer += chunk
        yield _event_line("answer", content=chunk)
    finally:
        if answer:
            save_round(store_key, question, answer)
        yield _event_line("done", session_id=client_sid)

