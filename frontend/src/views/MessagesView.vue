<template>
  <div class="page messages-page">
    <div class="section-title">
      <div>
        <h2>好友消息</h2>
        <p class="muted">添加好友、处理申请，并和好友实时聊天。</p>
      </div>
      <el-button :icon="RefreshCcw" @click="loadAll">刷新</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="8" :lg="7">
        <el-card class="side-card">
          <div class="side-head">
            <h3>好友</h3>
            <el-button type="primary" :icon="UserPlus" @click="openAddFriend">添加好友</el-button>
          </div>

          <div class="panel-block">
            <div class="block-title">
              <span>好友申请</span>
              <el-badge v-if="requests.length" :value="requests.length" />
            </div>
            <div v-if="requests.length" class="request-list">
              <div v-for="request in requests" :key="request.id" class="request-row">
                <div class="request-info">
                  <el-avatar :size="34">用</el-avatar>
                  <div>
                    <strong>用户 {{ request.fromUserId }}</strong>
                    <span>请求添加你为好友</span>
                  </div>
                </div>
                <div class="request-actions">
                  <el-button size="small" type="primary" @click="accept(request.id)">同意</el-button>
                  <el-button size="small" @click="reject(request.id)">拒绝</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无好友申请" :image-size="72" />
          </div>

          <el-divider />

          <div class="panel-block">
            <div class="block-title">
              <span>好友列表</span>
              <span class="count">{{ friends.length }} 人</span>
            </div>
            <div v-if="friends.length" class="friend-list">
              <div
                v-for="friend in friends"
                :key="friend.id"
                class="friend-row"
                :class="{ active: selected?.id === friend.id }"
                @click="selectFriend(friend)"
              >
                <el-avatar :src="friend.avatarUrl">{{ avatarText(friend) }}</el-avatar>
                <div>
                  <strong>{{ friend.nickname || friend.username }}</strong>
                  <span>{{ friend.city || friend.username }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="还没有好友，先添加一个吧" :image-size="82" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16" :lg="17">
        <el-card class="chat-card">
          <template v-if="selected">
            <div class="chat-head">
              <el-avatar :src="selected.avatarUrl">{{ avatarText(selected) }}</el-avatar>
              <div>
                <h3>和 {{ selected.nickname || selected.username }} 聊天</h3>
                <span :class="['connect-status', connected ? 'online' : 'offline']">
                  {{ connected ? '聊天连接已就绪' : '聊天连接未就绪' }}
                </span>
              </div>
            </div>
            <div ref="messageListRef" class="messages">
              <div
                v-for="message in messages"
                :key="message.id || `${message.senderId}-${message.createTime}`"
                :class="['bubble', message.senderId === currentUser.id ? 'mine' : '']"
              >
                <span>{{ message.content }}</span>
              </div>
              <el-empty v-if="messages.length === 0" description="还没有聊天记录" :image-size="96" />
            </div>
            <div class="send-box">
              <el-input
                v-model="content"
                placeholder="输入消息，按 Enter 发送"
                maxlength="500"
                show-word-limit
                @keyup.enter="send"
              />
              <el-button type="primary" :icon="Send" @click="send">发送</el-button>
            </div>
          </template>
          <div v-else class="chat-empty">
            <el-empty description="选择一个好友开始聊天" />
            <el-button type="primary" :icon="UserPlus" @click="openAddFriend">添加好友</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="addDialogVisible" title="添加好友" width="560px">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="输入用户名或昵称"
          clearable
          @keyup.enter="searchUsers"
        />
        <el-button type="primary" :icon="Search" :loading="searching" @click="searchUsers">搜索</el-button>
      </div>

      <div class="search-results">
        <div v-for="item in searchResults" :key="item.user.id" class="search-row">
          <div class="user-info">
            <el-avatar :src="item.user.avatarUrl">{{ avatarText(item.user) }}</el-avatar>
            <div>
              <strong>{{ item.user.nickname || item.user.username }}</strong>
              <span>@{{ item.user.username }} · {{ item.user.city || '未填写城市' }}</span>
            </div>
          </div>
          <el-tag v-if="item.friendStatus === 'SELF'" type="info">自己</el-tag>
          <el-tag v-else-if="item.friendStatus === 'FRIEND'" type="success">已是好友</el-tag>
          <el-tag v-else-if="item.friendStatus === 'PENDING'" type="warning">待通过</el-tag>
          <el-button v-else size="small" type="primary" @click="sendFriendRequest(item)">添加</el-button>
        </div>
        <el-empty v-if="hasSearched && searchResults.length === 0" description="没有找到用户" :image-size="88" />
        <el-empty v-if="!hasSearched" description="搜索用户后可以发送好友申请" :image-size="88" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshCcw, Search, Send, UserPlus } from 'lucide-vue-next'
import http from '../api'

const route = useRoute()
const friends = ref([])
const requests = ref([])
const messages = ref([])
const selected = ref(null)
const content = ref('')
const connected = ref(false)
const addDialogVisible = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const searching = ref(false)
const hasSearched = ref(false)
const messageListRef = ref(null)
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
let socket

onMounted(async () => {
  await loadAll()
  connect()
  const friendId = Number(route.query.friendId)
  if (friendId) {
    const friend = friends.value.find((item) => item.id === friendId)
    if (friend) selectFriend(friend)
  }
})

onBeforeUnmount(() => socket?.close())

async function loadAll() {
  ;[friends.value, requests.value] = await Promise.all([
    http.get('/friends'),
    http.get('/friends/requests')
  ])
}

