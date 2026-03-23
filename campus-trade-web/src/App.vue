<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, Plus, Search, Tickets, UserFilled, User, DataAnalysis } from '@element-plus/icons-vue'
import { getToken, removeToken } from './utils/request'
import { AUTH_CHANGED_EVENT, dispatchAuthChanged, getUserInfo, isAdmin, removeUserInfo } from './utils/user'
import CampusLogo from './components/CampusLogo.vue'

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
        <CampusLogo :size="34" compact />
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
  flex: 1 1 680px;
  min-width: 420px;
  display: block;
}

.nav-search-input {
  width: 100%;
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
