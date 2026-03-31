"""会话记忆服务：负责会话ID管理、5轮上下文记忆与本地持久化。"""

import sqlite3
import threading
import uuid
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from config.config import settings

_DB_LOCK = threading.Lock()


def _db_path() -> Path:
    path = Path(settings.QA_SESSION_DB_PATH).expanduser()
    if not path.parent.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
    return path


def _connect() -> sqlite3.Connection:
    conn = sqlite3.connect(str(_db_path()), check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


def _init_db() -> None:
    with _DB_LOCK:
        conn = _connect()
        try:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS qa_session_message (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id TEXT NOT NULL,
                    question TEXT NOT NULL,
                    answer TEXT NOT NULL,
                    thinking TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """
            )
            conn.commit()
        finally:
            conn.close()


_init_db()


def ensure_session_id(session_id: Optional[str]) -> str:
    sid = (session_id or "").strip()
    if sid:
        return sid
    return str(uuid.uuid4())


def compose_memory_key(session_id: Optional[str], user_id: Optional[str]) -> Tuple[str, str]:
    """返回 (数据库存储用会话键, 回传给前端的 session_id)。

    已登录用户使用 user_id + session_id 组合键，避免同一浏览器会话下多账号串话；
    未传 user_id 时行为与旧版一致，仅使用 session_id。
    """
    sid = ensure_session_id(session_id)
    uid = (user_id or "").strip()
    if uid:
        return f"u:{uid}::s:{sid}", sid
    return sid, sid


def save_round(session_id: str, question: str, answer: str, thinking: Optional[str] = None) -> None:
    if not session_id or not question or not answer:
        return
    with _DB_LOCK:
        conn = _connect()
        try:
            conn.execute(
                """
                INSERT INTO qa_session_message (session_id, question, answer, thinking)
                VALUES (?, ?, ?, ?)
                """,
                (session_id, question, answer, thinking or ""),
            )
            conn.commit()
        finally:
            conn.close()


def get_recent_rounds(session_id: str, rounds: Optional[int] = None) -> List[Dict[str, str]]:
    if not session_id:
        return []
    limit_rounds = rounds or settings.QA_MEMORY_ROUNDS
    with _DB_LOCK:
        conn = _connect()
        try:
            rows = conn.execute(
                """
                SELECT question, answer
                FROM qa_session_message
                WHERE session_id = ?
                ORDER BY id DESC
                LIMIT ?
                """,
                (session_id, max(1, int(limit_rounds))),
            ).fetchall()
        finally:
            conn.close()

    rounds_data = [{"question": row["question"], "answer": row["answer"]} for row in rows]
    rounds_data.reverse()
    return rounds_data


def format_rounds_for_prompt(rounds_data: List[Dict[str, str]]) -> str:
    if not rounds_data:
        return "（暂无历史对话）"
    lines: List[str] = []
    for idx, item in enumerate(rounds_data, start=1):
        q = (item.get("question") or "").strip()
        a = (item.get("answer") or "").strip()
        if not q and not a:
            continue
        lines.append(f"[第{idx}轮]\n用户：{q}\n助手：{a}")
    return "\n\n".join(lines) if lines else "（暂无历史对话）"
