const USER_INFO_KEY = 'userInfo'

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
  return user?.id ?? null
}

export function removeUserInfo() {
  localStorage.removeItem(USER_INFO_KEY)
}
