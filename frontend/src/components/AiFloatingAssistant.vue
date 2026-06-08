<template>
  <div>
    <el-tooltip content="AI旅行助手" placement="left">
      <button
        class="ai-float-button"
        :class="{ 'is-open': open }"
        :style="buttonStyle"
        type="button"
        aria-label="AI旅行助手"
        @pointerdown="startDrag"
        @click="togglePanel"
      >
        <Sparkles :size="24" />
      </button>
    </el-tooltip>

    <section v-if="open" class="ai-chat-panel" :style="panelStyle">
      <header class="ai-chat-header">
        <div>
          <strong>AI旅行助手</strong>
          <span>问目的地、路线、预算都可以</span>
        </div>
        <div class="ai-chat-actions">
          <el-button circle text :icon="Trash2" :disabled="loading || messages.length === 0" @click="clearMessages" />
          <el-button circle text :icon="X" @click="open = false" />
        </div>
      </header>

      <div ref="messageListRef" class="ai-chat-messages">
        <div v-if="messages.length === 0" class="ai-chat-empty">
          <Sparkles :size="28" />
          <p>想去哪里、玩几天、预算多少？我帮你先理出方向。</p>
        </div>
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="ai-message"
          :class="`is-${message.role}`"
        >
          <div class="ai-message-bubble">{{ message.content }}</div>
        </div>
        <div v-if="loading && waitingForFirstChunk" class="ai-message is-assistant">
          <div class="ai-message-bubble">正在整理建议...</div>
        </div>
      </div>

      <div class="ai-chat-input">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="300"
          show-word-limit
          placeholder="例如：周末从上海出发，想看湖景，预算1500"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <el-button type="primary" :icon="Send" :loading="loading" :disabled="!draft.trim()" @click="sendMessage">
          发送
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { Send, Sparkles, Trash2, X } from 'lucide-vue-next'

const STORAGE_KEY = 'aiAssistantPosition'
const BUTTON_SIZE = 58
const PANEL_WIDTH = 360
const PANEL_HEIGHT = 500
const EDGE_GAP = 18

const open = ref(false)
const draft = ref('')
const loading = ref(false)
const waitingForFirstChunk = ref(false)
const messages = ref([])
const messageListRef = ref(null)
const viewport = ref({ width: 1280, height: 720 })
const position = ref({ x: 0, y: 0 })
let dragState = null
let suppressClick = false

const buttonStyle = computed(() => ({
  left: `${position.value.x}px`,
  top: `${position.value.y}px`
}))

const panelStyle = computed(() => {
  const placeLeft = position.value.x + BUTTON_SIZE + PANEL_WIDTH + EDGE_GAP > viewport.value.width
  const left = placeLeft
    ? Math.max(EDGE_GAP, position.value.x - PANEL_WIDTH - 12)
    : Math.min(viewport.value.width - PANEL_WIDTH - EDGE_GAP, position.value.x + BUTTON_SIZE + 12)
  const top = clamp(position.value.y - 18, EDGE_GAP, viewport.value.height - PANEL_HEIGHT - EDGE_GAP)
  return {
    left: `${left}px`,
    top: `${top}px`
  }
})

onMounted(() => {
  updateViewport()
  restorePosition()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  stopDrag()
})

function updateViewport() {
  viewport.value = {
    width: window.innerWidth,
    height: window.innerHeight
  }
}

function restorePosition() {
  const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  const nextPosition = saved && Number.isFinite(saved.x) && Number.isFinite(saved.y)
    ? saved
    : {
        x: viewport.value.width - BUTTON_SIZE - 28,
        y: viewport.value.height - BUTTON_SIZE - 44
      }
  position.value = clampPosition(nextPosition.x, nextPosition.y)
}

function handleResize() {
  updateViewport()
  position.value = clampPosition(position.value.x, position.value.y)
  savePosition()
}

function startDrag(event) {
  dragState = {
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    originX: position.value.x,
    originY: position.value.y,
    moved: false
  }
  event.currentTarget.setPointerCapture?.(event.pointerId)
  document.addEventListener('pointermove', handleDrag)
  document.addEventListener('pointerup', stopDrag)
}

function handleDrag(event) {
  if (!dragState) return
  const deltaX = event.clientX - dragState.startX
  const deltaY = event.clientY - dragState.startY
  if (Math.abs(deltaX) + Math.abs(deltaY) > 4) {
    dragState.moved = true
  }
  position.value = clampPosition(dragState.originX + deltaX, dragState.originY + deltaY)
}

function stopDrag() {
  if (dragState?.moved) {
    suppressClick = true
    savePosition()
    window.setTimeout(() => {
      suppressClick = false
    }, 0)
  }
  dragState = null
  document.removeEventListener('pointermove', handleDrag)
  document.removeEventListener('pointerup', stopDrag)
}

function togglePanel() {
  if (suppressClick) return
  open.value = !open.value
  if (open.value) {
    nextTick(scrollToBottom)
  }
}

async function sendMessage() {
  const content = draft.value.trim()
  if (!content || loading.value) return

  const history = messages.value.slice(-8)
  messages.value.push({ role: 'user', content })
  draft.value = ''
  loading.value = true
  waitingForFirstChunk.value = true
  await nextTick(scrollToBottom)

  let assistantIndex = -1
  try {
    assistantIndex = messages.value.length
    messages.value.push({ role: 'assistant', content: '' })
    await streamChat(content, history, async (chunk) => {
      waitingForFirstChunk.value = false
      messages.value[assistantIndex] = {
        ...messages.value[assistantIndex],
        content: messages.value[assistantIndex].content + chunk
      }
      await nextTick(scrollToBottom)
    })
    if (!messages.value[assistantIndex].content.trim()) {
      messages.value[assistantIndex] = {
        ...messages.value[assistantIndex],
        content: '我暂时没有整理出合适的回复。'
      }
    }
  } catch (error) {
    const errorMessage = getFriendlyError(error)
    if (assistantIndex >= 0) {
      messages.value[assistantIndex] = {
        ...messages.value[assistantIndex],
        content: errorMessage
      }
    } else {
      messages.value.push({ role: 'assistant', content: errorMessage })
    }
  } finally {
    loading.value = false
    waitingForFirstChunk.value = false
    await nextTick(scrollToBottom)
  }
}

