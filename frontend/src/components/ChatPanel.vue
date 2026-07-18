<template>
  <div class="chat-panel">
    <aside class="chat-context">
      <span class="context-icon"><Bot :size="20" /></span>
      <h4>合同分析助手</h4>
      <p>可继续追问差异原因、风险影响与处理建议，回答将结合当前任务数据。</p>
      <div class="quick-title"><WandSparkles :size="13" />推荐追问</div>
      <button
        v-for="question in quickQuestions"
        :key="question"
        type="button"
        :disabled="sending"
        @click="sendQuestion(question)"
      >
        {{ question }}<ArrowUpRight :size="13" />
      </button>
    </aside>

    <section class="conversation">
      <div ref="listRef" class="chat-messages no-scrollbar" aria-live="polite">
        <div v-if="!messages.length && !sending" class="chat-empty">
          <span><MessageCircleMore :size="22" /></span>
          <strong>从一个问题开始</strong>
          <p>选择左侧推荐问题，或直接输入你关心的合同条款。</p>
        </div>
        <div
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role.toLowerCase()"
        >
          <span v-if="message.role !== 'USER'" class="assistant-avatar"
            ><Bot :size="15"
          /></span>
          <div class="message-group">
            <small>{{ message.role === "USER" ? "我" : "AI 分析助手" }}</small>
            <div v-if="message.role === 'USER'" class="message user-message">
              {{ message.content }}
            </div>
            <div v-else class="message assistant-message">
              <StructuredAssistantMessage :content="message.content" />
            </div>
          </div>
        </div>
        <div v-if="sending" class="message-row assistant">
          <span class="assistant-avatar"><Bot :size="15" /></span>
          <div class="message-group">
            <small>AI 分析助手</small>
            <div class="message assistant-message typing">
              <i /><i /><i /><span>正在结合合同内容分析</span>
            </div>
          </div>
        </div>
      </div>

      <div class="composer">
        <div class="composer-box">
          <textarea
            v-model="content"
            rows="1"
            placeholder="追问合同差异、风险或处理建议…"
            :disabled="sending"
            @keydown.enter.exact.prevent="onSend"
          />
          <button
            type="button"
            :disabled="sending || !content.trim()"
            aria-label="发送追问"
            @click="onSend"
          >
            <SendHorizontal :size="17" />
          </button>
        </div>
        <small>按 Enter 发送 · AI 结果仅供业务复核参考</small>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import {
  ArrowUpRight,
  Bot,
  MessageCircleMore,
  SendHorizontal,
  WandSparkles,
} from "lucide-vue-next";
import { taskApi } from "@/api/task";
import { useToastStore } from "@/stores/toast";
import StructuredAssistantMessage from "@/components/StructuredAssistantMessage.vue";

const props = defineProps<{ taskId: number }>();
const toast = useToastStore();
const messages = ref<any[]>([]);
const content = ref("");
const sending = ref(false);
const listRef = ref<HTMLElement | null>(null);
const quickQuestions = [
  "本次最高风险是什么？",
  "金额差异会带来什么影响？",
  "提交采购申请前还需确认什么？",
];

async function scrollToBottom() {
  await nextTick();
  if (listRef.value)
    listRef.value.scrollTo({
      top: listRef.value.scrollHeight,
      behavior: "smooth",
    });
}
async function load() {
  try {
    const response: any = await taskApi.messages(props.taskId, {
      page: 1,
      size: 50,
    });
    messages.value = response.data.items || [];
    await scrollToBottom();
  } catch {
    // The HTTP layer owns API feedback.
  }
}
async function sendQuestion(question?: string) {
  const text = (question || content.value).trim();
  if (!text || sending.value) return;
  messages.value.push({ id: Date.now(), role: "USER", content: text });
  content.value = "";
  sending.value = true;
  await scrollToBottom();
  try {
    const response: any = await taskApi.chat(props.taskId, text);
    messages.value.push({
      id: Date.now() + 1,
      role: "ASSISTANT",
      content: response.data.reply,
    });
    await scrollToBottom();
  } catch {
    toast.error("追问失败，请稍后重试");
  } finally {
    sending.value = false;
  }
}
function onSend() {
  sendQuestion();
}
onMounted(load);
</script>

