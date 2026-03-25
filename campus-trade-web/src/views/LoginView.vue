<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <CampusLogo :size="58" :title="'校园二手交易系统'" subtitle="欢迎登录" />
      </div>

      <LoginForm @success="handleLoginSuccess" @register="goRegister" />

      <div class="login-footer">
        <p>校园二手交易平台</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import CampusLogo from '../components/CampusLogo.vue'
import LoginForm from '../components/LoginForm.vue'

const router = useRouter()
const route = useRoute()

const goRegister = () => {
  router.push('/register')
}

const handleLoginSuccess = () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  router.push(redirect)
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #fffdf4 0%, #fff7dd 100%);
}

.login-box {
  width: 420px;
  padding: 40px;
  background: #fffaf0;
  border-radius: 12px;
  border: 1px solid #f5e6bf;
  box-shadow: 0 16px 36px rgba(180, 150, 78, 0.12);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header :deep(.campus-logo) {
  justify-content: center;
}

.login-header :deep(.campus-logo__title) {
  font-size: 26px;
}

.login-header :deep(.campus-logo__subtitle) {
  font-size: 14px;
  letter-spacing: 0.4px;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f1e4c0;
}

.login-footer p {
  color: #aa8c4f;
  font-size: 14px;
}
</style>
