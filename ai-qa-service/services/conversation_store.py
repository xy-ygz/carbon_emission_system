"""多轮对话：内存会话，每会话保留最近若干轮 Q&A（线程安全）。"""

from __future__ import annotations

import uuid
from collections import deque
from threading import Lock
from typing import Deque, Dict, List, Optional, Tuple

Turn = Tuple[str, str]  # (user, assistant)


class ConversationStore:
    """conversation_id -> 最近 max_turns 轮 (user, assistant)。"""

    def __init__(self, max_turns: int = 5) -> None:
        self._max = max_turns
        self._turns: Dict[str, Deque[Turn]] = {}
        self._locks: Dict[str, Lock] = {}
        self._meta_lock = Lock()

    def _lock(self, cid: str) -> Lock:
        with self._meta_lock:
            if cid not in self._locks:
                self._locks[cid] = Lock()
            return self._locks[cid]

    def new_id(self) -> str:
        return str(uuid.uuid4())

    def get_turns(self, conversation_id: Optional[str]) -> List[Turn]:
        if not conversation_id:
            return []
        with self._lock(conversation_id):
            dq = self._turns.get(conversation_id)
            return list(dq) if dq else []

    def append(self, conversation_id: str, user: str, assistant: str) -> None:
        with self._lock(conversation_id):
            if conversation_id not in self._turns:
                self._turns[conversation_id] = deque(maxlen=self._max)
            self._turns[conversation_id].append((user, assistant))

    def clear(self, conversation_id: Optional[str]) -> None:
        if not conversation_id:
            return
        with self._lock(conversation_id):
            self._turns.pop(conversation_id, None)


conversation_store = ConversationStore(max_turns=5)
