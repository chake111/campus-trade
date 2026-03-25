<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, Plus, Search, Tickets, User, DataAnalysis, ChatDotRound } from '@element-plus/icons-vue'
import { getToken, removeToken } from './utils/request'
import { AUTH_CHANGED_EVENT, dispatchAuthChanged, getUserInfo, hasValidAuthState, isAdmin, removeUserInfo } from './utils/user'
import { getConsultUnreadCount } from './api/consult'
import CampusLogo from './components/CampusLogo.vue'
import LoginForm from './components/LoginForm.vue'
import defaultAvatar from './assets/default-avatar.svg'

const router = useRouter()
const route = useRoute()

const token = ref(getToken())
const userInfo = ref(getUserInfo())

function syncAuthState() {
  token.value = getToken()
  userInfo.value = getUserInfo()
}

const isLoggedIn = computed(() => hasValidAuthState(token.value, userInfo.value))

const displayName = computed(() => {
  if (!userInfo.value) return '同学'
  return userInfo.value.username || `用户${userInfo.value.id || ''}`
})

const canAccessDashboard = computed(() => isAdmin(userInfo.value))
const navAvatar = computed(() => {
  const avatar = userInfo.value?.avatar
  return avatar || defaultAvatar
})
const navSearchKeyword = ref('')
const consultUnreadCount = ref(0)
const consultUnreadLoading = ref(false)
let unreadPollingTimer = null
const loginDialogVisible = ref(false)
const pendingAction = ref(null)
const loginFormRef = ref(null)

const showFloatingCapsule = computed(() => {
  return !route.path.startsWith('/register')
})

function goHome() {
  router.push('/products')
}

function goPlatformSearch() {
  const keyword = navSearchKeyword.value.trim()
  router.push({ path: '/products', query: keyword ? { keyword } : {} })
}

const showNavSearch = computed(() => {
  return !route.path.startsWith('/register')
})

function sanitizeRedirectPath(value) {
  if (typeof value !== 'string') return ''
  if (!value.startsWith('/')) return ''
  if (value.startsWith('//')) return ''
  return value
}

function clearLoginQueryTrigger() {
  const query = { ...route.query }
  if (!('login' in query) && !('loginRedirect' in query)) return
  delete query.login
  delete query.loginRedirect
  router.replace({ path: route.path, query })
}

function openLoginDialog() {
  pendingAction.value = null
  loginDialogVisible.value = true
}

function runPendingAction() {
  const action = pendingAction.value
  pendingAction.value = null
  if (typeof action === 'function') {
    action()
  }
}

function requireLogin(action, message = '请先登录后再操作') {
  if (isLoggedIn.value) {
    if (typeof action === 'function') action()
    return
  }

  if (message) {
    ElMessage.info(message)
  }
  pendingAction.value = typeof action === 'function' ? action : null
  loginDialogVisible.value = true
}

function handleLoginSuccess() {
  loginDialogVisible.value = false
  syncAuthState()
  runPendingAction()
  clearLoginQueryTrigger()
}

function handleLoginDialogClose() {
  pendingAction.value = null
  loginFormRef.value?.reset?.()
  clearLoginQueryTrigger()
}

function handleLoginRegister() {
  loginDialogVisible.value = false
  pendingAction.value = null
  router.push('/register')
}

function handleExternalRequireLogin(event) {
  const detail = event?.detail || {}
  const redirectPath = sanitizeRedirectPath(detail.redirectPath)
  const action = typeof detail.action === 'function'
    ? detail.action
    : redirectPath
      ? () => router.push(redirectPath)
      : null
  requireLogin(action, detail.message)
}

function goOrders() {
  requireLogin(() => router.push('/orders'), '请先登录后再操作')
}

function goMessages() {
  requireLogin(() => router.push('/messages'), '请先登录后再操作')
}

function goPublish() {
  requireLogin(() => router.push('/product/create'), '请先登录后再操作')
}

function goProfile() {
  requireLogin(() => router.push('/profile'), '请先登录后再操作')
}

function goDashboard() {
  requireLogin(() => {
    if (!canAccessDashboard.value) return
    router.push('/dashboard')
  }, '请先登录后再操作')
}

function handleLogout() {
  removeToken()
  removeUserInfo()
  syncAuthState()
  dispatchAuthChanged()
  ElMessage.success('已退出登录')
  router.push('/products')
}

