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
          <span class="username">你好，{{ displayName }}</span>
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
  background: #f5f7fa;
}

.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.brand {
  flex-shrink: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  cursor: pointer;
}

.nav-menu {
  flex: 1;
  margin: 0 24px;
  border-bottom: none;
  min-width: 0;
}

.user-actions {
  min-width: 160px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.username {
  color: #606266;
  font-size: 14px;
}

.page-content {
  padding: 20px;
}

@media (max-width: 900px) {
  .top-nav {
    padding: 0 12px;
  }

  .brand {
    font-size: 15px;
  }

  .nav-menu {
    margin: 0 8px;
  }

  .user-actions {
    min-width: 90px;
  }

  .username {
    display: none;
  }
}
</style>
