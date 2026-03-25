<template>
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

  <div class="register-link">
    <span>没有账号？</span>
    <el-button link type="primary" @click="emit('register')">立即注册</el-button>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { login } from '../api/user'
import { removeToken, setToken } from '../utils/request'
import { dispatchAuthChanged, normalizeUserInfo, setUserInfo } from '../utils/user'

const emit = defineEmits(['success', 'register'])

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

function pickUser(payload) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return null

  const preferredKeys = ['user', 'userInfo', 'currentUser', 'profile', 'account']
  for (const key of preferredKeys) {
    const maybeUser = normalizeUserInfo(payload[key])
    if (maybeUser) return maybeUser
  }

  const maybeDirectUser = normalizeUserInfo(payload)
  if (maybeDirectUser && (maybeDirectUser.id != null || maybeDirectUser.username)) {
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
      console.error('登录返回缺少 token:', { rootPayload, dataPayload })
      ElMessage.error('登录失败，请检查用户名和密码')
      return
    }

    setToken(token)

    const user = pickUser(dataPayload) || pickUser(rootPayload)
    if (!user || user.id == null) {
      removeToken()
      console.error('登录返回缺少用户信息:', { rootPayload, dataPayload })
      ElMessage.error('登录失败，请检查用户名和密码')
      return
    }

    setUserInfo(user)
    dispatchAuthChanged()
    ElMessage.success(rootPayload?.message || '登录成功')
    emit('success', { user, payload: rootPayload })
  } catch (error) {
    console.error('登录失败:', error)
    const message =
      error?.data?.message || error?.response?.data?.message || error?.message
    console.error('登录失败详情:', message)
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-form {
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  font-size: 16px;
  font-weight: 500;
}

.register-link {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  color: #aa8c4f;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button--primary) {
  border-radius: 8px;
  border-color: #ffd56d;
  background: linear-gradient(135deg, #ffe49c 0%, #ffd875 100%);
  color: #5a4208;
}

:deep(.el-button--primary:hover) {
  border-color: #ffca33;
  background: linear-gradient(135deg, #ffde8d 0%, #ffd15c 100%);
}
</style>