async function refreshConsultUnreadCount() {
  if (!isLoggedIn.value || consultUnreadLoading.value) {
    consultUnreadCount.value = 0
    return
  }
  consultUnreadLoading.value = true
  try {
    const res = await getConsultUnreadCount()
    consultUnreadCount.value = Number(res?.data || 0)
  } catch (error) {
    consultUnreadCount.value = 0
  } finally {
    consultUnreadLoading.value = false
  }
}

function startUnreadPolling() {
  stopUnreadPolling()
  if (!isLoggedIn.value) return
  unreadPollingTimer = window.setInterval(() => {
    refreshConsultUnreadCount()
  }, 5000)
}

function stopUnreadPolling() {
  if (!unreadPollingTimer) return
  window.clearInterval(unreadPollingTimer)
  unreadPollingTimer = null
}

onMounted(() => {
  syncAuthState()
  window.addEventListener(AUTH_CHANGED_EVENT, syncAuthState)
  window.addEventListener('require-login', handleExternalRequireLogin)
  refreshConsultUnreadCount()
  startUnreadPolling()
})

onUnmounted(() => {
  window.removeEventListener(AUTH_CHANGED_EVENT, syncAuthState)
  window.removeEventListener('require-login', handleExternalRequireLogin)
  stopUnreadPolling()
})

watch(isLoggedIn, () => {
  refreshConsultUnreadCount()
  startUnreadPolling()
})

watch(
  () => [route.query.login, route.query.loginRedirect, isLoggedIn.value],
  ([loginTrigger, loginRedirect]) => {
    if (!loginTrigger) return
    const redirectPath = sanitizeRedirectPath(loginRedirect)
    if (isLoggedIn.value) {
      clearLoginQueryTrigger()
      return
    }

    pendingAction.value = redirectPath ? () => router.push(redirectPath) : null
    loginDialogVisible.value = true
  },
  { immediate: true }
)

watch(
  () => route.path,
  () => {
    if (isLoggedIn.value) {
      refreshConsultUnreadCount()
    }
  }
)

watch(
  () => route.query.keyword,
  (keyword) => {
    navSearchKeyword.value = typeof keyword === 'string' ? keyword : ''
  },
  { immediate: true }
)
</script>

<template>
  <el-container class="app-shell">
    <el-header class="top-nav">
      <div class="brand-wrap" @click="goHome">
        <CampusLogo :size="60" compact />
      </div>

      <div class="nav-search-wrap">
        <el-input
          v-if="showNavSearch"
          v-model="navSearchKeyword"
          class="nav-search-input"
          placeholder="搜索教材、数码、日用..."
          clearable
          @keyup.enter="goPlatformSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="goPlatformSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <div v-if="isLoggedIn" class="user-actions"  @click="goProfile">
        <el-avatar class="user-avatar" :size="32" :src="navAvatar" />
        <span class="username" :title="displayName">{{ displayName }}</span>
        <el-button link type="danger" @click="handleLogout">退出</el-button>
      </div>
      <div v-else class="user-actions user-actions--guest">
        <el-button class="guest-login-btn" @click="openLoginDialog">登录 / 注册</el-button>
      </div>
    </el-header>

    <el-dialog
      v-model="loginDialogVisible"
      width="460px"
      class="login-dialog"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
      @close="handleLoginDialogClose"
      title="登录 / 注册"
    >
      <p class="login-dialog-subtitle">登录后可继续当前操作</p>
      <LoginForm ref="loginFormRef" @success="handleLoginSuccess" @register="handleLoginRegister" />
    </el-dialog>

    <el-main class="page-content">
      <router-view />
    </el-main>

    <div v-if="showFloatingCapsule" class="floating-capsule">
      <button class="capsule-item" type="button" @click="goHome">
        <el-icon><House /></el-icon>
        <span>首页</span>
      </button>
      <button class="capsule-item" type="button" @click="goPublish">
        <el-icon><Plus /></el-icon>
        <span>发布商品</span>
      </button>
      <button class="capsule-item" type="button" @click="goOrders">
        <el-icon><Tickets /></el-icon>
        <span>我的订单</span>
      </button>
      <button class="capsule-item capsule-item--with-badge" type="button" @click="goMessages">
        <span v-if="isLoggedIn && consultUnreadCount > 0" class="unread-badge">
          {{ consultUnreadCount > 99 ? '99+' : consultUnreadCount }}
        </span>
        <el-icon><ChatDotRound /></el-icon>
        <span>商品咨询</span>
      </button>
      <button class="capsule-item" type="button" @click="goProfile">
        <el-icon><User /></el-icon>
        <span>个人中心</span>
      </button>
      <button v-if="canAccessDashboard" class="capsule-item" type="button" @click="goDashboard">
        <el-icon><DataAnalysis /></el-icon>
        <span>数据看板</span>
      </button>
    </div>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--theme-page-bg);
  position: relative;
}

