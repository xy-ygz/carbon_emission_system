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
            <template v-if="item.role === 'assistant'">
              <!-- 正式回答前：完整展示思考；正式回答出现后：默认折叠，可展开 -->
              <div
                v-if="showThinkingBlock(item)"
                class="qa-thinking-wrap"
              >
                <div
                  v-if="item.answerStarted && !item.thinkingOpen"
                  class="qa-thinking-toggle"
                >
                  <el-button type="text" size="small" @click="item.thinkingOpen = true">
                    查看思考过程
                  </el-button>
                </div>
                <div
                  v-show="!item.answerStarted || item.thinkingOpen"
                  class="qa-thinking-panel"
                >
                  <div class="qa-thinking-head">
                    <span class="qa-thinking-title">思考过程</span>
                    <el-button
                      v-if="item.answerStarted && item.thinkingOpen"
                      type="text"
                      size="mini"
                      class="qa-thinking-collapse-btn"
                      @click="item.thinkingOpen = false"
                    >
                      收起
                    </el-button>
                  </div>
                  <div
                    class="qa-thinking-body markdown-body qa-md"
                    v-html="renderThinkingHtml(item)"
                  />
                </div>
              </div>
              <div
                v-if="item.content"
                class="qa-answer-body qa-bubble-content markdown-body qa-md"
                v-html="renderMarkdown(item.content)"
              />
            </template>
            <div v-else class="qa-bubble-content qa-user-text">{{ item.content }}</div>
          </div>
        </div>
      </div>

      <div class="qa-input-area">
        <el-input
          ref="qaInput"
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
import {
  askQuestionStream,
  askQuestion,
  getStoredConversationId,
  setStoredConversationId,
  parseThinkAnswerMarkdown,
  finalizeThinkAnswerMarkdown
} from '../../api/qa';

export default {
  name: 'TanQA',
  data() {
    return {
      inputText: '',
      loading: false,
      messages: [],
      conversationId: ''
    };
  },
  mounted() {
    this.conversationId = getStoredConversationId();
  },
  methods: {
    renderMarkdown(raw) {
      if (!raw) return '';
      // breaks:true 会把单换行变 <br>，与段落叠加后间距过大
      const html = marked.parse(raw, { breaks: false, gfm: true });
      return DOMPurify.sanitize(html);
    },
    /** 思考区：leadBuffer = 出现【思考开始】前的流式前缀；thinking = 两标记之间 */
    renderThinkingHtml(item) {
      const buf = ((item.leadBuffer || '') + (item.thinking || '')).trim();
      if (buf) {
        return this.renderMarkdown((item.leadBuffer || '') + (item.thinking || ''));
      }
      if (!item.answerStarted && item.pending) {
        return '<p class="qa-thinking-placeholder">正在思考中…</p>';
      }
      return '';
    },
    showThinkingBlock(item) {
      const hasThinkText =
        (item.leadBuffer && item.leadBuffer.length) ||
        (item.thinking && item.thinking.trim());
      if (item.answerStarted && hasThinkText) return true;
      if (!item.answerStarted && (item.pending || hasThinkText)) {
        return true;
      }
      return false;
    },
    handleInputKeydown(e) {
      // IME 组合输入（中文/日文等）时回车用于“上屏/选词”，不应触发发送
      if (e && (e.isComposing === true || e.keyCode === 229)) return;

      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        // 粘贴后立刻回车时，v-model 可能尚未同步，延迟到 nextTick 再发送
        this.$nextTick(() => this.handleSend());
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
        pending: !!pending,
        thinking: '',
        leadBuffer: '',
        answerStarted: false,
        thinkingOpen: true,
        rawAccum: ''
      };
    },
    async handleSend() {
      if (this.loading) return;
      let raw = this.inputText;
      let q = raw && raw.trim();

      // 兜底：极端情况下（如粘贴后立即回车），v-model 可能尚未更新，直接从 textarea/input 取值
      if (!q && this.$refs && this.$refs.qaInput) {
        const comp = this.$refs.qaInput;
        let el = comp.$refs && (comp.$refs.textarea || comp.$refs.input);
        if (!el && comp.$el && typeof comp.$el.querySelector === 'function') {
          el = comp.$el.querySelector('textarea, input');
        }
        const domVal = el && typeof el.value === 'string' ? el.value : '';
        raw = domVal;
        q = domVal.trim();
      }
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
        await askQuestionStream(
          q,
          (text) => {
            if (!text) return;
            hasStreamContent = true;
            assistantMessage.rawAccum += text;
            const p = parseThinkAnswerMarkdown(assistantMessage.rawAccum);
            assistantMessage.thinking = p.thinking;
            assistantMessage.leadBuffer = p.leadBuffer || '';
            assistantMessage.content = p.answer;
            if (p.answerStarted) {
              assistantMessage.answerStarted = true;
              assistantMessage.thinkingOpen = false;
            }
            // 有可见思考/正文后再结束「等待」占位，避免首字节就关掉「正在思考中」
            assistantMessage.pending = !(
              (assistantMessage.leadBuffer && assistantMessage.leadBuffer.length) ||
              (assistantMessage.thinking && assistantMessage.thinking.trim()) ||
              assistantMessage.content ||
              assistantMessage.answerStarted
            );
            this.scrollToBottom();
          },
          {
            conversationId: this.conversationId,
            onConversationId: (cid) => {
              this.conversationId = cid;
              setStoredConversationId(cid);
            }
          }
        );
        const fin = finalizeThinkAnswerMarkdown(assistantMessage.rawAccum);
        assistantMessage.thinking = fin.thinking;
        assistantMessage.leadBuffer = fin.leadBuffer || '';
        assistantMessage.content = fin.answer;
        assistantMessage.answerStarted = fin.answerStarted;
        if (fin.answerStarted) {
          assistantMessage.thinkingOpen = false;
        }
        if (!assistantMessage.content && !hasStreamContent) {
          assistantMessage.pending = false;
          assistantMessage.content = '暂无回答内容';
        }
      } catch (e) {
        this.$message.info('流式请求异常，正在改用普通模式重试…');
        try {
          const res = await askQuestion(q, this.conversationId);
          assistantMessage.pending = false;
          assistantMessage.content = (res.data && res.data.data) || '暂无回答内容';
          if (res.data && res.data.conversation_id) {
            this.conversationId = res.data.conversation_id;
            setStoredConversationId(res.data.conversation_id);
          }
          if (res.data && res.data.thinking) {
            assistantMessage.thinking = res.data.thinking;
            assistantMessage.answerStarted = true;
            assistantMessage.thinkingOpen = false;
          }
        } catch (e2) {
          assistantMessage.pending = false;
          assistantMessage.content = '请求失败，请稍后重试。' + (e2.message || e.message || '');
        }
      } finally {
        this.loading = false;
        assistantMessage.pending = false;
        this.scrollToBottom();
      }
    },
    handleClear() {
      this.inputText = '';
      this.messages = [];
      this.loading = false;
      this.conversationId = '';
      setStoredConversationId('');
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
  width: 100%;
}

