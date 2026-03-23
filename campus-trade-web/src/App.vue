<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, Tickets, UserFilled, User, DataAnalysis } from '@element-plus/icons-vue'
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
const navSearchKeyword = ref('')

const showFloatingCapsule = computed(() => {
  return !route.path.startsWith('/login') && !route.path.startsWith('/register')
})

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/login')) return '/login'
  return '/products'
})

function goHome() {
  router.push('/products')
}

function goPlatformSearch() {
  const keyword = navSearchKeyword.value.trim()
  router.push({ path: '/products', query: keyword ? { keyword } : {} })
}

const showNavSearch = computed(() => {
  return !route.path.startsWith('/login') && !route.path.startsWith('/register')
})

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
    ElMessage.info('请先登录后发布商品')
    router.push('/login')
    return
  }
  router.push('/product/create')
}

function goProfile() {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录后查看个人中心')
    router.push('/login')
    return
  }
  router.push('/profile')
}

function goDashboard() {
  if (!canAccessDashboard.value) return
  router.push('/dashboard')
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
        <div class="brand">校园二手交易系统</div>
        <span class="brand-subtitle">CAMPUS MARKET</span>
      </div>

      <div class="nav-capsule-group">
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          :ellipsis="false"
          router
          class="nav-menu"
        >
          <el-menu-item index="/products">首页</el-menu-item>
        </el-menu>

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

      <div v-if="isLoggedIn" class="user-actions">
        <div class="user-avatar"><el-icon><UserFilled /></el-icon></div>
        <span class="username" :title="displayName">{{ displayName }}</span>
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
        <span>发布商品</span>
      </button>
      <button class="capsule-item" type="button" @click="goOrders">
        <el-icon><Tickets /></el-icon>
        <span>我的订单</span>
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

.nav-capsule-group {
  flex: 1 1 680px;
  min-width: 420px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px;
  border-radius: 999px;
  border: 1px solid #efd9aa;
  background: rgba(255, 248, 230, 0.9);
}

.nav-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--theme-text-secondary);
  --el-menu-hover-text-color: var(--theme-text-primary);
  --el-menu-hover-bg-color: rgba(255, 230, 172, 0.55);
  --el-menu-active-color: var(--theme-text-primary);
  --el-menu-item-height: 38px;
  --el-menu-horizontal-height: 38px;
  flex: 0 0 auto;
  margin: 0;
  border-bottom: none;
  min-width: 0;
}

.nav-menu :deep(.el-menu-item) {
  height: 38px;
  line-height: 38px;
  padding: 0 12px;
  font-size: 13px;
  border-radius: 999px;
  margin: 0 2px;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: #ffe6b1;
  color: #6f4a11;
  font-weight: 600;
}

.nav-menu :deep(.el-menu--horizontal > .el-menu-item.is-active) {
  border-bottom: none;
}

.nav-search-input {
  flex: 1.2 1 360px;
  min-width: 250px;
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
  background: rgba(255, 245, 221, 0.66);
  border: 1px solid #efdbb1;
  white-space: nowrap;
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #99671d;
  background: #ffe8b7;
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

.floating-capsule {
  position: fixed;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 25;
  width: 124px;
  padding: 5px;
  border-radius: 20px;
  border: 1px solid #efdfba;
  background: rgba(255, 250, 238, 0.88);
  box-shadow: 0 8px 18px rgba(126, 92, 16, 0.08);
  backdrop-filter: blur(3px);
}

.capsule-item {
  width: 100%;
  height: 34px;
  margin: 2px 0;
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

.capsule-item:hover {
  background: #fff1d0;
}

@media (max-width: 1100px) {
  .nav-capsule-group { min-width: 0; }

  .floating-capsule {
    right: 12px;
    width: 122px;
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
    flex: 0 0 auto;
  }

  .user-actions {
    min-width: 0;
  }

  .nav-capsule-group {
    order: 3;
    flex: 1 1 100%;
    border-radius: 18px;
    flex-wrap: wrap;
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