.top-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  column-gap: 12px;
  row-gap: 10px;
  min-height: 68px;
  height: auto;
  padding: 10px 20px;
  background: linear-gradient(180deg, #fff9e8 0%, #fffdf5 100%);
  border-bottom: 1px solid var(--theme-border);
  box-shadow: 0 4px 14px rgba(141, 108, 22, 0.07);
}

.brand-wrap {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
}

.nav-search-wrap {
  align-items: center;
  flex: 1 1 80px;
  min-width: 20px;
  display: block;
}

.nav-search-input {
  width: 50%;
  min-width: 0;
}

.nav-search-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border: 1px solid #efd7a3;
  border-radius: 999px 0 0 999px;
  background: #fffcf3;
}

.nav-search-input :deep(.el-input-group__append) {
  background: transparent;
  box-shadow: none;
  border: none;
}

.nav-search-input :deep(.el-input-group__append .el-button) {
  min-height: 38px;
  border-radius: 0 999px 999px 0;
  border: 1px solid #efcd86;
  background: #ffe8b6;
  color: #6f4a11;
}

.user-actions {
  margin-left: auto;
  flex: 0 0 auto;
  min-width: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  padding: 4px 8px 4px 6px;
  border-radius: 999px;
  white-space: nowrap;
}

.user-avatar {
  flex-shrink: 0;
  border: 1px solid #efcd86;
}

.username {
  max-width: 84px;
  color: #5a4b2a;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-actions :deep(.el-button.is-link) {
  padding: 2px 0;
  font-size: 13px;
  color: #d34a24;
}

.user-actions :deep(.el-button.is-link:hover) {
  color: #b53c1d;
}

.user-actions--guest {
  padding: 0;
  border: none;
  background: transparent;
}

.guest-login-btn {
  border-radius: 999px;
  border-color: #efcd89;
  color: #7b5317;
  background: #fff3d0;
}

.page-content {
  width: min(100%, 1560px);
  margin: 0 auto;
  padding: 28px 28px 34px;
  box-sizing: border-box;
}

:deep(.login-dialog .el-dialog__body) {
  padding-top: 14px;
}

.login-dialog-subtitle {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.5;
  color: #a88f5a;
}

.floating-capsule {
  position: fixed;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 25;
  width: 120px;
  padding: 5px;
  border-radius: 20px;
  border: 1px solid #efdfba;
  background: rgba(255, 250, 238, 0.88);
  box-shadow: 0 8px 18px rgba(126, 92, 16, 0.08);
  backdrop-filter: blur(3px);
}

.capsule-item {
  width: 100%;
  height: 32px;
  margin: 1px 0;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #8b6a33;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 0 10px;
  font-size: 12px;
  cursor: pointer;
}

.capsule-item--with-badge {
  position: relative;
}

.unread-badge {
  position: absolute;
  top: 6px;
  right: 14px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  box-shadow: 0 0 0 2px #fff;
}

.capsule-item:hover {
  background: #fff1d0;
}

@media (max-width: 1100px) {
  .nav-search-wrap { min-width: 0; }

  .floating-capsule {
    right: 12px;
    width: 118px;
  }
}

@media (max-width: 980px) {
  .top-nav {
    padding: 8px 12px;
  }

  .brand {
    font-size: 16px;
  }

  .user-actions {
    min-width: 0;
  }

  .nav-search-wrap {
    order: 3;
    flex: 1 1 100%;
  }

  .nav-search-input {
    flex: 1 1 100%;
    min-width: 0;
  }

  .page-content {
    width: 100%;
    padding: 18px 14px 22px;
  }

  .floating-capsule {
    right: 10px;
    top: auto;
    bottom: 22px;
    transform: none;
    width: auto;
    display: flex;
    gap: 6px;
    border-radius: 999px;
    padding: 6px;
  }

  .capsule-item {
    width: auto;
    margin: 0;
    padding: 0 10px;
    justify-content: center;
  }

  .capsule-item span {
    display: none;
  }
}
</style>
