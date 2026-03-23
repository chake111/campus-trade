<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, Tickets, ChatDotRound, UserFilled } from '@element-plus/icons-vue'
import { getToken, removeToken } from './utils/request'
import { AUTH_CHANGED_EVENT, dispatchAuthChanged, getUserInfo, isAdmin, removeUserInfo } from './utils/user'

const router = useRouter()
const route = useRoute()

const token = ref(getToken())
const userInfo = ref(getUserInfo())

function syncAuthState() {
  token.value = getToken()
  userInfo.value = getUserInfo()
}

const isLoggedIn = computed(() => {
  const currentUser = userInfo.value
  const userId = currentUser?.id
  return Boolean(token.value && currentUser && userId)
})

const displayName = computed(() => {
  if (!userInfo.value) return '同学'
  return userInfo.value.username || `用户${userInfo.value.id || ''}`
})

const canAccessDashboard = computed(() => isAdmin(userInfo.value))

const showFloatingCapsule = computed(() => {
  return !route.path.startsWith('/login') && !route.path.startsWith('/register')
})

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/product/create')) return '/product/create'
  if (path.startsWith('/orders')) return '/orders'
  if (path.startsWith('/profile')) return '/profile'
  if (path.startsWith('/dashboard')) return '/dashboard'
  if (path.startsWith('/login')) return '/login'
  return '/products'
})

function goHome() {
  router.push('/products')
}

function goPlatformSearch() {
  router.push('/products')
}

function goOrders() {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录后查看订单')
    router.push('/login')
    return
  }
  router.push('/orders')
}

function goPublish() {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录后发布闲置')
    router.push('/login')
    return
  }
  router.push('/product/create')
}

function goHelp() {
  ElMessage.info('可在个人中心查看账号信息并提交反馈')
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }
  router.push('/profile')
}

function handleLogout() {
  removeToken()
  removeUserInfo()
  syncAuthState()
  dispatchAuthChanged()
  ElMessage.success('已退出登录')
  router.push('/products')
}

onMounted(() => {
  syncAuthState()
  window.addEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})

onUnmounted(() => {
  window.removeEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})
</script>

<template>
  <el-container class="app-shell">
    <el-header class="top-nav">
      <div class="brand-wrap" @click="goHome">
        <div class="brand">校园二手交易系统</div>
        <span class="brand-subtitle">CAMPUS MARKET</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        :ellipsis="false"
        router
        class="nav-menu"
      >
        <el-menu-item index="/products">首页</el-menu-item>
        <el-menu-item v-if="canAccessDashboard" index="/dashboard">数据看板</el-menu-item>

        <template v-if="isLoggedIn">
          <el-menu-item index="/product/create">发布商品</el-menu-item>
          <el-menu-item index="/orders">我的订单</el-menu-item>
          <el-menu-item index="/profile">个人中心</el-menu-item>
        </template>

        <template v-else>
          <el-menu-item index="/login">登录</el-menu-item>
        </template>
      </el-menu>

      <button class="platform-search-entry" type="button" @click="goPlatformSearch">
        <el-icon><Search /></el-icon>
        <span>平台搜索</span>
      </button>

      <div v-if="isLoggedIn" class="user-actions">
        <div class="user-avatar"><el-icon><UserFilled /></el-icon></div>
        <div class="user-brief">
          <span class="user-label">个人中心</span>
          <span class="username" :title="displayName">{{ displayName }}</span>
        </div>
        <el-button link type="danger" @click="handleLogout">退出</el-button>
      </div>
      <div v-else class="user-actions user-actions--guest">
        <el-button class="guest-login-btn" @click="router.push('/login')">登录 / 注册</el-button>
      </div>
    </el-header>

    <el-main class="page-content">
      <router-view />
    </el-main>

    <div v-if="showFloatingCapsule" class="floating-capsule">
      <button class="capsule-item" type="button" @click="goPublish">
        <el-icon><Plus /></el-icon>
        <span>发布闲置</span>
      </button>
      <button class="capsule-item" type="button" @click="goOrders">
        <el-icon><Tickets /></el-icon>
        <span>我的订单</span>
      </button>
      <button class="capsule-item" type="button" @click="goHelp">
        <el-icon><ChatDotRound /></el-icon>
        <span>帮助反馈</span>
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
  justify-content: space-between;
  flex-wrap: wrap;
  column-gap: 12px;
  row-gap: 10px;
  min-height: 62px;
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

.brand {
  font-size: 17px;
  font-weight: 700;
  color: var(--theme-text-primary);
  line-height: 1.1;
}

.brand-subtitle {
  font-size: 11px;
  letter-spacing: 1.1px;
  color: #a07b31;
}

.nav-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--theme-text-secondary);
  --el-menu-hover-text-color: var(--theme-text-primary);
  --el-menu-hover-bg-color: var(--theme-primary-soft-1);
  --el-menu-active-color: var(--theme-text-primary);
  --el-menu-item-height: 44px;
  --el-menu-horizontal-height: 44px;
  flex: 1 1 460px;
  margin: 0 2px;
  border-bottom: none;
  min-width: 280px;
}

.nav-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  padding: 0 14px;
  font-size: 14px;
  border-radius: 10px;
  margin: 0 2px;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: #ffe8b4;
  color: #7d5317;
  font-weight: 600;
}

.nav-menu :deep(.el-menu--horizontal > .el-menu-item.is-active) {
  border-bottom: none;
}

.platform-search-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid #f1d8a0;
  background: #fff8e6;
  color: #876128;
  font-size: 13px;
  cursor: pointer;
}

.platform-search-entry:hover {
  background: #ffefc8;
}

.user-actions {
  flex: 0 0 auto;
  min-width: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 8px;
  border-radius: 999px;
  background: #fff5dd;
  border: 1px solid #efd7a6;
  white-space: nowrap;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #99671d;
  background: #ffeab9;
}

.user-brief {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.1;
  gap: 1px;
}

.user-label {
  font-size: 11px;
  color: #a08247;
}

.username {
  max-width: 116px;
  color: #5a4b2a;
  font-size: 13px;
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

.floating-capsule {
  position: fixed;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 25;
  width: 130px;
  padding: 8px;
  border-radius: 20px;
  border: 1px solid #efd6a3;
  background: rgba(255, 249, 234, 0.92);
  box-shadow: 0 10px 24px rgba(126, 92, 16, 0.12);
  backdrop-filter: blur(3px);
}

.capsule-item {
  width: 100%;
  height: 40px;
  margin: 3px 0;
  border: none;
  border-radius: 14px;
  background: transparent;
  color: #855f24;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 0 10px;
  font-size: 13px;
  cursor: pointer;
}

.capsule-item:hover {
  background: #ffefc7;
}

@media (max-width: 1100px) {
  .platform-search-entry {
    order: 4;
    margin-left: auto;
  }

  .floating-capsule {
    right: 12px;
    width: 120px;
  }
}

@media (max-width: 980px) {
  .top-nav {
    padding: 8px 12px;
  }

  .brand {
    font-size: 16px;
  }

  .nav-menu {
    order: 3;
    flex: 1 1 100%;
    min-width: 0;
    margin: 0;
  }

  .platform-search-entry {
    order: 5;
    margin-left: 0;
  }

  .user-actions {
    margin-left: auto;
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
