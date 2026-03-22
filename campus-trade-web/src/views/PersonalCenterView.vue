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
            <div class="score-value">{{ creditScore }}</div>
            <div class="score-label">当前信用分</div>
          </div>
          <el-progress
            :percentage="progressValue"
            :stroke-width="10"
            :color="getCreditColor(creditScore)"
            :show-text="false"
            class="credit-progress"
          />
          <div class="score-tag-wrap">
            <el-tag :type="getCreditTagType(creditScore)" size="large">信用等级：{{ creditLevelText }}</el-tag>
          </div>
        </div>

        <el-descriptions :column="1" border class="user-details">
          <el-descriptions-item label="用户编号">
            {{ userInfo.id || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="用户名">
            <el-tag type="success">{{ userInfo.username || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="信用分">
            <el-tag :type="getCreditTagType(creditScore)" size="large">
              {{ creditScore }}
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

        <el-card shadow="never" class="credit-log-card">
          <template #header>
            <div class="log-header">信用变动明细</div>
          </template>

          <el-table v-if="creditLogs.length" :data="creditLogs" border stripe style="width: 100%">
            <el-table-column label="变动分值" width="140" align="center">
              <template #default="{ row }">
                <el-tag :type="row.change > 0 ? 'success' : row.change < 0 ? 'danger' : 'info'">
                  {{ formatChange(row.change) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="变动原因" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" min-width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-else description="暂无信用记录" />
        </el-card>

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
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, HomeFilled, SwitchButton } from '@element-plus/icons-vue'
import request from '../utils/request'
import { getUserId, getUserInfo, removeUserInfo } from '../utils/user'
import { removeToken } from '../utils/request'

const router = useRouter()
const loading = ref(false)
const creditScore = ref(0)
const creditLogs = ref([])

const userInfo = ref({
  id: null,
  username: '',
  createTime: '',
  status: 1
})

const progressValue = computed(() => Math.max(0, Math.min(Number(creditScore.value) || 0, 100)))

const creditLevelText = computed(() => {
  if (creditScore.value >= 80) return '优秀'
  if (creditScore.value >= 60) return '良好'
  return '待提升'
})

const getApiData = (res) => (res && typeof res === 'object' && 'data' in res ? res.data : res)

const toNumber = (value, defaultValue = 0) => {
  const n = Number(value)
  return Number.isFinite(n) ? n : defaultValue
}

const normalizeCreditScore = (payload) => {
  const data = getApiData(payload)
  if (typeof data === 'number') return data
  if (typeof payload === 'number') return payload
  return (
    toNumber(data?.creditScore, NaN) ||
    toNumber(data?.score, NaN) ||
    toNumber(data?.credit, NaN) ||
    toNumber(data?.value, NaN) ||
    0
  )
}

const normalizeCreditLogs = (payload) => {
  const data = getApiData(payload)
  const rawList =
    (Array.isArray(data) && data) ||
    (Array.isArray(data?.records) && data.records) ||
    (Array.isArray(data?.list) && data.list) ||
    (Array.isArray(data?.data) && data.data) ||
    []

  return rawList.map((item) => {
    const change =
      toNumber(item?.scoreChange, NaN) ||
      toNumber(item?.changeValue, NaN) ||
      toNumber(item?.credit, NaN) ||
      0

    return {
      change,
      reason: item?.reason || item?.description || '未提供原因',
      createTime: item?.createTime || item?.time || ''
    }
  })
}

const loadUserInfo = () => {
  const localUserInfo = getUserInfo()
  if (!localUserInfo) return
  userInfo.value = {
    id: localUserInfo.id ?? null,
    username: localUserInfo.username || '',
    createTime: localUserInfo.createTime || '',
    status: localUserInfo.status ?? 1
  }
  if (localUserInfo.creditScore != null) {
    creditScore.value = toNumber(localUserInfo.creditScore, 0)
  }
}

const fetchCreditScore = async (userId) => {
  const res = await request({
    url: '/credit/score',
    method: 'get',
    params: { userId }
  })
  creditScore.value = normalizeCreditScore(res)
}

const fetchCreditLogs = async (userId) => {
  const res = await request({
    url: '/credit/log',
    method: 'get',
    params: { userId }
  })
  creditLogs.value = normalizeCreditLogs(res)
}

const fetchCreditData = async () => {
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  loading.value = true
  try {
    await Promise.all([fetchCreditScore(userId), fetchCreditLogs(userId)])
  } catch (error) {
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.data?.message ||
      error?.message ||
      '获取信用记录失败'
    ElMessage.error(message)
    creditLogs.value = []
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time !== 'string') return String(time)
  return time.replace('T', ' ')
}

const formatChange = (change) => {
  if (change > 0) return `+${change}`
  return String(change)
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
  removeToken()
  removeUserInfo()
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(() => {
  loadUserInfo()
  fetchCreditData()
})
</script>

<style scoped>
.personal-center {
  max-width: 900px;
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

.score-tag-wrap {
  margin-top: 16px;
}

.credit-progress {
  max-width: 400px;
  margin: 0 auto;
}

.user-details {
  margin-bottom: 20px;
}

.credit-log-card {
  margin-bottom: 30px;
}

.log-header {
  font-weight: 600;
  color: #303133;
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
