<template>
  <el-container class="app-shell">
    <el-header v-if="!isAuthPage" class="topbar">
      <router-link class="brand" to="/home">旅游管理系统</router-link>
      <nav class="nav">
        <router-link to="/home">首页</router-link>
        <router-link v-if="user" to="/flyai-search">旅行搜索</router-link>
        <router-link to="/community">旅游社区</router-link>
        <router-link to="/footprints">灵感足迹</router-link>
        <router-link to="/messages">好友消息</router-link>
        <router-link v-if="user" :to="`/users/${user.id}`">个人主页</router-link>
        <router-link v-if="user?.role === 'ADMIN'" to="/admin">后台管理</router-link>
      </nav>
      <div class="account">
        <span v-if="user">{{ user.nickname || user.username }}</span>
        <el-button :icon="themeIcon" @click="toggleTheme">{{ themeLabel }}</el-button>
        <el-button v-if="user" :icon="LogOut" @click="logout">退出</el-button>
        <el-button v-else type="primary" :icon="LogIn" @click="$router.push('/login')">登录</el-button>
      </div>
    </el-header>
    <el-button v-else class="theme-float" :icon="themeIcon" @click="toggleTheme">{{ themeLabel }}</el-button>
    <el-main>
      <router-view />
    </el-main>
    <AiFloatingAssistant v-if="showAiAssistant" />
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LogIn, LogOut, Moon, Sun } from 'lucide-vue-next'
import http from './api'
import { connectAuthSocket, disconnectAuthSocket } from './authSocket'
import AiFloatingAssistant from './components/AiFloatingAssistant.vue'

const route = useRoute()
const router = useRouter()
const theme = ref(localStorage.getItem('themeMode') || 'dark')
const user = computed(() => {
  route.fullPath
  return JSON.parse(localStorage.getItem('user') || 'null')
})
const isAuthPage = computed(() => route.path === '/login')
const showAiAssistant = computed(() => Boolean(user.value) && !isAuthPage.value)
const themeIcon = computed(() => theme.value === 'dark' ? Sun : Moon)
const themeLabel = computed(() => theme.value === 'dark' ? '亮色' : '暗色')

onMounted(() => {
  applyTheme(theme.value)
  if (route.path !== '/login') {
    connectAuthSocket()
  }
})
onBeforeUnmount(disconnectAuthSocket)
watch(theme, applyTheme)
watch(() => route.fullPath, () => {
  if (localStorage.getItem('token') && route.path !== '/login') {
    connectAuthSocket()
  }
  if (!localStorage.getItem('token') || route.path === '/login') {
    disconnectAuthSocket()
  }
})

function applyTheme(value) {
  document.body.classList.toggle('theme-light', value === 'light')
  document.body.classList.toggle('theme-dark', value !== 'light')
  localStorage.setItem('themeMode', value)
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
}

async function logout() {
  try {
    await http.post('/auth/logout', null, { silent: true })
  } catch (error) {
    // Local cleanup still happens if the server session is already gone.
  }
  disconnectAuthSocket()
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>
