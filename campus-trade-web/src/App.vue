<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './utils/request'
import { getUserInfo, removeUserInfo } from './utils/user'

const router = useRouter()
const route = useRoute()

const token = ref(getToken())
const userInfo = ref(getUserInfo())

function syncAuthState() {
  token.value = getToken()
  userInfo.value = getUserInfo()
}

watch(
  () => route.fullPath,
  () => {
    syncAuthState()
  },
  { immediate: true }
)

const isLoggedIn = computed(() => Boolean(token.value && userInfo.value))

const displayName = computed(() => {
  if (!userInfo.value) return '同学'
  return userInfo.value.username || userInfo.value.nickname || userInfo.value.name || `用户${userInfo.value.id || ''}`
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

function handleLogout() {
  removeToken()
  removeUserInfo()
  ElMessage.success('已退出登录')
  router.push('/products')
}
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
        <el-menu-item index="/dashboard">数据看板</el-menu-item>

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
  background: #f7f7f8;
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
  background: linear-gradient(180deg, #fff9e8 0%, #fffdf5 100%);
  border-bottom: 1px solid #efdca7;
}

.brand {
  flex-shrink: 0;
  font-size: 17px;
  font-weight: 700;
  color: #3c3c3c;
  cursor: pointer;
}

.nav-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #595959;
  --el-menu-hover-text-color: #3c3c3c;
  --el-menu-hover-bg-color: #ffefba;
  --el-menu-active-color: #3c3c3c;
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
  background: #ffe080;
  color: #43320f;
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
  background: #fff2c8;
  border: 1px solid #ebcb6f;
  white-space: nowrap;
}

.username {
  max-width: 160px;
  color: #5a4116;
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
  padding: 18px;
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
}
</style>
