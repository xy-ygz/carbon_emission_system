import request from '../utils/request';
import tokenManager from '../utils/tokenManager';

function getQaUserId() {
  try {
    const id = localStorage.getItem('id');
    return id && String(id).trim() ? String(id).trim() : null;
  } catch (e) {
    return null;
  }
}

export function askQuestion(question, sessionId) {
  return request({
    url: '/api/ai/qa/ask',
    method: 'post',
    data: {
      question,
      session_id: sessionId || null,
      user_id: getQaUserId()
    }
  });
}

/**
 * 流式问答：边生成边回调 onChunk，首字延迟低，体验更好
 * @param {string} question
 * @param {string} sessionId
 * @param {function({type: string, content?: string, session_id?: string}): void} onEvent
 * @returns {Promise<void>} - 流结束或出错时 resolve/reject
 */
export function askQuestionStream(question, sessionId, onEvent) {
  const base = typeof window !== 'undefined' && window.location ? window.location.origin : '';
  const url = `${base}/api/ai/qa/ask/stream`;
  const token = tokenManager.getAccessToken();
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) headers['Authorization'] = 'Bearer ' + token;

  return fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      question: question.trim(),
      session_id: sessionId || null,
      user_id: getQaUserId()
    })
  }).then(async (response) => {
    if (!response.ok) {
      const err = new Error(response.statusText || '请求失败');
      err.status = response.status;
      throw err;
    }
    if (!response.body) {
      throw new Error('流式响应体为空');
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';
    while (true) {
      const { value, done } = await reader.read();
      const text = value ? decoder.decode(value, { stream: true }) : '';
      if (text) {
        buffer += text;
        let splitIdx = buffer.indexOf('\n');
        while (splitIdx >= 0) {
          const line = buffer.slice(0, splitIdx).trim();
          buffer = buffer.slice(splitIdx + 1);
          if (line) {
            try {
              const payload = JSON.parse(line);
              if (onEvent) onEvent(payload);
            } catch (e) {
              // 兼容旧版纯文本流
              if (onEvent) onEvent({ type: 'answer', content: line });
            }
          }
          splitIdx = buffer.indexOf('\n');
        }
      }
      if (done) {
        const tail = decoder.decode();
        if (tail) {
          const line = (buffer + tail).trim();
          if (line) {
            try {
              const payload = JSON.parse(line);
              if (onEvent) onEvent(payload);
            } catch (e) {
              if (onEvent) onEvent({ type: 'answer', content: line });
            }
          }
        }
        break;
      }
    }
  });
}

