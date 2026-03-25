import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { dispatchAuthChanged, removeUserInfo } from './user'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8',
  },
})

// Token helpers (导出供路由守卫等使用)
export function setToken(token) {
  if (token == null) return
  localStorage.setItem('token', token)
}
export function getToken() {
  return localStorage.getItem('token')
}
export function removeToken() {
  localStorage.removeItem('token')
}

function clearAuthState() {
  removeToken()
  removeUserInfo()
  dispatchAuthChanged()
}

function sanitizeRedirectPath(path) {
  if (typeof path !== 'string') return ''
  if (!path.startsWith('/')) return ''
  if (path.startsWith('//')) return ''
  return path
}

function handleAuthExpired(message) {
  ElMessage.error(message || '登录已过期，请重新登录')
  const currentPath = sanitizeRedirectPath(router.currentRoute.value.fullPath)
  clearAuthState()
  const query = { login: '1' }
  if (currentPath && currentPath !== '/products') {
    query.loginRedirect = currentPath
  }
  router.push({ path: '/products', query })
}

function isAuthFailure(status, payload = {}) {
  const code = Number(payload?.code)
  if (status === 401 || code === 401) return true

  return false
}

// 简单请求方法封装，方便 API 调用以统一用法
export function get(url, params) {
  return request.get(url, { params })
}

export function post(url, data) {
  return request.post(url, data)
}

export function put(url, data) {
  return request.put(url, data)
}

export function del(url, params) {
  return request.delete(url, { params })
}

const normalizeAuthorizationToken = (token) => {
  if (!token) return ''
  return /^Bearer\s+/i.test(token) ? token : `Bearer ${token}`
}

// 请求拦截器：从 localStorage 读取 token，放入请求头
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = normalizeAuthorizationToken(token)
      // 兼容仍读取自定义 token 头的后端
      config.headers.token = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

const STATUS_MESSAGE_MAP = {
  401: '登录已过期，请重新登录',
  403: '没有访问权限',
  404: '请求资源不存在',
  500: '服务器内部错误',
}

const getStatusMessage = (status, fallbackMessage = '服务异常') => {
  return STATUS_MESSAGE_MAP[status] || fallbackMessage
}

const wrapSuccessResponse = (payload, status = 200) => {
  if (payload && typeof payload === 'object' && !Array.isArray(payload) && 'code' in payload) {
    return payload
  }

  return {
    code: status,
    message: 'success',
    data: payload,
  }
}

// 响应拦截器：统一成功返回结构为 { code, message, data }
request.interceptors.response.use(
  (response) => {
    const res = response.data
    const wrapped = wrapSuccessResponse(res, response.status)

    if (wrapped.code === 200) {
      return wrapped
    }

    if (isAuthFailure(response.status, wrapped)) {
      handleAuthExpired(getStatusMessage(401, wrapped.message))
      return Promise.reject(wrapped)
    }

    ElMessage.error(getStatusMessage(wrapped.code, wrapped.message || '服务异常'))
    return Promise.reject(wrapped)
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const respData = error.response.data || {}

      if (isAuthFailure(status, respData)) {
        handleAuthExpired(getStatusMessage(401, respData.message))
        return Promise.reject(error.response)
      }

      const message = getStatusMessage(status, respData.message || '服务异常')
      ElMessage.error(message)
      return Promise.reject(error.response)
    }

    ElMessage.error('网络异常，请检查后端服务是否启动')
    return Promise.reject(error)
  }
)

export default request
