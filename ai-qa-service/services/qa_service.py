"""智能问答领域服务：上下文拉取、向量检索、智谱 GLM 同步/流式调用。"""

"""
    LLM 调用使用 LangChain（ChatOpenAI），支持同步/流式
    RAG context 由 `RAG/simple_rag.py` 生成
"""
import logging
from typing import Optional, Tuple
from urllib.parse import urlparse, urlunparse

import httpx
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableLambda, RunnableParallel
from langchain_openai import ChatOpenAI

from config.config import settings
from rag.simple_rag import get_relevant_context as simple_get_relevant_context

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
    return simple_get_relevant_context(question=question, top_k=top_k)


def _build_rag_chain(streaming: bool = False):
    """构造 RAG + LLM 链：context -> prompt -> LLM -> text。"""
    llm = _build_chat_model(streaming=streaming)
    prompt = ChatPromptTemplate.from_template(
        "你是北京林业大学碳排放核算与管理系统的智能助手，只能基于我提供的系统数据进行回答，不要编造没有的数据。"
        "如果系统数据中没有相关信息，请明确向用户说明。\n\n"
        "=== 系统数据开始 ===\n"
        "{context}\n"
        "=== 系统数据结束 ===\n\n"
        "用户问题：{question}\n"
        "请用简体中文、面向学校管理人员，使用 Markdown 格式输出（适当使用 ## 标题、- 列表、**加粗**、`代码` 等），"
        "结构清晰、便于阅读。"
    )
    return (
        RunnableParallel(
            context=RunnableLambda(lambda x: get_relevant_context(x["question"], top_k=settings.VECTOR_TOP_K)),
            question=RunnableLambda(lambda x: x["question"]),
        )
        | prompt
        | llm
        | StrOutputParser()
    )


def call_llm(question: str) -> str:
    """同步调用：返回完整答案文本。"""
    if not settings.LLM_API_KEY:
        raise ValueError("未配置 LLM_API_KEY")
    chain = _build_rag_chain(streaming=False)
    return chain.invoke({"question": question})


def call_llm_stream(question: str):
    """流式调用：逐块 yield 输出内容。"""
    if not settings.LLM_API_KEY:
        raise ValueError("未配置 LLM_API_KEY")
    chain = _build_rag_chain(streaming=True)
    for chunk in chain.stream({"question": question}):
        if chunk:
            yield chunk


def answer_question_sync(question: str) -> Tuple[int, str, Optional[str]]:
    """返回 (http风格业务code, 回答正文, 可选 message)。"""
    if not settings.LLM_ENABLED:
        context = get_relevant_context(question, top_k=settings.VECTOR_TOP_K)
        return (
            200,
            "当前服务未启用大模型（LLM_ENABLED=false），请联系管理员配置。\n\n系统上下文（供调试）：\n"
            + context,
            None,
        )

    try:
        answer = call_llm(question)
        return 200, answer, None
    except httpx.TimeoutException as e:
        logger.error("调用大模型接口超时: %s", e)
        return (
            200,
            "调用大模型接口失败：大模型响应超时，请稍后重试，或简化问题内容后再次尝试。",
            None,
        )
    except Exception as e:
        logger.error("调用大模型接口失败: %s", e)
        return 200, f"调用大模型接口失败：{str(e)}", None


def stream_answer_chunks(question: str):
    """流式返回回答片段（由 SSE/StreamingResponse 承载）。"""
    if not settings.LLM_ENABLED:
        yield "当前服务未启用大模型（LLM_ENABLED=false），请联系管理员配置。"
        return

    try:
        for chunk in call_llm_stream(question):
            yield chunk
    except httpx.TimeoutException:
        yield "\n\n[调用大模型超时，请稍后重试或简化问题。]"
    except Exception as e:
        logger.error("流式调用失败: %s", e)
        yield f"\n\n[调用失败：{str(e)}]"

