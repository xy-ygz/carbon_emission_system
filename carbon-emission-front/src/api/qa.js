import request from '../utils/request';
import tokenManager from '../utils/tokenManager';

const STORAGE_KEY = 'tan-qa-conversation-id';

/** 与 ai-qa-service `think_stream.py` 中标记一致 */
export const QA_THINK_OPEN = '【思考开始】';
export const QA_THINK_CLOSE = '【思考结束】';

/**
 * 流式拆分：在出现「思考开始」之前的内容放入 leadBuffer，便于在思考区逐字展示；
 * 切勿把前缀误判为正式回答（否则思考区永远只有「正在思考中」）。
 * @returns {{ thinking: string, answer: string, answerStarted: boolean, leadBuffer: string }}
 */
export function parseThinkAnswerMarkdown(raw) {
  const TO = QA_THINK_OPEN;
  const TC = QA_THINK_CLOSE;
  if (!raw) {
    return { thinking: '', answer: '', answerStarted: false, leadBuffer: '' };
  }
  const i = raw.indexOf(TO);
  if (i === -1) {
    return {
      thinking: '',
      answer: '',
      answerStarted: false,
      leadBuffer: raw
    };
  }
  const afterOpen = i + TO.length;
  const j = raw.indexOf(TC, afterOpen);
  if (j === -1) {
    return {
      thinking: raw.slice(afterOpen),
      answer: '',
      answerStarted: false,
      leadBuffer: ''
    };
  }
  return {
    thinking: raw.slice(afterOpen, j),
    answer: raw.slice(j + TC.length),
    answerStarted: true,
    leadBuffer: ''
  };
}

/**
 * 流结束后再解析一次：若全文从未出现思考标记，则整段视为正式回答（兼容模型未按格式输出）
 */
export function finalizeThinkAnswerMarkdown(raw) {
  const TO = QA_THINK_OPEN;
  if (!raw || !raw.trim()) {
    return { thinking: '', answer: '', answerStarted: false, leadBuffer: '' };
  }
  if (raw.indexOf(TO) === -1) {
    return {
      thinking: '',
      answer: raw,
      answerStarted: true,
      leadBuffer: ''
    };
  }
  return parseThinkAnswerMarkdown(raw);
}

export function getStoredConversationId() {
  try {
    return sessionStorage.getItem(STORAGE_KEY) || '';
  } catch (e) {
    return '';
  }
}

export function setStoredConversationId(id) {
  try {
    if (id) sessionStorage.setItem(STORAGE_KEY, id);
    else sessionStorage.removeItem(STORAGE_KEY);
  } catch (e) {
    /* ignore */
  }
}

export function askQuestion(question, conversationId) {
  return request({
    url: '/api/ai/qa/ask',
    method: 'post',
    data: {
      question,
      conversation_id: conversationId || undefined
    }
  });
}

/**
 * 流式问答：text/markdown 分块，会话 ID 在响应头 X-Conversation-Id
 * @param {string} question
 * @param {function(string): void} onChunk
 * @param {{ conversationId?: string, onConversationId?: function(string): void }} [options]
 * @returns {Promise<void>}
 */
export function askQuestionStream(question, onChunk, options = {}) {
  const base = typeof window !== 'undefined' && window.location ? window.location.origin : '';
  const url = `${base}/api/ai/qa/ask/stream`;
  const token = tokenManager.getAccessToken();
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const conversationId = options.conversationId || '';

  return fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      question: question.trim(),
      conversation_id: conversationId || undefined
    })
  }).then(async (response) => {
    if (!response.ok) {
      const err = new Error(response.statusText || '请求失败');
      err.status = response.status;
      throw err;
    }
    const cid = response.headers.get('X-Conversation-Id');
    if (cid && typeof options.onConversationId === 'function') {
      options.onConversationId(cid);
    }
    if (!response.body) {
      throw new Error('流式响应体为空');
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    while (true) {
      const { value, done } = await reader.read();
      const text = value ? decoder.decode(value, { stream: true }) : '';
      if (text && onChunk) onChunk(text);
      if (done) {
        const tail = decoder.decode();
        if (tail && onChunk) onChunk(tail);
        break;
      }
    }
  });
}
