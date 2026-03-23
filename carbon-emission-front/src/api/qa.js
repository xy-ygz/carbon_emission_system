import request from '../utils/request';
import tokenManager from '../utils/tokenManager';

export function askQuestion(question) {
  return request({
    url: '/api/ai/qa/ask',
    method: 'post',
    data: { question }
  });
}

/**
 * 流式问答：边生成边回调 onChunk，首字延迟低，体验更好
 * @param {string} question
 * @param {function(string): void} onChunk - 每收到一段文本调用
 * @returns {Promise<void>} - 流结束或出错时 resolve/reject
 */
export function askQuestionStream(question, onChunk) {
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
    body: JSON.stringify({ question: question.trim() })
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

