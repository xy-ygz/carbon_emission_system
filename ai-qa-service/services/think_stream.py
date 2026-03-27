"""将模型输出按 think 标签拆成「思考」与「正式回答」（流式 + 全文）。"""

from __future__ import annotations

from typing import Generator, Iterable, List, Tuple

# 与 qa_service 提示词约定一致
THINK_OPEN = "【思考开始】"
THINK_CLOSE = "【思考结束】"


def split_think_answer_full(text: str) -> Tuple[str, str]:
    """全文解析：返回 (thinking, answer)。无标签时 thinking 为空，全文视为 answer。"""
    if not text:
        return "", ""
    o = text.find(THINK_OPEN)
    c = text.find(THINK_CLOSE)
    if o == -1 or c == -1 or c < o:
        return "", text.strip()
    think = text[o + len(THINK_OPEN) : c].strip()
    ans = text[c + len(THINK_CLOSE) :].strip()
    return think, ans


class ThinkAnswerSplitter:
    """
    流式分片：yield ("think"|"answer", chunk)。
    若长时间未出现起始标签，则整段视为 answer（兼容模型未按格式输出）。
    """

    def __init__(self, no_tag_flush_chars: int = 512) -> None:
        self._buf = ""
        self._phase = "seek_open"  # seek_open | think | answer
        self._no_tag_flush = no_tag_flush_chars

    def push(self, chunk: str) -> List[Tuple[str, str]]:
        if not chunk:
            return []
        self._buf += chunk
        out: List[Tuple[str, str]] = []

        while self._buf:
            if self._phase == "seek_open":
                i = self._buf.find(THINK_OPEN)
                if i == -1:
                    if len(self._buf) >= self._no_tag_flush:
                        out.append(("answer", self._buf))
                        self._buf = ""
                        self._phase = "answer"
                    break
                if i > 0:
                    out.append(("answer", self._buf[:i]))
                self._buf = self._buf[i + len(THINK_OPEN) :]
                self._phase = "think"
                continue

            if self._phase == "think":
                j = self._buf.find(THINK_CLOSE)
                if j == -1:
                    keep = len(THINK_CLOSE) - 1
                    if len(self._buf) <= keep:
                        break
                    emit = self._buf[:-keep]
                    self._buf = self._buf[-keep:]
                    if emit:
                        out.append(("think", emit))
                    break
                out.append(("think", self._buf[:j]))
                self._buf = self._buf[j + len(THINK_CLOSE) :]
                self._phase = "answer"
                continue

            if self._phase == "answer":
                if self._buf:
                    out.append(("answer", self._buf))
                    self._buf = ""
                break

        return out

    def flush(self) -> List[Tuple[str, str]]:
        """流结束：缓冲区剩余按当前阶段归类。"""
        if not self._buf:
            return []
        tail = self._buf
        self._buf = ""
        if self._phase == "seek_open":
            return [("answer", tail)]
        if self._phase == "think":
            return [("think", tail)]
        return [("answer", tail)]


def iter_tagged_stream(chunks: Iterable[str]) -> Generator[Tuple[str, str], None, None]:
    sp = ThinkAnswerSplitter()
    for ch in chunks:
        for pair in sp.push(ch or ""):
            yield pair
    for pair in sp.flush():
        yield pair
