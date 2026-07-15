<template>
  <div>
    <div class="chat-msgs no-scrollbar" ref="listRef">
      <div v-for="m in messages" :key="m.id">
        <!-- User message -->
        <div v-if="m.role === 'USER'" class="flex justify-end">
          <div class="msg-user">{{ m.content }}</div>
        </div>
        <!-- AI reply -->
        <div v-else class="flex justify-start gap-3">
          <div class="msg-avatar">AI</div>
          <div class="msg-ai" v-html="renderMd(m.content)"></div>
        </div>
      </div>
      <div v-if="sending" class="flex justify-start gap-3">
        <div class="msg-avatar">AI</div>
        <div class="msg-ai" style="opacity:0.7">思考中…</div>
      </div>
    </div>
    <!-- Input -->
    <div class="chat-input-row">
      <input
        type="text"
        v-model="content"
        placeholder="输入追问内容..."
        @keydown.enter.prevent="onSend"
        :disabled="sending"
      />
      <button class="chat-send" type="button" :disabled="sending || !content.trim()" @click="onSend">
        <SendHorizontal :size="18" />
      </button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { SendHorizontal } from 'lucide-vue-next'
import { marked } from 'marked'
import { taskApi } from '@/api/task'
import { useToastStore } from '@/stores/toast'

const props = defineProps<{ taskId: number }>()
const toast = useToastStore()
const messages = ref<any[]>([])
const content = ref('')
const sending = ref(false)
const listRef = ref<HTMLElement | null>(null)

function renderMd(s: string) {
  try { return marked.parse(s || '') } catch { return s }
}
async function scrollToBottom() {
  await nextTick()
  if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
}
async function load() {
  try {
    const res: any = await taskApi.messages(props.taskId, { page: 1, size: 50 })
    messages.value = res.data.items || []
    await scrollToBottom()
  } catch { /* 拦截器已提示 */ }
}
async function onSend() {
  const text = content.value.trim()
  if (!text || sending.value) return
  messages.value.push({ id: Date.now(), role: 'USER', content: text })
  content.value = ''
  sending.value = true
  await scrollToBottom()
  try {
    const res: any = await taskApi.chat(props.taskId, text)
    messages.value.push({ id: Date.now() + 1, role: 'ASSISTANT', content: res.data.reply })
    await scrollToBottom()
  } catch {
    toast.error('追问失败')
  } finally {
    sending.value = false
  }
}
onMounted(load)
</script>
<style scoped>
/* 拷贝自 design/pages/任务详情.html 的对话追问区样式 */
.chat-msgs {
  display: flex; flex-direction: column; gap: calc(var(--spacing) * 4);
  padding: calc(var(--spacing) * 5);
  max-height: 420px; overflow-y: auto;
}
.msg-user {
  max-width: 70%; padding: 12px 16px; font-size: 0.875rem;
  border-radius: calc(var(--radius) + 4px) calc(var(--radius) + 4px) 4px calc(var(--radius) + 4px);
  background: var(--primary); color: var(--primary-foreground);
}
.msg-ai {
  max-width: 70%; padding: 12px 16px; font-size: 0.875rem; line-height: 1.6;
  border-radius: 4px calc(var(--radius) + 4px) calc(var(--radius) + 4px) calc(var(--radius) + 4px);
  background: var(--muted); color: var(--foreground);
}
.msg-ai :deep(p) { margin: 0.3em 0; }
.msg-ai :deep(p:first-child) { margin-top: 0; }
.msg-ai :deep(p:last-child) { margin-bottom: 0; }
.msg-avatar {
  width: 32px; height: 32px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; background: var(--accent); color: var(--accent-foreground);
  font-weight: 700; font-size: 12px;
}
.chat-input-row {
  display: flex; align-items: center; gap: 12px;
  padding: calc(var(--spacing) * 5);
  border-top: 1px solid var(--border);
}
.chat-input-row input {
  flex: 1; min-width: 0;
  padding: 10px 16px; font-size: 0.875rem;
  border: 1px solid var(--input); border-radius: var(--radius);
  background: var(--popover); color: var(--foreground);
  outline: none;
}
.chat-input-row input:focus { border-color: var(--primary); }
.chat-send {
  width: 40px; height: 40px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius); border: none; cursor: pointer;
  background: var(--primary); color: var(--primary-foreground);
}
.chat-send:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
