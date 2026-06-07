import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:8080/api',
  timeout: 30000
})

function clearAuthAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (!window.__authExpiredNotified) {
    window.__authExpiredNotified = true
    ElMessage.error('登录已失效，请重新登录')
    window.setTimeout(() => {
      window.__authExpiredNotified = false
    }, 1200)
  }
  if (window.location.pathname !== '/login') {
    window.location.assign('/login')
  }
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use((response) => {
  const body = response.data
  if (body && body.success === false) {
    if (!response.config.silent) {
      ElMessage.error(body.message || '请求失败')
    }
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return body?.data ?? body
}, (error) => {
  const status = error.response?.status
  const body = error.response?.data
  if (status === 401) {
    clearAuthAndRedirect()
    return Promise.reject(new Error(body?.message || '登录已失效'))
  }
  if (!error.config?.silent) {
    ElMessage.error(body?.message || error.message || '请求失败')
  }
  return Promise.reject(error)
})

export default http
