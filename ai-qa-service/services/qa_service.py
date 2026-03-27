"""智能问答：上下文拉取、向量检索、多轮对话、智谱 GLM 同步/流式调用。"""

import logging
from typing import Any, Dict, Generator, List, Optional, Tuple

import httpx
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableLambda, RunnableParallel
from langchain_openai import ChatOpenAI

from config.config import settings
from rag.simple_rag import get_relevant_context as simple_get_relevant_context
from services.conversation_store import conversation_store
from services.think_stream import THINK_CLOSE, THINK_OPEN, split_think_answer_full

logger = logging.getLogger(__name__)


def _normalize_base_url(url: str, endpoint_suffix: str) -> str:
    """将可能的全路径 endpoint 转换为 OpenAI 兼容 base_url。"""
    from urllib.parse import urlparse, urlunparse

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
    return simple_get_relevant_context(question=question, top_k=top_k)


def _format_history_block(turns: List[Tuple[str, str]]) -> str:
    if not turns:
        return "（无）"
    parts = []
    for i, (u, a) in enumerate(turns, 1):
        parts.append(f"第{i}轮\n用户：{u}\n助手：{a}\n")
    return "\n".join(parts)


def _retrieval_query(question: str, turns: List[Tuple[str, str]]) -> str:
    """追问、短句时合并上一轮用户表述，便于向量检索命中。"""
    q = (question or "").strip()
    if not turns:
        return q
    prev_u = turns[-1][0].strip()
    if len(q) < 48 and prev_u:
        return f"{prev_u}\n追问：{q}"
    return q


def _build_rag_chain(streaming: bool = False):
    llm = _build_chat_model(streaming=streaming)
    prompt = ChatPromptTemplate.from_template(
        "你是北京林业大学碳排放核算与管理系统的智能助手，只能基于我提供的系统数据进行回答，不要编造没有的数据。"
        "如果系统数据中没有相关信息，请明确向用户说明。\n\n"
        "【历史对话（最近至多5轮）】\n"
        "{history_block}\n"
        "若当前问题与历史无关，可忽略历史，仅依据下方系统数据回答。\n\n"
        "=== 系统数据开始 ===\n"
        "{context}\n"
        "=== 系统数据结束 ===\n\n"
        "用户问题：{question}\n\n"
        "请先输出 {think_open}，紧接着写你的分析步骤与检索要点（不超过约300字），"
        "然后输出 {think_close}；两标记之间仅写思考内容。"
        "之后再输出正式回答：使用简体中文、面向学校管理人员，使用 Markdown 格式"
        "（适当使用 ## 标题、- 列表、**加粗**、`代码` 等），结构清晰、便于阅读。"
        "正式回答必须出现在 {think_close} 之后。"
    )
    return (
        RunnableParallel(
            context=RunnableLambda(
                lambda x: get_relevant_context(x["retrieval_query"], top_k=settings.VECTOR_TOP_K)
            ),
            question=RunnableLambda(lambda x: x["question"]),
            history_block=RunnableLambda(lambda x: x["history_block"]),
            think_open=RunnableLambda(lambda x: THINK_OPEN),
            think_close=RunnableLambda(lambda x: THINK_CLOSE),
        )
        | prompt
        | llm
        | StrOutputParser()
    )


def _chain_input(question: str, turns: List[Tuple[str, str]]) -> Dict[str, Any]:
    return {
        "question": question,
        "retrieval_query": _retrieval_query(question, turns),
        "history_block": _format_history_block(turns),
    }


def call_llm(question: str, turns: List[Tuple[str, str]]) -> str:
    if not settings.LLM_API_KEY:
        raise ValueError("未配置 LLM_API_KEY")
    chain = _build_rag_chain(streaming=False)
    return chain.invoke(_chain_input(question, turns))


def call_llm_stream(question: str, turns: List[Tuple[str, str]]):
    if not settings.LLM_API_KEY:
        raise ValueError("未配置 LLM_API_KEY")
    chain = _build_rag_chain(streaming=True)
    for chunk in chain.stream(_chain_input(question, turns)):
        if chunk:
            yield chunk


def answer_question_sync(question: str, conversation_id: Optional[str]) -> Tuple[int, str, Optional[str], Optional[str], str]:
    """(code, answer, message, thinking, conversation_id)"""
    cid = (conversation_id or "").strip() or conversation_store.new_id()
    turns = conversation_store.get_turns(cid)

    if not settings.LLM_ENABLED:
        context = get_relevant_context(question, top_k=settings.VECTOR_TOP_K)
        body = (
            "当前服务未启用大模型（LLM_ENABLED=false），请联系管理员配置。\n\n系统上下文（供调试）：\n"
            + context
        )
        return 200, body, None, None, cid

    try:
        raw = call_llm(question, turns)
        think, ans = split_think_answer_full(raw)
        if not ans.strip():
            ans = raw.strip()
        conversation_store.append(cid, question, ans)
        return 200, ans, None, think or None, cid
    except httpx.TimeoutException as e:
        logger.error("调用大模型接口超时: %s", e)
        return (
            200,
            "调用大模型接口失败：大模型响应超时，请稍后重试，或简化问题内容后再次尝试。",
            None,
            None,
            cid,
        )
    except Exception as e:
        logger.error("调用大模型接口失败: %s", e)
        return 200, f"调用大模型接口失败：{str(e)}", None, None, cid


def stream_answer_markdown(question: str, cid: str) -> Generator[str, None, None]:
    """text/markdown 流：与模型输出一致（含思考标记），结束后写入会话。"""
    turns = conversation_store.get_turns(cid)

    if not settings.LLM_ENABLED:
        msg = "当前服务未启用大模型（LLM_ENABLED=false），请联系管理员配置。"
        yield msg
        conversation_store.append(cid, question, msg)
        return

    acc: List[str] = []
    try:
        for chunk in call_llm_stream(question, turns):
            acc.append(chunk or "")
            yield chunk or ""
        raw_text = "".join(acc)
        if raw_text.strip():
            _, ans = split_think_answer_full(raw_text)
            if not (ans or "").strip():
                ans = raw_text.strip()
            conversation_store.append(cid, question, ans)
    except httpx.TimeoutException:
        yield "\n\n[调用大模型超时，请稍后重试或简化问题。]"
    except Exception as e:
        logger.error("流式调用失败: %s", e)
        yield f"\n\n[调用失败：{str(e)}]"

