import axios from 'axios'
import { ElMessage } from 'element-plus'

const LOGIN_EXPIRED_MESSAGE = '\u767b\u5f55\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55'
const REQUEST_FAILED_MESSAGE = '\u8bf7\u6c42\u5931\u8d25'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:8080/api',
  timeout: 30000
})

export function clearAuthAndRedirect(message = LOGIN_EXPIRED_MESSAGE) {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (!window.__authExpiredNotified) {
    window.__authExpiredNotified = true
    ElMessage.error(message || LOGIN_EXPIRED_MESSAGE)
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
      ElMessage.error(body.message || REQUEST_FAILED_MESSAGE)
    }
    return Promise.reject(new Error(body.message || REQUEST_FAILED_MESSAGE))
  }
  return body?.data ?? body
}, (error) => {
  const status = error.response?.status
  const body = error.response?.data
  if (status === 401) {
    clearAuthAndRedirect(body?.message)
    return Promise.reject(new Error(body?.message || LOGIN_EXPIRED_MESSAGE))
  }
  if (!error.config?.silent) {
    ElMessage.error(body?.message || error.message || REQUEST_FAILED_MESSAGE)
  }
  return Promise.reject(error)
})

export default http