<style scoped>
.chat-panel {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 520px;
  background: #fff;
}
.chat-context {
  padding: 22px 18px;
  border-right: 0;
  background: #f7f9fc;
}
.context-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(145deg, #398bf1, #1769e0);
  box-shadow: 0 10px 22px rgba(23, 105, 224, 0.2);
  color: white;
}
.chat-context h4 {
  margin: 14px 0 5px;
  font-size: 15px;
}
.chat-context > p {
  margin: 0;
  color: #526176;
  font-size: 12px;
  line-height: 1.7;
}
.quick-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 22px 0 8px;
  color: #617087;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.04em;
}
.chat-context button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 7px;
  padding: 10px 11px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.85);
  color: #344054;
  font: inherit;
  font-size: 12px;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}
.chat-context button:hover:not(:disabled) {
  transform: translateX(2px);
  border-color: #bed5f5;
  background: white;
  color: var(--primary);
  box-shadow: 0 7px 18px rgba(25, 70, 125, 0.07);
}
.conversation {
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.chat-messages {
  flex: 1;
  max-height: 560px;
  min-height: 390px;
  overflow-y: auto;
  padding: 22px;
}
.chat-empty {
  height: 100%;
  min-height: 285px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  text-align: center;
}
.chat-empty > span {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: #eef5ff;
  color: var(--primary);
}
.chat-empty strong {
  margin-top: 12px;
  font-size: 15px;
}
.chat-empty p {
  margin: 5px 0 0;
  color: var(--muted-foreground);
  font-size: 13px;
}
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-bottom: 18px;
}
.message-row.user {
  justify-content: flex-end;
}
.assistant-avatar {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #e8f1ff;
  color: var(--primary);
}
.message-group {
  max-width: min(78%, 680px);
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.message-row.user .message-group {
  align-items: flex-end;
}
.message-group > small {
  color: #748197;
  font-size: 11px;
}
.message {
  padding: 13px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
}
.user-message {
  border-bottom-right-radius: 5px;
  background: linear-gradient(145deg, #2f80ed, #1769e0);
  box-shadow: 0 8px 18px rgba(23, 105, 224, 0.16);
  color: white;
}
.assistant-message {
  border: 0;
  border-top-left-radius: 5px;
  background: #f4f7fb;
  color: var(--foreground);
}
.assistant-message :deep(p) {
  margin: 0.3em 0;
}
.assistant-message :deep(p:first-child) {
  margin-top: 0;
}
.assistant-message :deep(p:last-child) {
  margin-bottom: 0;
}
.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--muted-foreground);
}
.typing i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--primary);
  animation: typing 1.1s infinite ease-in-out;
}
.typing i:nth-child(2) {
  animation-delay: 0.15s;
}
.typing i:nth-child(3) {
  animation-delay: 0.3s;
}
.typing span {
  margin-left: 5px;
  font-size: 12px;
}
@keyframes typing {
  50% {
    transform: translateY(-3px);
    opacity: 0.45;
  }
}
.composer {
  padding: 12px 18px 18px;
  border-top: 0;
  background: #fff;
}
.composer-box {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  min-height: 76px;
  padding: 12px 10px 10px 15px;
  border: 1px solid transparent;
  border-radius: 16px;
  background: white;
  box-shadow: inset 0 0 0 1px #c9d8eb;
  transition: all 0.2s ease;
}
.composer-box:focus-within {
  border-color: #a9c9f5;
  box-shadow: 0 0 0 4px rgba(23, 105, 224, 0.07);
}
.composer textarea {
  flex: 1;
  min-width: 0;
  max-height: 90px;
  padding: 5px 0;
  border: 0;
  outline: 0;
  resize: none;
  background: transparent;
  color: var(--foreground);
  font: inherit;
  min-height: 44px;
  font-size: 14px;
  line-height: 1.6;
}
.composer button {
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 11px;
  border: 1px solid #c9ddfb;
  background: #e8f1ff;
  color: #155fc9;
  cursor: pointer;
  transition: all 0.2s ease;
}
.composer button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #dceaff;
  box-shadow: 0 7px 15px rgba(23, 105, 224, 0.12);
}
.composer button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.composer > small {
  display: block;
  margin: 7px 4px 0;
  color: #9aa5b3;
  font-size: 11px;
}
@media (max-width: 760px) {
  .chat-panel {
    grid-template-columns: 1fr;
  }
  .chat-context {
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }
  .chat-context button {
    display: inline-flex;
    width: auto;
    margin-right: 6px;
  }
  .message-group {
    max-width: 88%;
  }
}
</style>
