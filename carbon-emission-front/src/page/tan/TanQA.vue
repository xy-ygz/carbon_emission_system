<template>
  <div class="tan-qa-page">
    <el-card class="qa-card">
      <div slot="header" class="qa-header">
        <span>智能问答助手</span>
        <p class="qa-desc">聚焦学校碳排放、能耗监测与减碳分析，支持流式回复。</p>
      </div>

      <div ref="chatScroll" class="qa-chat-area">
        <div v-if="!messages.length" class="qa-empty">
          开始提问吧，我会基于碳排放相关场景为你提供建议。
        </div>
        <div
          v-for="item in messages"
          :key="item.id"
          class="qa-message"
          :class="item.role === 'user' ? 'is-user' : 'is-assistant'"
        >
          <div class="qa-bubble">
            <div
              v-if="item.role === 'assistant'"
              class="qa-bubble-content markdown-body"
              v-html="renderMarkdown(item.content || (item.pending ? '正在思考...' : ''))"
            />
            <div v-else class="qa-bubble-content qa-user-text">{{ item.content }}</div>
          </div>
        </div>
      </div>

      <div class="qa-input-area">
        <el-input
          type="textarea"
          v-model="inputText"
          :rows="3"
          resize="none"
          placeholder="请在这里输入与学校碳排放、能耗监测、减碳分析等相关的问题…"
          @keydown.native="handleInputKeydown"
        />
        <div class="qa-actions">
          <el-button type="primary" :loading="loading" @click="handleSend">
            {{ loading ? '正在思考…' : '发送' }}
          </el-button>
          <el-button @click="handleClear">清空会话</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { askQuestionStream, askQuestion } from '../../api/qa';

export default {
  name: 'TanQA',
  data() {
    return {
      inputText: '',
      loading: false,
      messages: []
    };
  },
  methods: {
    renderMarkdown(raw) {
      if (!raw) return '';
      const html = marked.parse(raw, { breaks: true });
      return DOMPurify.sanitize(html);
    },
    handleInputKeydown(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        this.handleSend();
      }
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.chatScroll;
        if (!el) return;
        el.scrollTop = el.scrollHeight;
      });
    },
    createMessage(role, content, pending) {
      return {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        role,
        content: content || '',
        pending: !!pending
      };
    },
    async handleSend() {
      if (this.loading) return;
      const q = this.inputText && this.inputText.trim();
      if (!q) {
        this.$message.warning('请先输入问题');
        return;
      }

      const userMessage = this.createMessage('user', q, false);
      const assistantMessage = this.createMessage('assistant', '', true);
      this.messages.push(userMessage, assistantMessage);
      this.inputText = '';
      this.loading = true;
      this.scrollToBottom();

      try {
        let hasStreamContent = false;
        await askQuestionStream(q, (text) => {
          if (!text) return;
          hasStreamContent = true;
          assistantMessage.pending = false;
          assistantMessage.content += text;
          this.scrollToBottom();
        });
        if (!assistantMessage.content && !hasStreamContent) {
          assistantMessage.pending = false;
          assistantMessage.content = '暂无回答内容';
        }
      } catch (e) {
        this.$message.info('流式请求异常，正在改用普通模式重试…');
        try {
          const res = await askQuestion(q);
          assistantMessage.pending = false;
          assistantMessage.content = (res.data && res.data.data) || '暂无回答内容';
        } catch (e2) {
          assistantMessage.pending = false;
          assistantMessage.content = '请求失败，请稍后重试。' + (e2.message || e.message || '');
        }
      } finally {
        this.loading = false;
        this.scrollToBottom();
      }
    },
    handleClear() {
      this.inputText = '';
      this.messages = [];
      this.loading = false;
    }
  }
};
</script>

<style scoped>
.qa-card {
  max-width: 900px;
  margin: 0 auto;
}

.qa-header {
  font-size: 16px;
  font-weight: 600;
}

.qa-desc {
  margin: 8px 0 0;
  font-size: 13px;
  font-weight: 400;
  color: #8a919f;
}

.qa-chat-area {
  display: flex;
  flex-direction: column;
  height: min(58vh, 560px);
  min-height: 0;
  overflow-y: auto;
  padding: 14px 4px 10px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafbfc;
}

.qa-empty {
  color: #8a919f;
  text-align: center;
  padding: 26px 12px;
  font-size: 13px;
}

.qa-message {
  display: flex;
  margin-bottom: 12px;
}

.qa-message.is-user {
  justify-content: flex-end;
}

.qa-message.is-assistant {
  justify-content: flex-start;
}

.qa-bubble {
  width: fit-content;
  max-width: min(86%, 780px);
  border-radius: 14px;
  padding: 10px 12px;
  line-height: 1.65;
  font-size: 14px;
  text-align: left;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.qa-message.is-user .qa-bubble {
  background: #3b82f6;
  color: #fff;
  border-bottom-right-radius: 6px;
}

.qa-message.is-assistant .qa-bubble {
  background: #ffffff;
  color: #2f3441;
  border: 1px solid #ebeef5;
  border-bottom-left-radius: 6px;
}

.qa-user-text {
  white-space: pre-wrap;
  text-align: left;
}

.qa-input-area {
  margin-top: 14px;
}

.qa-actions {
  margin-top: 10px;
  text-align: right;
}

.qa-bubble-content {
  text-align: left;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.qa-bubble-content.markdown-body >>> h1,
.qa-bubble-content.markdown-body >>> h2,
.qa-bubble-content.markdown-body >>> h3 {
  margin: 0.6em 0 0.35em;
  font-weight: 600;
}

.qa-bubble-content.markdown-body >>> h1 {
  font-size: 1.25em;
}

.qa-bubble-content.markdown-body >>> h2 {
  font-size: 1.15em;
}

.qa-bubble-content.markdown-body >>> h3 {
  font-size: 1.05em;
}

.qa-bubble-content.markdown-body >>> p {
  margin: 0.4em 0;
  text-align: left;
}

.qa-bubble-content.markdown-body >>> ul,
.qa-bubble-content.markdown-body >>> ol {
  margin: 0.4em 0 0.4em 1.2em;
  padding: 0;
}

.qa-bubble-content.markdown-body >>> code {
  background: rgba(0, 0, 0, 0.06);
  padding: 0.1em 0.35em;
  border-radius: 3px;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 0.9em;
}

.qa-bubble-content.markdown-body >>> pre {
  background: #2d2d2d;
  color: #f8f8f2;
  padding: 10px 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 0.5em 0;
}

.qa-bubble-content.markdown-body >>> pre code {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: 0.85em;
}

.qa-bubble-content.markdown-body >>> blockquote {
  margin: 0.5em 0;
  padding-left: 0.75em;
  border-left: 3px solid #dcdfe6;
  color: #606266;
  text-align: left;
}

.qa-bubble-content.markdown-body >>> table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.5em 0;
  font-size: 0.95em;
}

.qa-bubble-content.markdown-body >>> th,
.qa-bubble-content.markdown-body >>> td {
  border: 1px solid #dcdfe6;
  padding: 6px 10px;
  text-align: left;
}

.qa-bubble-content.markdown-body >>> th {
  background: #ebeef5;
}

@media (max-width: 768px) {
  .tan-qa-page {
    padding: 10px 8px;
  }

  .qa-chat-area {
    height: 56vh;
    border-radius: 8px;
    padding: 10px 2px 8px;
  }

  .qa-bubble {
    max-width: 88%;
    font-size: 13px;
    padding: 9px 10px;
  }

  .qa-desc {
    font-size: 12px;
  }
}
</style>

