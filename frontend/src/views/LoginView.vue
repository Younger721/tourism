<template>
  <div class="page login-page">
    <el-card class="login-card">
      <h2>{{ isRegister ? '创建账号' : '欢迎登录' }}</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <template v-if="isRegister">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" />
          </el-form-item>
        </template>
        <el-button type="primary" class="wide" :icon="LogIn" @click="submit">
          {{ isRegister ? '注册并登录' : '登录' }}
        </el-button>
        <el-button text class="wide" @click="isRegister = !isRegister">
          {{ isRegister ? '已有账号，去登录' : '没有账号，去注册' }}
        </el-button>
      </el-form>
      <p class="muted">管理员默认账号：admin / admin123</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LogIn } from 'lucide-vue-next'
import http from '../api'
import { connectAuthSocket } from '../authSocket'

const router = useRouter()
const isRegister = ref(false)
const form = reactive({ username: '', password: '', nickname: '', phone: '' })

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  const data = await http.post(isRegister.value ? '/auth/register' : '/auth/login', form)
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user))
  window.dispatchEvent(new Event('auth-state-changed'))
  connectAuthSocket()
  ElMessage.success('登录成功')
  router.push(data.user.role === 'ADMIN' ? '/admin' : '/home')
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: calc(100vh - 40px);
  place-items: center;
}

.login-card {
  width: min(420px, 100%);
  border-radius: 26px;
}

.wide {
  width: 100%;
  margin: 8px 0 0;
}
</style>
