const USER_INFO_KEY = 'userInfo'
export const AUTH_CHANGED_EVENT = 'auth-changed'
export const ROLE_USER = 'USER'
export const ROLE_ADMIN = 'ADMIN'

export function hasValidAuthState(token, userInfo) {
  const userId = userInfo?.id
  return Boolean(token && userInfo && userId)
}

export function dispatchAuthChanged() {
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT))
}

export function setUserInfo(user) {
  const normalized = normalizeUserInfo(user)
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(normalized || {}))
}

export function getUserInfo() {
  const raw = localStorage.getItem(USER_INFO_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

export function getUserId() {
  const user = getUserInfo()
  return user?.id ?? null
}

export function normalizeUserInfo(rawUser) {
  if (!rawUser || typeof rawUser !== 'object' || Array.isArray(rawUser)) return null

  const id = rawUser.id ?? rawUser.userId ?? rawUser.uid ?? null
  const rawRole = rawUser.role ?? rawUser.userRole ?? rawUser.type ?? rawUser.roles
  const role = normalizeRole(rawRole)

  return {
    id,
    username: rawUser.username || rawUser.name || rawUser.nickname || '',
    avatar: normalizeAvatar(rawUser),
    role,
    creditScore: rawUser.creditScore ?? null,
    createTime: rawUser.createTime ?? null,
    status: rawUser.status ?? null,
  }
}

function normalizeAvatar(rawUser) {
  const avatar = rawUser.avatar || rawUser.avatarUrl || rawUser.headImg || rawUser.photo || ''
  return String(avatar).trim()
}

export function normalizeRole(rawRole) {
  if (Array.isArray(rawRole)) {
    return normalizeRole(rawRole[0])
  }
  const role = String(rawRole || ROLE_USER).toUpperCase()
  return role === ROLE_ADMIN ? ROLE_ADMIN : ROLE_USER
}

export function hasRole(user, allowedRoles = []) {
  if (!user || !allowedRoles.length) return false
  const role = normalizeRole(user.role)
  return allowedRoles.map((item) => normalizeRole(item)).includes(role)
}

export function isAdmin(user) {
  return hasRole(user, [ROLE_ADMIN])
}

export function removeUserInfo() {
  localStorage.removeItem(USER_INFO_KEY)
}
