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
              <div
                v-if="
                  !item.thinkingCompleted &&
                  ((item.pending && !item.answerStarted) || thinkingSteps(item).length)
                "
                class="qa-thinking qa-thinking--live"
              >
                <div class="qa-thinking-head">
                  <span class="qa-thinking-pulse" aria-hidden="true" />
                  <span>正在准备回答</span>
                </div>
                <ol v-if="thinkingSteps(item).length" class="qa-thinking-steps">
                  <li v-for="(st, si) in thinkingSteps(item)" :key="'live-' + si" class="qa-thinking-step">
                    <span class="qa-thinking-step-title">{{ st.title }}</span>
                    <div class="qa-thinking-step-detail">{{ st.detail }}</div>
                  </li>
                </ol>
              </div>
              <div
                v-if="thinkingSteps(item).length && item.thinkingCompleted"
                class="qa-thinking-bar"
              >
                <button
                  type="button"
                  class="qa-thinking-toggle"
                  @click="toggleThinking(item)"
                >
                  <span class="qa-thinking-toggle-chevron" :class="{ 'is-open': item.thinkingExpanded }" />
                  <span>{{ item.thinkingExpanded ? '收起思考过程' : '查看思考过程' }}</span>
                  <span class="qa-thinking-meta">{{ thinkingSteps(item).length }} 步</span>
                </button>
              </div>
              <transition name="qa-thinking-reveal">
                <div
                  v-show="
                    thinkingSteps(item).length &&
                    item.thinkingCompleted &&
                    item.thinkingExpanded
                  "
                  class="qa-thinking qa-thinking--folded"
                >
                  <ol class="qa-thinking-steps">
                    <li
                      v-for="(st, si) in thinkingSteps(item)"
                      :key="'fold-' + si"
                      class="qa-thinking-step"
                    >
                      <span class="qa-thinking-step-title">{{ st.title }}</span>
                      <div class="qa-thinking-step-detail">{{ st.detail }}</div>
                    </li>
                  </ol>
                </div>
              </transition>
              <div
                class="qa-bubble-content markdown-body"
                v-html="
                  renderMarkdown(item.content || (item.pending ? '正在生成回答…' : ''))
                "
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
import { askQuestionStream, askQuestion } from '../../api/qa';

const QA_TAB_ID_KEY = 'tan_qa_tab_id';
const QA_STORAGE_PREFIX = 'tan_qa_session_v1_';