async function streamChat(content, history, onChunk) {
  const token = localStorage.getItem('token')
  const baseURL = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'
  const response = await fetch(`${baseURL}/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ message: content, history })
  })

  if (!response.ok || !response.body) {
    const errorText = await response.text().catch(() => '')
    throw new Error(errorText || `请求失败：${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split(/\r?\n\r?\n/)
    buffer = parts.pop() || ''
    for (const part of parts) {
      await handleSseEvent(part, onChunk)
    }
  }

  if (buffer.trim()) {
    await handleSseEvent(buffer, onChunk)
  }
}

async function handleSseEvent(rawEvent, onChunk) {
  const lines = rawEvent.split(/\r?\n/)
  const event = lines.find((line) => line.startsWith('event:'))?.slice(6).trim() || 'message'
  const dataText = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trim())
    .join('\n')

  if (!dataText) return
  let payload
  try {
    payload = JSON.parse(dataText)
  } catch {
    payload = { content: dataText }
  }
  if (event === 'error') {
    throw new Error(payload.message || 'AI流式调用失败')
  }
  if (event === 'done') {
    return
  }
  if (event === 'message') {
    const content = typeof payload === 'string' ? payload : payload.content
    if (content) {
      await onChunk(content)
    }
  }
}

function getFriendlyError(error) {
  const message = error.message || '请检查后端服务和API Key配置'
  if (message.includes('请先登录')) {
    return 'AI调用失败：请先登录'
  }
  if (message.includes('401') || message.includes('API Key 无效') || message.includes('令牌')) {
    return 'AI调用失败：API Key 无效、过期，或不是当前模型平台的 Key。请检查后端 ai.api-key、ai.chat-url 和 ai.model 配置。'
  }
  if (message.includes('timeout') || message.includes('超时')) {
    return 'AI调用超时：模型生成时间太长，请稍后再试，或把问题说得更具体一点。'
  }
  return `AI调用失败：${message}`
}

function clearMessages() {
  messages.value = []
}

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function savePosition() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(position.value))
}

function clampPosition(x, y) {
  return {
    x: clamp(x, EDGE_GAP, viewport.value.width - BUTTON_SIZE - EDGE_GAP),
    y: clamp(y, EDGE_GAP, viewport.value.height - BUTTON_SIZE - EDGE_GAP)
  }
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), Math.max(min, max))
}
</script>

<style scoped>
.ai-float-button {
  position: fixed;
  z-index: 1000;
  width: 58px;
  height: 58px;
  border: 0;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #0f766e, #2563eb);
  box-shadow: 0 16px 34px rgba(37, 99, 235, 0.28), 0 5px 14px rgba(15, 118, 110, 0.22);
  display: grid;
  place-items: center;
  cursor: grab;
  touch-action: none;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.ai-float-button:hover,
.ai-float-button.is-open {
  transform: translateY(-2px);
  box-shadow: 0 20px 38px rgba(37, 99, 235, 0.34), 0 8px 18px rgba(15, 118, 110, 0.25);
}

.ai-float-button:active {
  cursor: grabbing;
}

.ai-chat-panel {
  position: fixed;
  z-index: 999;
  width: min(360px, calc(100vw - 36px));
  height: min(500px, calc(100vh - 36px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 22px;
  background: var(--panel-bg);
  backdrop-filter: blur(14px) saturate(130%);
  box-shadow: 0 28px 88px rgba(0, 0, 0, 0.42);
}

.ai-chat-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 14px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  background: var(--glass-bg-soft);
}

.ai-chat-header strong,
.ai-chat-header span {
  display: block;
}

.ai-chat-header strong {
  color: var(--accent);
  font-size: 15px;
}

.ai-chat-header span {
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 12px;
}

.ai-chat-actions {
  display: flex;
  flex: 0 0 auto;
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  background:
    radial-gradient(circle at 24% 16%, rgba(94, 234, 212, 0.1), transparent 34%),
    var(--glass-bg-soft);
}

.ai-chat-empty {
  min-height: 100%;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 10px;
  color: var(--text-muted);
  text-align: center;
}

.ai-chat-empty p {
  max-width: 230px;
  margin: 0;
  line-height: 1.7;
}

.ai-message {
  display: flex;
  margin-bottom: 10px;
}

.ai-message.is-user {
  justify-content: flex-end;
}

.ai-message-bubble {
  max-width: 82%;
  padding: 9px 11px;
  border-radius: 16px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-main);
  background: var(--glass-bg-soft);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.ai-message.is-user .ai-message-bubble {
  color: #fff;
  background: linear-gradient(135deg, #0f766e, #2563eb);
  border-color: rgba(94, 234, 212, 0.46);
}

.ai-chat-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
  background: var(--glass-bg-soft);
}

@media (max-width: 560px) {
  .ai-chat-panel {
    left: 18px !important;
    top: auto !important;
    bottom: 88px;
    width: calc(100vw - 36px);
    height: min(470px, calc(100vh - 112px));
  }

  .ai-chat-input {
    grid-template-columns: 1fr;
  }
}
</style>