.qa-bubble {
  width: fit-content;
  max-width: min(86%, 780px);
  border-radius: 14px;
  padding: 10px 12px;
  line-height: 1.6;
  font-size: 14px;
  text-align: left;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.qa-message.is-user .qa-bubble {
  background: #3b82f6;
  color: #fff;
  border-bottom-right-radius: 6px;
  white-space: pre-wrap;
}

.qa-message.is-assistant .qa-bubble {
  background: #ffffff;
  color: #2f3441;
  border: 1px solid #e8eaed;
  border-bottom-left-radius: 6px;
  max-width: 100%;
  width: 100%;
  white-space: normal;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.qa-thinking-wrap {
  margin-bottom: 8px;
}

.qa-thinking-toggle {
  margin-bottom: 8px;
}

.qa-thinking-panel {
  background: linear-gradient(180deg, #f5f7fa 0%, #f0f2f5 100%);
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 10px 12px 12px;
  margin-bottom: 0;
  max-height: min(42vh, 320px);
  overflow: auto;
}

.qa-thinking-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 22px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.qa-thinking-title {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  letter-spacing: 0.02em;
}

.qa-thinking-collapse-btn {
  color: #409eff !important;
  padding: 0 4px !important;
}

.qa-thinking-body {
  font-size: 13px;
  line-height: 1.55;
  color: #606266;
  word-break: break-word;
}

.qa-thinking-body >>> .qa-thinking-placeholder {
  margin: 0;
  color: #909399;
  font-size: 13px;
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

.qa-answer-body {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fafbfd;
  border-radius: 8px;
  border-left: 3px solid #409eff;
  text-align: left;
}

.qa-answer-body:first-child {
  margin-top: 0;
}

/* 思考区与回答区共用：收紧段落与标题间距，避免「段落间距过大」 */
.qa-md >>> h1,
.qa-md >>> h2,
.qa-md >>> h3 {
  font-weight: 600;
  color: #1f2937;
  line-height: 1.35;
}

.qa-md >>> h1 {
  font-size: 1.15em;
  margin: 0.35em 0 0.2em;
}

.qa-md >>> h2 {
  font-size: 1.08em;
  margin: 0.45em 0 0.18em;
}

.qa-md >>> h3 {
  font-size: 1.02em;
  margin: 0.4em 0 0.15em;
}

.qa-md >>> h1:first-child,
.qa-md >>> h2:first-child,
.qa-md >>> h3:first-child {
  margin-top: 0;
}

.qa-md >>> p {
  margin: 0.2em 0;
  line-height: 1.55;
  text-align: left;
  color: #374151;
}

.qa-md >>> p:first-child {
  margin-top: 0;
}

.qa-md >>> p:last-child {
  margin-bottom: 0;
}

.qa-md >>> ul,
.qa-md >>> ol {
  margin: 0.25em 0 0.25em 1.15em;
  padding: 0;
}

.qa-md >>> li {
  margin: 0.12em 0;
  line-height: 1.5;
}

.qa-md >>> li > p {
  margin: 0.1em 0;
}

.qa-md >>> strong {
  color: #111827;
  font-weight: 600;
}

.qa-md >>> code {
  background: rgba(0, 0, 0, 0.06);
  padding: 0.12em 0.35em;
  border-radius: 4px;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 0.88em;
}

.qa-md >>> pre {
  background: #1e293b;
  color: #f1f5f9;
  padding: 8px 10px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 0.35em 0;
  font-size: 0.86em;
}

.qa-md >>> pre code {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: inherit;
}

.qa-md >>> blockquote {
  margin: 0.35em 0;
  padding: 0.2em 0 0.2em 0.65em;
  border-left: 3px solid #c7d2fe;
  color: #4b5563;
  background: rgba(99, 102, 241, 0.06);
  border-radius: 0 4px 4px 0;
}

.qa-md >>> table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.35em 0;
  font-size: 0.94em;
}

.qa-md >>> th,
.qa-md >>> td {
  border: 1px solid #e5e7eb;
  padding: 5px 8px;
  text-align: left;
}

.qa-md >>> th {
  background: #f3f4f6;
  font-weight: 600;
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

  .qa-message.is-user .qa-bubble {
    max-width: 88%;
  }

  .qa-bubble {
    font-size: 13px;
    padding: 9px 10px;
  }

  .qa-message.is-assistant .qa-bubble {
    max-width: 100%;
  }

  .qa-desc {
    font-size: 12px;
  }
}
</style>

