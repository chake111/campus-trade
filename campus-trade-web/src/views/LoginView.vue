<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>校园二手交易系统</h1>
        <p>欢迎登录</p>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="80px"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="loading"
            class="login-button"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>校园二手交易平台</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/user'
import { removeToken, setToken } from '../utils/request'
import { dispatchAuthChanged, setUserInfo } from '../utils/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

function pickToken(payload) {
  return payload?.token || payload?.accessToken || payload?.access_token || null
}

function normalizeUser(rawUser) {
  if (!rawUser || typeof rawUser !== 'object' || Array.isArray(rawUser)) return null

  const normalized = { ...rawUser }
  if (normalized.id == null && normalized.userId != null) {
    normalized.id = normalized.userId
  }
  if (normalized.id == null && normalized.uid != null) {
    normalized.id = normalized.uid
  }
  return normalized
}

function pickUser(payload) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return null

  const preferredKeys = ['user', 'userInfo', 'currentUser', 'profile', 'account']
  for (const key of preferredKeys) {
    const maybeUser = normalizeUser(payload[key])
    if (maybeUser) return maybeUser
  }

  const maybeDirectUser = normalizeUser(payload)
  if (maybeDirectUser && (maybeDirectUser.id != null || maybeDirectUser.username || maybeDirectUser.nickName)) {
    return maybeDirectUser
  }

  return null
}

const handleLogin = async () => {
  if (!formRef.value || loading.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(formData)

    const rootPayload = res && typeof res === 'object' ? res : {}
    const dataPayload = rootPayload?.data && typeof rootPayload.data === 'object' ? rootPayload.data : null

    const token = pickToken(dataPayload) || pickToken(rootPayload)
    if (!token) {
      ElMessage.error(rootPayload?.message || dataPayload?.message || '登录失败：未获取到 token')
      return
    }

    setToken(token)

    const user = pickUser(dataPayload) || pickUser(rootPayload)
    if (!user || user.id == null) {
      removeToken()
      ElMessage.error('登录返回缺少用户信息，请联系后端确认登录返回字段')
      return
    }

    setUserInfo(user)
    dispatchAuthChanged()
    ElMessage.success(rootPayload?.message || '登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
    const message =
      error?.data?.message ||
      error?.response?.data?.message ||
      error?.message ||
      '登录失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 10px;
  font-weight: 600;
}

.login-header p {
  font-size: 16px;
  color: #909399;
}

.login-form {
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  font-size: 16px;
  font-weight: 500;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.login-footer p {
  color: #909399;
  font-size: 14px;
}

.login-footer a {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

.login-footer a:hover {
  text-decoration: underline;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button--primary) {
  border-radius: 8px;
}
</style>
