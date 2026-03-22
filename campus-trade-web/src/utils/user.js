const USER_INFO_KEY = 'userInfo'
export const AUTH_CHANGED_EVENT = 'auth-changed'

export function dispatchAuthChanged() {
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT))
}

export function setUserInfo(user) {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(user || {}))
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
  return user?.id ?? user?.userId ?? user?.uid ?? null
}

export function removeUserInfo() {
  localStorage.removeItem(USER_INFO_KEY)
}
