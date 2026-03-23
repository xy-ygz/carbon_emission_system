"""
   simple_rag：
        预构建「已有碳排放数据接口」关键词 -> 接口调用路径映射，
        基于用户输入问题，向量检索topK最相关接口，获取相关碳排放数据，并格式化成chunks
        送模型 context 实现 RAG
"""

import logging
import os
import re
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import urlparse, urlunparse

import httpx
import numpy as np
from langchain_openai import OpenAIEmbeddings

from config.config import settings

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

_LOG_FILE = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "logs", "qa_rag.log"))


def _ensure_file_logger() -> None:
    """给当前模块日志增加文件落盘（避免重复添加 handler）。"""
    log_dir = os.path.dirname(_LOG_FILE)
    os.makedirs(log_dir, exist_ok=True)

    for h in logger.handlers:
        if isinstance(h, logging.FileHandler) and getattr(h, "baseFilename", None) == _LOG_FILE:
            return

    file_handler = logging.FileHandler(_LOG_FILE, encoding="utf-8")
    file_handler.setLevel(logging.INFO)
    formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)


_ensure_file_logger()

HTTP_CLIENT_TIMEOUT = httpx.Timeout(
    connect=10.0,
    read=60.0,
    write=20.0,
    pool=10.0,
)


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


def _build_embeddings_model() -> Optional[OpenAIEmbeddings]:
    """构造用于向量化的 embeddings client。"""
    if not settings.LLM_API_KEY:
        return None
    base_url = _normalize_base_url(settings.EMBEDDING_BASE_URL, "/embeddings")
    return OpenAIEmbeddings(
        model=settings.EMBEDDING_MODEL,
        api_key=settings.LLM_API_KEY,
        base_url=base_url,
    )


def _parse_year_range(text: str) -> Optional[Tuple[int, int]]:
    """解析年份区间（如 2023-2024 / 2023至2024）。"""
    if not text:
        return None
    m = re.search(r"(\d{4})\s*(?:-|—|至|到|~)\s*(\d{4})", text)
    if not m:
        return None
    y1 = int(m.group(1))
    y2 = int(m.group(2))
    if y1 <= 0 or y2 <= 0:
        return None
    return (min(y1, y2), max(y1, y2))


def _parse_year(text: str) -> Optional[int]:
    """解析单个年份（如 2024）。"""
    if not text:
        return None
    m = re.search(r"(19\d{2}|20\d{2}|21\d{2})", text)
    if not m:
        return None
    try:
        return int(m.group(0))
    except Exception:
        return None


def _parse_month(text: str) -> Optional[int]:
    """解析单个月份（如 3月 / 12月份）。"""
    if not text:
        return None
    m = re.search(r"(1[0-2]|[1-9])\s*(?:月|月份)", text)
    if not m:
        return None
    try:
        return int(m.group(1))
    except Exception:
        return None


def _parse_month_range(text: str) -> Optional[Tuple[int, int]]:
    """解析月份区间（如 3月~6月 / 3月到6月）。"""
    if not text:
        return None
    # 兼容常见写法：
    # - 3月~6月
    # - 3月到6月
    # - 2024年3月~6月
    # - 2-3月 / 2~3月 / 2到3月（首个“月”可省略）
    m = re.search(
        r"(1[0-2]|[1-9])\s*(?:月|月份)?\s*(?:-|—|~|到|至)\s*(1[0-2]|[1-9])\s*(?:月|月份)",
        text,
    )
    if not m:
        return None
    try:
        m1 = int(m.group(1))
        m2 = int(m.group(2))
        if m1 <= 0 or m2 <= 0:
            return None
        return (min(m1, m2), max(m1, m2))
    except Exception:
        return None


def _detect_area_flag(text: str) -> bool:
    """根据问题关键词判断是否需要地均排放（area=true）。"""
    if not text:
        return False
    return any(key in text for key in ["地均", "单位面积", "每平方米", "m2"])


def _clean_params(params: Dict[str, Any]) -> Dict[str, Any]:
    """去掉 None / 空字符串入参，避免后端解析异常。"""
    return {k: v for k, v in params.items() if v is not None and v != ""}


