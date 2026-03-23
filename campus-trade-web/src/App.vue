<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
      <div class="brand" @click="goHome">校园二手交易系统</div>

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

      <div class="user-actions">
        <template v-if="isLoggedIn">
          <span class="username" :title="displayName">你好，{{ displayName }}</span>
          <el-button link type="danger" @click="handleLogout">退出登录</el-button>
        </template>
      </div>
    </el-header>

    <el-main class="page-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--theme-page-bg);
}

.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  column-gap: 12px;
  row-gap: 8px;
  min-height: 56px;
  height: auto;
  padding: 8px 20px;
  background: linear-gradient(180deg, #fff9e8 0%, var(--theme-page-bg) 100%);
  border-bottom: 1px solid var(--theme-border);
}

.brand {
  flex-shrink: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--theme-text-primary);
  cursor: pointer;
}

.nav-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--theme-text-secondary);
  --el-menu-hover-text-color: var(--theme-text-primary);
  --el-menu-hover-bg-color: var(--theme-primary-soft-1);
  --el-menu-active-color: var(--theme-text-primary);
  --el-menu-item-height: 46px;
  --el-menu-horizontal-height: 46px;
  flex: 1 1 520px;
  margin: 0 4px;
  border-bottom: none;
  min-width: 280px;
}

.nav-menu :deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
  padding: 0 14px;
  font-size: 14px;
  border-radius: 8px;
  margin: 0 2px;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: var(--theme-primary-soft-1);
  color: var(--theme-text-primary);
  font-weight: 600;
}

.nav-menu :deep(.el-menu--horizontal > .el-menu-item.is-active) {
  border-bottom: none;
}

.user-actions {
  flex: 0 0 auto;
  min-width: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--theme-primary-soft-2);
  border: 1px solid var(--theme-primary-soft-1);
  white-space: nowrap;
}

.username {
  max-width: 160px;
  color: var(--theme-text-secondary);
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

.page-content {
  width: min(100%, 1560px);
  margin: 0 auto;
  padding: 28px 28px 34px;
  box-sizing: border-box;
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

  .user-actions {
    margin-left: auto;
    min-width: 0;
    padding: 0;
    border: none;
    background: transparent;
  }

  .username {
    display: none;
  }

  .page-content {
    width: 100%;
    padding: 18px 14px 22px;
  }
}
</style>
