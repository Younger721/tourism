import { clearAuthAndRedirect } from './api'

const LOGIN_EXPIRED_MESSAGE = '\u767b\u5f55\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55'

let socket = null
let closingByClient = false
let handlingKickout = false

function authSocketUrl(token) {
  const base = import.meta.env.VITE_AUTH_WS_BASE || 'ws://localhost:8080/ws/auth'
  return `${base}?token=${encodeURIComponent(token)}`
}

export function connectAuthSocket() {
  const token = localStorage.getItem('token')
  if (!token || socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) {
    return
  }

  closingByClient = false
  handlingKickout = false
  const currentSocket = new WebSocket(authSocketUrl(token))
  socket = currentSocket

  currentSocket.onmessage = (event) => {
    if (socket !== currentSocket) {
      return
    }
    let message
    try {
      message = JSON.parse(event.data)
    } catch (error) {
      return
    }
    if (message.type === 'KICKOUT') {
      handlingKickout = true
      disconnectAuthSocket()
      clearAuthAndRedirect(message.message || LOGIN_EXPIRED_MESSAGE)
    }
  }

  currentSocket.onclose = (event) => {
    if (socket !== currentSocket) {
      return
    }
    const shouldRedirect = !closingByClient && !handlingKickout && localStorage.getItem('token') &&
      (event.code === 1003 || event.code === 1008 || Boolean(event.reason))
    socket = null
    closingByClient = false
    handlingKickout = false
    if (shouldRedirect) {
      clearAuthAndRedirect(event.reason || LOGIN_EXPIRED_MESSAGE)
    }
  }

  currentSocket.onerror = () => {
    // The close event decides whether this is auth invalidation or a transient network failure.
  }
}

export function disconnectAuthSocket() {
  closingByClient = true
  if (socket) {
    socket.close()
    socket = null
  }
}