def _fetch_endpoint_data(
    client: httpx.Client,
    endpoint_path: str,
    params: Dict[str, Any],
) -> Optional[Any]:
    """调用单个后端 GET 接口并返回 data。"""
    url = f"{settings.BACKEND_BASE_URL}{endpoint_path}"
    resp = client.get(url, params=params)
    if resp.status_code != 200:
        logger.warning(
            "[RAG] endpoint_non_200 path=%s params=%s status=%s",
            endpoint_path,
            params,
            resp.status_code,
        )
        return None
    body = resp.json()
    if body.get("code") != 200:
        logger.warning(
            "[RAG] endpoint_code_not_200 path=%s params=%s code=%s msg=%s",
            endpoint_path,
            params,
            body.get("code"),
            body.get("msg") or body.get("message"),
        )
        return None
    logger.info("[RAG] endpoint_success path=%s params=%s", endpoint_path, params)
    return body.get("data")


def _format_endpoint_chunk(endpoint_title: str, data: Any, endpoint_kind: str) -> Optional[str]:
    """把后端 data 格式化成适合 LLM 的上下文片段。"""
    if data is None:
        return None

    # emissionCategory 返回结构是：
    # [
    #   { "objectCategory": "食品", "emissionMonthAmount": [ {"time":"2025年2月","emissionAmount":...}, ... ] },
    #   ...
    # ]
    if endpoint_kind == "emissionCategory":
        if not isinstance(data, list) or not data:
            return None
        lines = [endpoint_title]
        for cat in data:
            if not isinstance(cat, dict):
                continue
            category = cat.get("objectCategory") or cat.get("category") or ""
            month_items = cat.get("emissionMonthAmount") or cat.get("emissionMonthAmounts") or []
            if not isinstance(month_items, list) or not month_items:
                continue
            parts = []
            for it in month_items:
                if not isinstance(it, dict):
                    continue
                t = it.get("time")
                v = it.get("emissionAmount", it.get("amount"))
                if t is None or v is None:
                    continue
                parts.append(f"{t}：{v} kg CO2")
            if category and parts:
                lines.append(f"{category}：\n- " + "\n- ".join(parts))
        if len(lines) <= 1:
            return None
        return "\n".join(lines)

    if endpoint_kind in {"listSpeciesCarbon", "listSpeciesConsumptionCount"}:
        raw = data
        items = raw.get("list", []) if isinstance(raw, dict) else (raw if isinstance(raw, list) else [])
        if not items:
            return None
        lines = [endpoint_title]
        for it in items:
            if not isinstance(it, dict):
                continue
            name = it.get("objectCategory", it.get("category", ""))
            val = it.get("emissionAmount", it.get("amount", it.get("value", "")))
            if name or val not in (None, ""):
                lines.append(f"{name}：{val}")
        if len(lines) <= 1:
            return None
        return "\n".join(lines)

    if isinstance(data, dict):
        lines = [endpoint_title]
        for k, v in data.items():
            if v is None:
                continue
            lines.append(f"{k}：{v}")
        if len(lines) <= 1:
            return None
        return "\n".join(lines)

    if isinstance(data, list) and data:
        # 未知结构（如楼宇月度列表）直接序列化
        return f"{endpoint_title}\n{str(data)}"

    return f"{endpoint_title}\n{str(data)}"