export default {
  name: 'TanQA',
  data() {
    return {
      inputText: '',
      loading: false,
      messages: [],
      sessionId: ''
    };
  },
  created() {
    this.ensureTabIsolation();
    this.restoreSession();
  },
  methods: {
    getStorageKey() {
      const tabId = window.sessionStorage.getItem(QA_TAB_ID_KEY) || '';
      return `${QA_STORAGE_PREFIX}${tabId}`;
    },
    ensureTabIsolation() {
      try {
        let tabId = window.sessionStorage.getItem(QA_TAB_ID_KEY);
        if (!tabId) {
          tabId = `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
          window.sessionStorage.setItem(QA_TAB_ID_KEY, tabId);
        }
      } catch (e) {}
    },
    renderMarkdown(raw) {
      if (!raw) return '';
      const html = marked.parse(raw, { breaks: true });
      return DOMPurify.sanitize(html);
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
      const base = {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        role,
        content: content || '',
        pending: !!pending
      };
      if (role === 'assistant') {
        base.thinking = null;
        base.thinkingExpanded = false;
        base.thinkingCompleted = false;
        base.answerStarted = false;
      }
      return base;
    },
    toggleThinking(item) {
      if (!item || item.role !== 'assistant') return;
      this.$set(item, 'thinkingExpanded', !item.thinkingExpanded);
    },
    thinkingSteps(item) {
      if (!item || item.role !== 'assistant') return [];
      const t = item.thinking;
      const steps = t && t.steps;
      return Array.isArray(steps) ? steps : [];
    },
    parseThinkingPayload(raw) {
      if (!raw || typeof raw !== 'string') return null;
      try {
        const o = JSON.parse(raw);
        if (o && Array.isArray(o.steps)) return o;
      } catch (e) {}
      return { steps: [{ title: '说明', detail: String(raw) }] };
    },
    parseThinkingStreamInit(raw) {
      if (!raw || typeof raw !== 'string') return { steps: [] };
      try {
        const o = JSON.parse(raw);
        if (!o || !Array.isArray(o.steps)) return { steps: [] };
        const steps = o.steps.map(() => ({ title: '', detail: '' }));
        return { steps };
      } catch (e) {
        return { steps: [] };
      }
    },
    parseThinkingChunk(raw) {
      if (!raw || typeof raw !== 'string') return null;
      try {
        const o = JSON.parse(raw);
        const idx = Number(o && o.step_index);
        const field = o && o.field;
        const char = o && o.char;
        if (!Number.isInteger(idx) || idx < 0) return null;
        if (field !== 'title' && field !== 'detail') return null;
        if (typeof char !== 'string') return null;
        return { stepIndex: idx, field, char };
      } catch (e) {
        return null;
      }
    },
    applyThinkingChunk(item, chunk) {
      if (!item || !chunk) return;
      const t = item.thinking && Array.isArray(item.thinking.steps) ? item.thinking : { steps: [] };
      while (t.steps.length <= chunk.stepIndex) {
        t.steps.push({ title: '', detail: '' });
      }
      const step = t.steps[chunk.stepIndex];
      const prev = typeof step[chunk.field] === 'string' ? step[chunk.field] : '';
      this.$set(step, chunk.field, prev + chunk.char);
      this.$set(t.steps, chunk.stepIndex, step);
      this.$set(item, 'thinking', t);
    },
    scheduleThinkingUiSync(item) {
      if (!item) return;
      if (item._thinkingUiTimer) return;
      item._thinkingUiTimer = setTimeout(() => {
        item._thinkingUiTimer = null;
        this.persistSession();
        this.scrollToBottom();
      }, 180);
    },
    persistSession() {
      try {
        const now = Date.now();
        if (this._persistThrottleUntil && now < this._persistThrottleUntil) return;
        this._persistThrottleUntil = now + 120;
        const payload = {
          sessionId: this.sessionId || '',
          messages: this.messages
        };
        window.sessionStorage.setItem(this.getStorageKey(), JSON.stringify(payload));
      } catch (e) {}
    },
    restoreSession() {
      try {
        const raw = window.sessionStorage.getItem(this.getStorageKey());
        if (!raw) return;
        const data = JSON.parse(raw);
        this.sessionId = (data && data.sessionId) || '';
        const rawMessages = Array.isArray(data && data.messages) ? data.messages : [];
        this.messages = rawMessages.map((msg) => {
          if (!msg || msg.role !== 'assistant') return msg;
          const normalized = { ...msg };
          if (typeof normalized.thinkingCompleted !== 'boolean') {
            normalized.thinkingCompleted = !!(
              normalized.thinking &&
              (normalized.answerStarted || !normalized.pending)
            );
          }
          return normalized;
        });
      } catch (e) {
        this.sessionId = '';
        this.messages = [];
      }
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
      this.persistSession();
      this.scrollToBottom();

      try {
        let hasStreamContent = false;
        await askQuestionStream(q, this.sessionId, (event) => {
          if (!event || !event.type) return;
          if (event.type === 'session' && event.session_id) {
            this.sessionId = event.session_id;
            this.persistSession();
            return;
          }
          if (event.type === 'thinking' && event.content) {
            this.$set(assistantMessage, 'thinking', this.parseThinkingPayload(event.content));
            this.$set(assistantMessage, 'thinkingCompleted', true);
            this.persistSession();
            this.scrollToBottom();
            return;
          }
          if (event.type === 'thinking_init') {
            const t = this.parseThinkingStreamInit(event.content);
            this.$set(assistantMessage, 'thinking', t);
            this.$set(assistantMessage, 'thinkingCompleted', false);
            this.persistSession();
            this.scrollToBottom();
            return;
          }
          if (event.type === 'thinking_chunk') {
            const chunk = this.parseThinkingChunk(event.content);
            if (!chunk) return;
            this.applyThinkingChunk(assistantMessage, chunk);
            this.scheduleThinkingUiSync(assistantMessage);
            return;
          }
          if (event.type === 'thinking_done') {
            this.$set(assistantMessage, 'thinkingCompleted', true);
            this.persistSession();
            this.scrollToBottom();
            return;
          }
          if (event.type === 'answer') {
            if (event.content && !assistantMessage.answerStarted) {
              this.$set(assistantMessage, 'answerStarted', true);
            }
            assistantMessage.pending = false;
            assistantMessage.content += event.content || '';
            hasStreamContent = true;
            this.persistSession();
            this.scrollToBottom();
            return;
          }
          if (event.type === 'done') {
            assistantMessage.pending = false;
            if (assistantMessage.thinking && !assistantMessage.thinkingCompleted) {
              this.$set(assistantMessage, 'thinkingCompleted', true);
            }
            this.persistSession();
          }
        });
        if (!assistantMessage.content && !hasStreamContent) {
          assistantMessage.pending = false;
          assistantMessage.content = '暂无回答内容';
        }
      } catch (e) {
        this.$message.info('流式请求异常，正在改用普通模式重试…');
        try {
          const res = await askQuestion(q, this.sessionId);
          assistantMessage.pending = false;
          assistantMessage.content = (res.data && res.data.data) || '暂无回答内容';
          const sid = res.data && res.data.session_id;
          if (sid) this.sessionId = sid;
          const th = res.data && res.data.thinking;
          if (th) {
            this.$set(assistantMessage, 'thinking', this.parseThinkingPayload(th));
            this.$set(assistantMessage, 'thinkingCompleted', true);
            this.$set(assistantMessage, 'answerStarted', true);
          }
        } catch (e2) {
          assistantMessage.pending = false;
          assistantMessage.content = '请求失败，请稍后重试。' + (e2.message || e.message || '');
        }
      } finally {
        this.loading = false;
        this.persistSession();
        this.scrollToBottom();
      }
    },
    handleClear() {
      this.inputText = '';
      this.messages = [];
      this.sessionId = '';
      this.loading = false;
      try {
        window.sessionStorage.removeItem(this.getStorageKey());
      } catch (e) {}
    }
  }
};
</script>

<style scoped>
.qa-card {
  max-width: 900px;
  margin: 0 auto;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  border: 1px solid #e6ecf5;
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
  padding: 16px 10px 10px;
  border: 1px solid #e8eef7;
  border-radius: 12px;
  background: #f7f9fc;
}

.qa-empty {
  color: #8a919f;
  text-align: center;
  padding: 26px 12px;
  font-size: 13px;
}

.qa-message {
  display: flex;
  margin-bottom: 8px;
}

.qa-message.is-user {
  justify-content: flex-end;
}

.qa-message.is-assistant {
  justify-content: flex-start;
}

.qa-bubble {
  width: fit-content;
  max-width: min(82%, 740px);
  border-radius: 16px;
  padding: 10px 12px;
  line-height: 1.46;
  font-size: 14px;
  text-align: left;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.qa-message.is-user .qa-bubble {
  background: linear-gradient(135deg, #4f8cff 0%, #3a73ea 100%);
  color: #fff;
  border-bottom-right-radius: 8px;
  box-shadow: 0 4px 16px rgba(58, 115, 234, 0.25);
}

.qa-message.is-assistant .qa-bubble {
  background: #ffffff;
  color: #2f3441;
  border: 1px solid #e7edf6;
  border-bottom-left-radius: 8px;
  box-shadow: 0 1px 4px rgba(30, 41, 59, 0.05);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.qa-thinking {
  font-size: 12.5px;
  line-height: 1.45;
  color: #64748b;
  background: linear-gradient(145deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
}

.qa-thinking--live {
  color: #64748b;
}

.qa-thinking-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 0.02em;
  color: #475569;
  margin-bottom: 8px;
}

.qa-thinking-pulse {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #94a3b8;
  animation: qa-pulse 1.2s ease-in-out infinite;
}

@keyframes qa-pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.92);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.qa-thinking-steps {
  margin: 0;
  padding-left: 1.15em;
}

.qa-thinking-step {
  margin-bottom: 6px;
}

.qa-thinking-step:last-child {
  margin-bottom: 0;
}

.qa-thinking-step-title {
  display: block;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0;
  line-height: 1.35;
}

.qa-thinking-step-detail {
  margin: 0;
  margin-top: 2px;
  padding: 0;
  color: #64748b;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}

.qa-thinking-bar {
  margin: 0;
}

.qa-thinking-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 2px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  color: #94a3b8;
  transition: color 0.15s ease;
}

.qa-thinking-toggle:hover {
  color: #64748b;
}

.qa-thinking-toggle-chevron {
  display: inline-block;
  width: 0;
  height: 0;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
  border-left: 5px solid currentColor;
  transform: rotate(0deg);
  transition: transform 0.2s ease;
}

.qa-thinking-toggle-chevron.is-open {
  transform: rotate(90deg);
}

.qa-thinking-meta {
  margin-left: 4px;
  opacity: 0.85;
  font-size: 11px;
}

.qa-thinking--folded {
  margin-top: 2px;
}

.qa-thinking-reveal-enter-active,
.qa-thinking-reveal-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.qa-thinking-reveal-enter,
.qa-thinking-reveal-leave-to {
  opacity: 0;
  transform: translateY(-4px);
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
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  line-height: 1.5;
}

.qa-bubble-content.markdown-body >>> h1,
.qa-bubble-content.markdown-body >>> h2,
.qa-bubble-content.markdown-body >>> h3 {
  margin: 0.2em 0 0.12em;
  font-weight: 600;
  line-height: 1.28;
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
  margin: 0.08em 0;
  text-align: left;
  line-height: 1.42;
}

.qa-bubble-content.markdown-body >>> p + p {
  margin-top: 0.12em;
}

.qa-bubble-content.markdown-body >>> ul,
.qa-bubble-content.markdown-body >>> ol {
  margin: 0.08em 0 0.08em 1.05em;
  padding: 0;
  line-height: 1.38;
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
  margin: 0.18em 0;
}

.qa-bubble-content.markdown-body >>> pre code {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: 0.85em;
}

.qa-bubble-content.markdown-body >>> blockquote {
  margin: 0.14em 0;
  padding-left: 0.75em;
  border-left: 3px solid #dcdfe6;
  color: #606266;
  text-align: left;
}

.qa-bubble-content.markdown-body >>> table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.14em 0;
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