async function selectFriend(friend) {
  selected.value = friend
  messages.value = await http.get('/messages', { params: { friendId: friend.id } })
  scrollMessages()
}

function connect() {
  const token = localStorage.getItem('token')
  if (!token) return
  const base = import.meta.env.VITE_WS_BASE || 'ws://localhost:8080/ws/chat'
  socket = new WebSocket(`${base}?token=${encodeURIComponent(token)}`)
  socket.onopen = () => {
    connected.value = true
  }
  socket.onclose = () => {
    connected.value = false
  }
  socket.onerror = () => {
    connected.value = false
  }
  socket.onmessage = async (event) => {
    const message = JSON.parse(event.data)
    if (message.type === 'FRIEND_UPDATED') {
      await refreshFriendState(message.relatedUserId)
      return
    }
    if (message.error) {
      ElMessage.error(message.error)
      return
    }
    if (selected.value && (message.senderId === selected.value.id || message.receiverId === selected.value.id)) {
      messages.value.push(message)
      scrollMessages()
    }
  }
}

async function refreshFriendState(relatedUserId) {
  await loadAll()
  if (addDialogVisible.value && searchKeyword.value.trim()) {
    await searchUsers()
  }
  if (relatedUserId && (!selected.value || selected.value.id !== relatedUserId)) {
    const friend = friends.value.find((item) => item.id === relatedUserId)
    if (friend && Number(route.query.friendId) === relatedUserId) {
      await selectFriend(friend)
    }
  }
}

function send() {
  const text = content.value.trim()
  if (!selected.value || !text) return
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    ElMessage.warning('聊天连接未就绪')
    return
  }
  socket.send(JSON.stringify({ receiverId: selected.value.id, content: text }))
  content.value = ''
}

async function accept(id) {
  await http.post(`/friends/requests/${id}/accept`)
  ElMessage.success('已添加好友')
  await loadAll()
}

async function reject(id) {
  await http.post(`/friends/requests/${id}/reject`)
  ElMessage.success('已拒绝申请')
  await loadAll()
}

function openAddFriend() {
  addDialogVisible.value = true
}

async function searchUsers() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    ElMessage.warning('请输入用户名或昵称')
    return
  }
  searching.value = true
  hasSearched.value = true
  try {
    searchResults.value = await http.get('/users/search', { params: { keyword } })
  } finally {
    searching.value = false
  }
}

async function sendFriendRequest(item) {
  await http.post('/friends/requests', { toUserId: item.user.id })
  ElMessage.success('好友申请已发送')
  item.friendStatus = 'PENDING'
  await Promise.all([
    loadAll(),
    searchKeyword.value.trim() ? searchUsers() : Promise.resolve()
  ])
}

function avatarText(user) {
  return user?.nickname?.[0] || user?.username?.[0] || '友'
}

function scrollMessages() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.messages-page {
  padding-bottom: 32px;
}

.section-title {
  align-items: flex-end;
}

.side-card,
.chat-card {
  min-height: 650px;
}

.side-head,
.block-title,
.chat-head,
.search-bar,
.user-info,
.request-info {
  display: flex;
  align-items: center;
}

.side-head {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.side-head h3,
.chat-head h3 {
  margin: 0;
}

.panel-block {
  min-height: 120px;
}

.block-title {
  justify-content: space-between;
  margin-bottom: 10px;
  color: var(--heading-color);
  font-weight: 700;
}

.count,
.request-info span,
.friend-row span,
.user-info span,
.connect-status {
  color: var(--text-muted);
  font-size: 13px;
}

.friend-list,
.request-list,
.search-results {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.friend-row,
.request-row,
.search-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 16px;
  background: var(--glass-bg-soft);
  backdrop-filter: blur(18px);
}

.friend-row {
  cursor: pointer;
}

.friend-row > div,
.request-info > div,
.user-info > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.friend-row.active,
.friend-row:hover {
  border-color: rgba(94, 234, 212, 0.56);
  background: rgba(94, 234, 212, 0.14);
}

.request-row,
.search-row {
  justify-content: space-between;
}

.request-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.chat-card {
  display: flex;
  flex-direction: column;
}

.chat-head {
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
}

.connect-status.online {
  color: var(--accent);
}

.connect-status.offline {
  color: var(--danger);
}

.messages {
  height: 480px;
  overflow-y: auto;
  padding: 14px;
  margin-top: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background:
    radial-gradient(circle at 24% 18%, rgba(94, 234, 212, 0.12), transparent 30%),
    var(--glass-bg-soft);
  backdrop-filter: blur(18px);
}

.bubble {
  display: flex;
  margin: 8px 0;
}

.bubble span {
  max-width: 70%;
  padding: 10px 12px;
  color: var(--text-main);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 16px;
  background: var(--glass-bg-soft);
  line-height: 1.6;
  word-break: break-word;
}

.bubble.mine {
  justify-content: flex-end;
}

.bubble.mine span {
  color: #fff;
  border-color: rgba(94, 234, 212, 0.58);
  background: linear-gradient(135deg, #0f766e, #2563eb);
}

.send-box {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.chat-empty {
  min-height: 570px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.search-bar {
  gap: 10px;
}

.search-results {
  min-height: 180px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .chat-card {
    margin-top: 16px;
  }

  .send-box,
  .search-bar {
    flex-direction: column;
  }

  .messages {
    height: 360px;
  }
}
</style>
