import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { dispatchAuthChanged, removeUserInfo } from './user'

const request = axios.create({
  baseURL: 'http://localhost:8080',
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

function handleAuthExpired(message) {
  ElMessage.error(message || '登录已过期，请重新登录')
  clearAuthState()
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

function isAuthFailure(status, payload = {}) {
  const code = Number(payload?.code)
  const rawMessage = payload?.message || payload?.msg || payload?.error || ''
  const message = String(rawMessage).toLowerCase()

  const hasAuthKeyword =
    message.includes('token') ||
    message.includes('登录') ||
    message.includes('认证') ||
    message.includes('unauthorized') ||
    message.includes('forbidden') ||
    message.includes('expired') ||
    message.includes('invalid') ||
    message.includes('未登录') ||
    message.includes('请先登录')

  if (status === 401 || code === 401) return true

  // 某些后端会把“认证失效”包装成 403，这里根据 code/message 再判定
  if ((status === 403 || code === 403) && hasAuthKeyword) return true

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

// 响应拦截器：兼容多种响应格式
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 情况 1：如果是数组，直接返回（兼容直接返回数组的接口）
    if (Array.isArray(res)) {
      return { data: res, code: 200, message: 'success' }
    }

    // 情况 2：如果是普通对象，检查是否有 code 字段
    if (res && typeof res === 'object') {
      // 有 code 字段，说明是标准格式 { code, message, data }
      if ('code' in res) {
        if (res.code === 200) {
          return res
        }

        if (isAuthFailure(response.status, res)) {
          handleAuthExpired(getStatusMessage(401, res.message))
          return Promise.reject(res)
        }

        ElMessage.error(getStatusMessage(res.code, res.message || '服务异常'))
        return Promise.reject(res)
      }
      // 没有 code 字段，说明是直接返回的对象，包装后返回
      return { data: res, code: 200, message: 'success' }
    }

    // 情况 3：其他类型，直接返回完整响应
    return response
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
