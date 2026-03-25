<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1>校园二手交易系统</h1>
        <p>欢迎注册</p>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="80px"
        class="register-form"
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
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item label="头像 URL" prop="avatar">
          <el-input
            v-model="formData.avatar"
            placeholder="选填：请输入头像图片链接"
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="loading"
            class="register-button"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-link">
        <span>已有账号？</span>
        <el-button link type="primary" @click="goLogin">去登录</el-button>
      </div>

      <div class="register-footer">
        <p>校园二手交易平台</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { register } from '../api/user'

const formRef = ref(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  avatar: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== formData.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const triggerLoginDialog = (message) => {
  window.dispatchEvent(
    new CustomEvent('require-login', {
      detail: {
        message,
      },
    })
  )
}

const handleRegister = async () => {
  if (!formRef.value || loading.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await register({
      username: formData.username,
      password: formData.password,
      avatar: formData.avatar?.trim() || null
    })

    ElMessage.success(res?.message || '注册成功，请登录')
    triggerLoginDialog('')
  } catch (error) {
    const message =
      error?.message ||
      error?.data?.message ||
      error?.response?.data?.message ||
      '注册失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const goLogin = () => {
  triggerLoginDialog('')
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #fffdf4 0%, #fff7dd 100%);
}

.register-box {
  width: 420px;
  padding: 40px;
  background: #fffaf0;
  border-radius: 12px;
  border: 1px solid #f5e6bf;
  box-shadow: 0 16px 36px rgba(180, 150, 78, 0.12);
}

.register-header {
  text-align: center;
  margin-bottom: 40px;
}

.register-header h1 {
  font-size: 28px;
  color: #8a6a21;
  margin-bottom: 10px;
  font-weight: 600;
}

.register-header p {
  font-size: 16px;
  color: #b19459;
}

.register-form {
  margin-bottom: 12px;
}

.register-button {
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

.register-footer {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f1e4c0;
}

.register-footer p {
  color: #aa8c4f;
  font-size: 14px;
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
