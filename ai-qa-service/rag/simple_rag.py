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


def _embedding_endpoint() -> Optional[str]:
    """构造 embedding 请求 URL。"""
    if not settings.LLM_API_KEY:
        return None
    raw_url = (settings.EMBEDDING_BASE_URL or "").strip()
    if not raw_url:
        return None
    if raw_url.endswith("/embeddings"):
        return raw_url
    base_url = _normalize_base_url(raw_url, "/embeddings").rstrip("/")
    return f"{base_url}/embeddings"


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


def _detect_annual_report_download_intent(text: str) -> bool:
    """
    识别「要拿报告类文件」意图，用于强制注入 exportReport。
    以「下载」为主触发词；无「下载」时仅「导出」须同时带报告/年报等语义，避免误伤其它导出。
    """
    if not text:
        return False
    t = str(text).lower()

    # 与年度报告导出相关的主题词（与「下载」组合使用）
    report_topic = (
        ("报告" in t)
        or ("年报" in t)
        or ("年度报告" in t)
        or ("碳排放报告" in t)
        or ("排放报告" in t)
        or ("温室气体报告" in t)
        or ("温室气体" in t and "报告" in t)
        or ("年度" in t and "报告" in t)
        or ("pdf" in t)
        or ("word" in t)
        or ("docx" in t)
    )

    has_download = "下载" in t
    has_export = "导出" in t
    if not has_download and not has_export:
        return False

    # 重点：出现「下载」且语境像报告/年报/pdf/word 等，即注入导出接口
    if has_download:
        return report_topic

    # 仅有「导出」：要求带报告类主题，避免「导出数据/导出表格」等走下载说明
    return has_export and report_topic


def _clean_params(params: Dict[str, Any]) -> Dict[str, Any]:
    """去掉 None / 空字符串入参，避免后端解析异常。"""
    return {k: v for k, v in params.items() if v is not None and v != ""}


def _public_api_base_for_links() -> str:
    """拼给前端/用户点击的 API 基址，补全 https://（compose 里常写成 host/path 无协议）。"""
    raw = (getattr(settings, "PUBLIC_BACKEND_BASE_URL", None) or "").strip()
    if not raw:
        raw = (settings.BACKEND_BASE_URL or "").strip()
    raw = raw.rstrip("/")
    if not raw:
        return ""
    if not (raw.startswith("http://") or raw.startswith("https://")):
        raw = "https://" + raw.lstrip("/")
    return raw


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

    # 文件下载/说明类接口：不依赖后端 data，直接输出调用方式提示
    if endpoint_kind in {"exportReportDownload", "downloadTemplate", "exportTaskDownloadFile"}:
        return str(data) if isinstance(data, str) and data else None

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
        {
            "id": "getAllCarbonEmission",
            "title": "【碳排放记录分页查询】",
            "path": "/carbonEmission/getAllCarbonEmission",
            "kind": "getAllCarbonEmission",
            "keywords": "分页查询 碳排放记录 current size 页码 每页大小 名称模糊查询 name 年份 year 月份 month 记录列表 total",
        },
        {
            "id": "exportReportDataByYear",
            "title": "【年度报告数据】",
            "path": "/carbonEmission/exportReportDataByYear",
            "kind": "exportReportDataByYear",
            "keywords": "年度报告 数据 year 获取年度报告数据 报告内容 概览 统计",
        },
        # 说明类/文件下载类接口：纳入目录供检索，但不在 RAG 里直接调用（非 JSON）
        {
            "id": "exportReport",
            "title": "【导出年度报告（文件下载）】",
            "path": "/carbonEmission/exportReport",
            "kind": "exportReportDownload",
            "keywords": "导出 报告 下载 Word PDF docx pdf 年度报告 碳排放报告 排放报告 年报 year format 导出报告文件 下载链接",
        },
    ]


def embed_texts(texts: List[str], embedding_url: str) -> Optional[np.ndarray]:
    """调用 embedding 接口把文本批量转成向量。"""
    if not texts or not settings.LLM_API_KEY:
        return None
    try:
        payload = {
            "model": settings.EMBEDDING_MODEL,
            "input": texts,
        }
        headers = {
            "Authorization": f"Bearer {settings.LLM_API_KEY}",
            "Content-Type": "application/json",
        }
        with httpx.Client(timeout=HTTP_CLIENT_TIMEOUT) as client:
            resp = client.post(embedding_url, json=payload, headers=headers)
        if resp.status_code != 200:
            logger.warning("embedding 接口返回非200: status=%s body=%s", resp.status_code, resp.text[:300])
            return None
        body = resp.json()
        # 兼容 OpenAI 风格返回：{"data":[{"embedding":[...]}]}
        data = body.get("data", []) if isinstance(body, dict) else []
        if not isinstance(data, list) or not data:
            logger.warning("embedding 接口返回格式异常: body=%s", str(body)[:300])
            return None
        all_embeddings = []
        for item in data:
            if not isinstance(item, dict):
                return None
            vec = item.get("embedding")
            if not isinstance(vec, list) or not vec:
                return None
            all_embeddings.append(vec)
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


