<template>
  <div class="personal-center">
    <el-card class="user-card">
      <template #header>
        <div class="card-header">
          <el-icon size="24"><User /></el-icon>
          <span>个人中心</span>
        </div>
      </template>

      <div v-loading="loading" class="user-info">
        <div class="credit-score-section">
          <div class="credit-score">
            <div class="score-value">{{ userInfo.creditScore }}</div>
            <div class="score-label">信用分</div>
          </div>
          <el-progress
            :percentage="userInfo.creditScore"
            :stroke-width="10"
            :color="getCreditColor(userInfo.creditScore)"
            :show-text="false"
            class="credit-progress"
          />
        </div>

        <el-descriptions :column="1" border class="user-details">
          <el-descriptions-item label="用户编号">
            {{ userInfo.id }}
          </el-descriptions-item>
          <el-descriptions-item label="用户名">
            <el-tag type="success">{{ userInfo.username }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="信用分">
            <el-tag :type="getCreditTagType(userInfo.creditScore)" size="large">
              {{ userInfo.creditScore }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ formatTime(userInfo.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="用户状态">
            <el-tag :type="userInfo.status === 1 ? 'success' : 'danger'">
              {{ userInfo.status === 1 ? '正常' : '冻结' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-buttons">
          <el-button type="primary" @click="goToProducts">
            <el-icon><HomeFilled /></el-icon>
            返回商品列表
          </el-button>
          <el-button type="danger" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, HomeFilled, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)

const userInfo = ref({
  id: null,
  username: '',
  creditScore: 0,
  createTime: '',
  status: 1
})

const loadUserInfo = () => {
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    try {
      const parsed = JSON.parse(storedUserInfo)
      userInfo.value = {
        id: parsed.id,
        username: parsed.username,
        creditScore: parsed.creditScore || 0,
        createTime: parsed.createTime || '',
        status: parsed.status || 1
      }
    } catch (error) {
      console.error('解析用户信息失败:', error)
      ElMessage.error('用户信息解析失败')
    }
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

const getCreditColor = (score) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getCreditTagType = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

const goToProducts = () => {
  router.push('/products')
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.personal-center {
  max-width: 800px;
  margin: 40px auto;
  padding: 20px;
}

.user-card {
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.user-info {
  padding: 20px 0;
}

.credit-score-section {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: #fff;
}

.credit-score {
  margin-bottom: 15px;
}

.score-value {
  font-size: 56px;
  font-weight: 700;
  line-height: 1;
}

.score-label {
  font-size: 18px;
  margin-top: 10px;
  opacity: 0.9;
}

.credit-progress {
  max-width: 400px;
  margin: 0 auto;
}

.user-details {
  margin-bottom: 30px;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
  width: 120px;
}

:deep(.el-descriptions__content) {
  font-size: 15px;
}

.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.action-buttons .el-button {
  min-width: 150px;
  font-size: 16px;
  padding: 12px 24px;
}

.action-buttons .el-button .el-icon {
  margin-right: 8px;
}
</style>