def _endpoint_catalog() -> List[Dict[str, str]]:
    """基于 已有碳排放数据接口 构建“接口关键词 -> 接口路径”的映射目录。"""
    return [
        {
            "id": "schoolInfo",
            "title": "【学校概况】",
            "path": "/school/info",
            "kind": "schoolInfo",
            "keywords": "校园概括 学校名称 人数 老师人数 学生人数 占地面积 建筑面积 绿地面积 学校碳排放总量 人均碳排放量 地均碳排放量 学校电耗 学校热耗 学校水耗 学校能耗总量",
        },
        {
            "id": "collegeCarbonEmission",
            "title": "【总碳排放量/人均/地均】",
            "path": "/carbonEmission/collegeCarbonEmission",
            "kind": "collegeCarbonEmission",
            "keywords": "得到总碳排放量 人均 地均 碳排放量 若当年无数据 默认回退 fallbackYears",
        },
        {
            "id": "mulberryDiagram",
            "title": "【桑葚图】",
            "path": "/carbonEmission/mulberryDiagram",
            "kind": "mulberryDiagram",
            "keywords": "桑葚图 指定年份不同物品分类对应的排放类型及排放量",
        },
        {
            "id": "emissionCategory",
            "title": "【排放类别/时间段】",
            "path": "/carbonEmission/emissionCategory",
            "kind": "emissionCategory",
            "keywords": "柱状堆叠图 指定起始年份-起始月份~终止年份-终止月份 每个月份不同物品分类对应的排放量",
        },
        {
            "id": "listSpeciesConsumptionCount",
            "title": "【各排放源碳排放消耗量】",
            "path": "/carbonEmission/listSpeciesConsumptionCount",
            "kind": "listSpeciesConsumptionCount",
            "keywords": "指定年份不同物品种类对应的碳排放消耗量",
        },
        {
            "id": "listSpeciesCarbon",
            "title": "【各排放源 CO2 排放量】",
            "path": "/carbonEmission/listSpeciesCarbon",
            "kind": "listSpeciesCarbon",
            "keywords": "指定年份不同物品种类对应的碳排放",
        },
        {
            "id": "listBuildingCarbonLine",
            "title": "【楼宇碳排放折线图】",
            "path": "/carbonEmission/listBuildingCarbonLine",
            "kind": "listBuildingCarbonLine",
            "keywords": "每年的不同楼宇对应的碳排放 折线图 年份 地均排放/总排放 楼宇名称",
        },
        {
            "id": "listBuildingCarbonBar",
            "title": "【楼宇碳排放柱状图】",
            "path": "/carbonEmission/listBuildingCarbonBar",
            "kind": "listBuildingCarbonBar",
            "keywords": "每年的每月不同楼宇对应的碳排放 柱状图 年份 月份 地均排放/总排放 楼宇名称",
        },
        {
            "id": "listBuildingInfo",
            "title": "【楼宇电耗/面积/碳排放】",
            "path": "/carbonEmission/listBuildingInfo",
            "kind": "listBuildingInfo",
            "keywords": "每年的不同楼宇电耗 建筑面积 碳排放量 单位面积排放量 年份 月份 地均/总 楼宇名称",
        },
    ]


def embed_texts(texts: List[str], embeddings_model: OpenAIEmbeddings) -> Optional[np.ndarray]:
    """调用 embedding 模型把文本批量转成向量。"""
    if not texts or not settings.LLM_API_KEY:
        return None
    try:
        all_embeddings = embeddings_model.embed_documents(texts)
    except Exception as e:
        logger.warning("embedding 调用异常: %s", e)
        return None
    if len(all_embeddings) != len(texts):
        return None
    return np.array(all_embeddings, dtype=np.float32)