def _choose_endpoints(question: str, endpoint_catalog: List[Dict[str, str]], embedding_url: Optional[str]) -> List[Dict[str, str]]:
    """根据接口关键词相似度选出最相关的接口（默认 topK=VECTOR_TOP_K，默认值见配置）。"""
    k = settings.VECTOR_TOP_K if isinstance(getattr(settings, "VECTOR_TOP_K", None), int) else 3
    k = 3 if k <= 0 else k
    take_n = min(k, len(endpoint_catalog)) if endpoint_catalog else 0
    selected = endpoint_catalog[:take_n] if take_n > 0 else []
    if embedding_url is None or not settings.LLM_API_KEY:
        return selected

    try:
        keyword_texts = [e["keywords"] for e in endpoint_catalog]
        keyword_emb = embed_texts(keyword_texts, embedding_url=embedding_url)
        q_emb = embed_texts([question], embedding_url=embedding_url)
        if keyword_emb is None or q_emb is None or q_emb.shape[0] != 1:
            return selected
        sims = _cosine_similarity(keyword_emb, q_emb)
        take_n = min(max(1, k), len(endpoint_catalog))
        top_indices = np.argsort(sims)[::-1][:take_n]
        selected = [endpoint_catalog[i] for i in top_indices]
    except Exception as e:
        logger.warning("接口选择向量化失败，使用默认候选: %s", e)

    return selected


def fetch_dynamic_chunks(question: str, embedding_url: Optional[str]) -> List[str]:
    """动态调用最相关后端接口并把结果拼成候选 chunks。"""
    chunks: List[str] = []

    year_range = _parse_year_range(question)
    year = _parse_year(question) if not year_range else year_range[0]
    month = _parse_month(question)
    month_range = _parse_month_range(question)
    area_flag = _detect_area_flag(question)

    endpoint_catalog = _endpoint_catalog()
    selected = _choose_endpoints(question, endpoint_catalog, embedding_url=embedding_url)

    # 当用户明确提出“月度/月份区间”问题时，强制把 emissionCategory 纳入候选接口
    # （否则只靠接口关键词相似度，可能漏掉唯一的月度分解入口）
    if (month is not None or month_range is not None) and not any(e["id"] == "emissionCategory" for e in selected):
        selected = [*selected, next(e for e in endpoint_catalog if e["id"] == "emissionCategory")]

    # 当用户话里以「下载」为主且涉及报告/年报/pdf 等时，把 exportReport 纳入候选
    # （避免向量相似度把“报告下载”误判为图表类接口）
    if _detect_annual_report_download_intent(question) and not any(e["id"] == "exportReport" for e in selected):
        selected = [*selected, next(e for e in endpoint_catalog if e["id"] == "exportReport")]

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

                # 文件下载/说明类接口：不直接请求后端，避免二进制内容/权限导致 RAG 失败
                if endpoint["id"] == "exportReport":
                    actual_year = year if year is not None else "（为空则后端会使用实际年份）"
                    fmt = "pdf" if "pdf" in (question or "").lower() else "docx"
                    pub = _public_api_base_for_links()
                    chunks.append(
                        "【导出年度报告（文件下载）】\n"
                        f"- 接口：GET {pub}{endpoint['path']}\n"
                        f"- 参数：year={actual_year}，format={fmt}（docx/pdf）\n"
                        "- 说明：该接口返回文件流（非JSON），适合浏览器下载。"
                    )
                    continue
                if endpoint["id"] == "downloadCarbonEmissionTemplate":
                    pub = _public_api_base_for_links()
                    chunks.append(
                        "【下载碳排放导入模板（文件下载）】\n"
                        f"- 接口：GET {pub}{endpoint['path']}\n"
                        "- 说明：该接口返回 .xlsx 文件流（非JSON）。"
                    )
                    continue
                if endpoint["id"] == "exportTaskDownload":
                    pub = _public_api_base_for_links()
                    chunks.append(
                        "【下载导出任务文件（文件下载）】\n"
                        f"- 接口：GET {pub}{endpoint['path']}\n"
                        "- 参数：taskId=导出任务ID\n"
                        "- 说明：该接口返回文件流（非JSON），任务需为 COMPLETED。"
                    )
                    continue

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

                if endpoint["id"] in {"getAllCarbonEmission"}:
                    params["current"] = 1
                    params["size"] = 10
                    params["name"] = None
                    if year is not None:
                        params["year"] = year
                    if month is not None:
                        params["month"] = month

                if endpoint["id"] in {"exportReportDataByYear"}:
                    if year is not None:
                        params["year"] = year

                if endpoint["id"] in {"exportTaskStatus"}:
                    # 任务ID无法从自然语言可靠解析，这里不给默认值，避免后端必填校验失败
                    params["taskId"] = None

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


def get_relevant_context(question: str, top_k: Optional[int] = None, embedding_url: Optional[str] = None) -> str:
    """拼装 RAG context：优先依赖接口选择（fetch_dynamic_chunks），不再对 chunks 做额外精排。"""

    # 如果调用方没有传 embedding_url，则按配置自动创建
    if embedding_url is None and settings.EMBEDDING_ENABLED and settings.LLM_API_KEY:
        embedding_url = _embedding_endpoint()

    # 得到与 用户问题 最相关的 topK 系统数据接口，将接口返回数据格式化成 chunks
    # 若 embeddings 仍不可用，则直接返回全部 chunks（保持“轻量兜底”）
    if embedding_url is None:
        chunks = fetch_dynamic_chunks(question=question, embedding_url=None)
        return "\n\n".join(chunks)

    chunks = fetch_dynamic_chunks(question=question, embedding_url=embedding_url)
    if not chunks:
        return "当前系统暂无可用的业务数据。"
    # chunks 已来自与问题相关的接口候选；当前数据量很少时，直接拼接即可保证召回覆盖
    return "\n\n".join(chunks)