def _cosine_similarity(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    """计算余弦相似度（a 的每行与 b 的向量比较）。"""
    a_norm = a / (np.linalg.norm(a, axis=1, keepdims=True) + 1e-8)
    b_norm = b / (np.linalg.norm(b) + 1e-8)
    return (a_norm @ b_norm.T).ravel()


def _choose_endpoints(question: str, endpoint_catalog: List[Dict[str, str]], embeddings_model: Optional[OpenAIEmbeddings]) -> List[Dict[str, str]]:
    """根据接口关键词相似度选出最相关的接口（默认 top2）。"""
    selected = endpoint_catalog[:2]
    if embeddings_model is None or not settings.LLM_API_KEY:
        return selected

    try:
        keyword_texts = [e["keywords"] for e in endpoint_catalog]
        keyword_emb = embed_texts(keyword_texts, embeddings_model=embeddings_model)
        q_emb = embed_texts([question], embeddings_model=embeddings_model)
        if keyword_emb is None or q_emb is None or q_emb.shape[0] != 1:
            return selected
        sims = _cosine_similarity(keyword_emb, q_emb)
        take_n = min(2, len(endpoint_catalog))
        top_indices = np.argsort(sims)[::-1][:take_n]
        selected = [endpoint_catalog[i] for i in top_indices]
    except Exception as e:
        logger.warning("接口选择向量化失败，使用默认候选: %s", e)

    return selected


def fetch_dynamic_chunks(question: str, embeddings_model: Optional[OpenAIEmbeddings]) -> List[str]:
    """动态调用最相关后端接口并把结果拼成候选 chunks。"""
    chunks: List[str] = []

    year_range = _parse_year_range(question)
    year = _parse_year(question) if not year_range else year_range[0]
    month = _parse_month(question)
    month_range = _parse_month_range(question)
    area_flag = _detect_area_flag(question)

    endpoint_catalog = _endpoint_catalog()
    selected = _choose_endpoints(question, endpoint_catalog, embeddings_model=embeddings_model)

    # 当用户明确提出“月度/月份区间”问题时，强制把 emissionCategory 纳入候选接口
    # （否则只靠接口关键词相似度，可能漏掉唯一的月度分解入口）
    if (month is not None or month_range is not None) and not any(e["id"] == "emissionCategory" for e in selected):
        selected = [*selected, next(e for e in endpoint_catalog if e["id"] == "emissionCategory")]

    logger.info(
        "[RAG] question=%s year_range=%s year=%s month_range=%s month=%s area_flag=%s",
        question,
        year_range,
        year,
        month_range,
        month,
        area_flag,
    )
    logger.info(
        "[RAG] selected_endpoints=%s",
        [f'{e.get("id")}:{e.get("path")}' for e in selected],
    )

    try:
        with httpx.Client(timeout=HTTP_CLIENT_TIMEOUT) as client:
            for endpoint in selected:
                params: Dict[str, Any] = {}

                if endpoint["id"] in {"collegeCarbonEmission", "mulberryDiagram", "listSpeciesConsumptionCount", "listSpeciesCarbon"}:
                    params["year"] = str(year) if endpoint["id"] == "collegeCarbonEmission" and year is not None else year
                    if month is not None:
                        params["month"] = month

                if endpoint["id"] in {"emissionCategory"}:
                    if year_range is not None:
                        params["startYear"] = year_range[0]
                        params["endYear"] = year_range[1]
                        params["startMonth"] = month_range[0] if month_range is not None else (month if month is not None else 1)
                        params["endMonth"] = month_range[1] if month_range is not None else (month if month is not None else 12)
                    elif year is not None:
                        params["startYear"] = year
                        params["endYear"] = year
                        params["startMonth"] = month_range[0] if month_range is not None else (month if month is not None else 1)
                        params["endMonth"] = month_range[1] if month_range is not None else (month if month is not None else 12)

                if endpoint["id"] in {"listBuildingCarbonLine"}:
                    params["year"] = year
                    params["area"] = area_flag

                if endpoint["id"] in {"listBuildingCarbonBar", "listBuildingInfo"}:
                    if year is not None and month is not None:
                        params["year"] = year
                        params["month"] = month
                    params["area"] = area_flag

                params = _clean_params(params)
                logger.info(
                    "[RAG] calling endpoint=%s path=%s params=%s",
                    endpoint.get("id"),
                    endpoint.get("path"),
                    params,
                )
                data = _fetch_endpoint_data(
                    client=client,
                    endpoint_path=endpoint["path"],
                    params=params,
                )
                if data is None:
                    continue

                chunk = _format_endpoint_chunk(
                    endpoint_title=endpoint["title"],
                    data=data,
                    endpoint_kind=endpoint["kind"],
                )
                if chunk:
                    chunks.append(chunk)
    except Exception as e:
        logger.warning("动态拉取并拼接失败: %s", e)

    return chunks


def get_relevant_context(question: str, top_k: Optional[int] = None, embeddings_model: Optional[OpenAIEmbeddings] = None) -> str:
    """拼装 RAG context：优先依赖接口选择（fetch_dynamic_chunks），不再对 chunks 做额外精排。"""

    # 如果调用方没有传 embeddings_model，则按配置自动创建
    if embeddings_model is None and settings.EMBEDDING_ENABLED and settings.LLM_API_KEY:
        embeddings_model = _build_embeddings_model()

    # 得到与 用户问题 最相关的 topK 系统数据接口，将接口返回数据格式化成 chunks
    # 若 embeddings 仍不可用，则直接返回全部 chunks（保持“轻量兜底”）
    if embeddings_model is None:
        chunks = fetch_dynamic_chunks(question=question, embeddings_model=None)
        return "\n\n".join(chunks)

    chunks = fetch_dynamic_chunks(question=question, embeddings_model=embeddings_model)
    if not chunks:
        return "当前系统暂无可用的业务数据。"
    # chunks 已来自与问题相关的接口候选；当前数据量很少时，直接拼接即可保证召回覆盖
    return "\n\n".join(chunks)

