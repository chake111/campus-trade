<template>
  <div class="personal-center">
    <div class="page-hero">
      <h1>个人中心</h1>
      <p>集中查看个人信息、信用分与信用记录，沉淀可信校园交易身份。</p>
    </div>

    <el-card class="user-card">
      <template #header>
        <div class="card-header">
          <el-icon size="24"><User /></el-icon>
          <span>个人中心</span>
        </div>
      </template>

      <div v-loading="loading" class="user-info">
        <div class="credit-score-section">
          <el-alert
            v-if="creditScoreState === 'forbidden'"
            title="信用分没有访问权限"
            type="warning"
            :closable="false"
            class="credit-alert"
          />
          <el-alert
            v-else-if="creditScoreState === 'error'"
            :title="creditScoreError || '信用分获取失败'"
            type="error"
            :closable="false"
            class="credit-alert"
          />
          <div class="credit-score">
            <div class="score-value">{{ creditScoreDisplay }}</div>
            <div class="score-label">信用接口实时信用分</div>
          </div>
          <el-progress
            :percentage="progressValue"
            :stroke-width="10"
            :color="getCreditColor(scoreForDisplay)"
            :show-text="false"
            class="credit-progress"
          />
          <div class="score-tag-wrap">
            <el-tag :type="getCreditTagType(scoreForDisplay)" size="large">信用等级：{{ creditLevelText }}</el-tag>
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
            <el-tag :type="getCreditTagType(scoreForDisplay)" size="large">
              {{ creditScoreDisplay }}
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

          <el-alert
            v-if="creditLogsState === 'forbidden'"
            title="信用记录没有访问权限"
            type="warning"
            :closable="false"
            class="credit-alert"
          />
          <el-alert
            v-else-if="creditLogsState === 'error'"
            :title="creditLogsError || '信用记录获取失败'"
            type="error"
            :closable="false"
            class="credit-alert"
          />

          <el-table
            v-if="creditLogsState === 'success' && creditLogs.length"
            :data="creditLogs"
            border
            stripe
            style="width: 100%"
          >
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

          <el-empty v-else-if="creditLogsState === 'success'" description="暂无信用记录" />
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
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, HomeFilled, SwitchButton } from '@element-plus/icons-vue'
import { getCreditLogs, getCreditScore } from '../api/credit'
import { AUTH_CHANGED_EVENT, dispatchAuthChanged, getUserId, getUserInfo, removeUserInfo } from '../utils/user'
import { removeToken } from '../utils/request'

const router = useRouter()
const loading = ref(false)
const creditScore = ref(null)
const creditLogs = ref([])
const creditScoreState = ref('idle')
const creditLogsState = ref('idle')
const creditScoreError = ref('')
const creditLogsError = ref('')

const userInfo = ref({
  id: null,
  username: '',
  createTime: '',
  status: 1
})

const scoreForDisplay = computed(() => (creditScore.value == null ? 0 : Number(creditScore.value) || 0))
const progressValue = computed(() => Math.max(0, Math.min(scoreForDisplay.value, 100)))
const creditScoreDisplay = computed(() => (creditScore.value == null ? '-' : scoreForDisplay.value))

const creditLevelText = computed(() => {
  if (creditScore.value == null) return '-'
  if (scoreForDisplay.value >= 80) return '优秀'
  if (scoreForDisplay.value >= 60) return '良好'
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
    id: localUserInfo.id ?? localUserInfo.userId ?? null,
    username: localUserInfo.username || '',
    createTime: localUserInfo.createTime || '',
    status: localUserInfo.status ?? 1
  }
}

const fetchCreditScore = async (userId) => {
  creditScoreState.value = 'loading'
  creditScoreError.value = ''
  const res = await getCreditScore({ userId })
  creditScore.value = normalizeCreditScore(res)
  creditScoreState.value = 'success'
}

const fetchCreditLogs = async (userId) => {
  creditLogsState.value = 'loading'
  creditLogsError.value = ''
  const res = await getCreditLogs({ userId })
  creditLogs.value = normalizeCreditLogs(res)
  creditLogsState.value = 'success'
}

const resolveCreditError = (error, fallback) => {
  const status = error?.status || error?.response?.status
  const message =
    error?.data?.message ||
    error?.response?.data?.message ||
    error?.response?.data?.data?.message ||
    error?.message ||
    fallback

  if (status === 403) {
    return { state: 'forbidden', message: '没有访问权限' }
  }

  return { state: 'error', message }
}

const fetchCreditData = async () => {
  const userId = getUserId() ?? userInfo.value.id
  if (!userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  loading.value = true
  try {
    await fetchCreditScore(userId)
  } catch (error) {
    creditScore.value = null
    const { state, message } = resolveCreditError(error, '获取信用分失败')
    creditScoreState.value = state
    creditScoreError.value = message
  }

  try {
    await fetchCreditLogs(userId)
  } catch (error) {
    const { state, message } = resolveCreditError(error, '获取信用记录失败')
    creditLogsState.value = state
    creditLogsError.value = message
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
  dispatchAuthChanged()
  ElMessage.success('已退出登录')
  router.push('/login')
}

const syncAuthState = () => {
  loadUserInfo()
  fetchCreditData()
}

onMounted(() => {
  loadUserInfo()
  fetchCreditData()
  window.addEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})

onUnmounted(() => {
  window.removeEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})
</script>

<style scoped>
.personal-center {
  max-width: 1320px;
  margin: 10px auto 26px;
  padding: 12px 8px 24px;
}

.page-hero {
  margin-bottom: 22px;
  padding: 24px 28px;
  border-radius: 16px;
  border: 1px solid #efdca8;
  background: linear-gradient(135deg, #fffef8 0%, #fff4cf 100%);
}

.page-hero h1 {
  margin: 0;
  font-size: 34px;
  color: #3d3220;
}

.page-hero p {
  margin: 10px 0 0;
  font-size: 16px;
  color: #7a6740;
}

.user-card {
  border-radius: 16px;
  border: 1px solid #f0e3be;
  box-shadow: 0 10px 24px rgba(67, 53, 26, 0.08);
}

.user-card :deep(.el-card__header) {
  padding: 18px 24px;
}

.user-card :deep(.el-card__body) {
  padding: 24px;
}

.credit-alert {
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #4a3915;
}

.user-info {
  padding: 4px 0;
}

.credit-score-section {
  text-align: center;
  margin-bottom: 28px;
  padding: 24px;
  background: linear-gradient(135deg, #fffef8 0%, #fff1c5 100%);
  border: 1px solid #efdba1;
  border-radius: 8px;
  color: #3d3526;
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
  color: #7b6541;
}

.score-tag-wrap {
  margin-top: 16px;
}

.credit-progress {
  max-width: 520px;
  margin: 0 auto;
}

.user-details {
  margin-bottom: 20px;
}

.credit-log-card {
  margin-bottom: 30px;
  border: 1px solid #f0e4c5;
  border-radius: 12px;
}

.log-header {
  font-weight: 600;
  color: #4b3a16;
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

.action-buttons :deep(.el-button--primary) {
  background: #ffd45a;
  border-color: #ffd45a;
  color: #3c3c3c;
}

.action-buttons :deep(.el-button--primary:hover) {
  background: #ffca33;
  border-color: #ffca33;
}

@media (max-width: 992px) {
  .personal-center {
    max-width: 100%;
    padding: 4px 0 16px;
  }

  .page-hero {
    padding: 18px 20px;
  }

  .page-hero h1 {
    font-size: 28px;
  }

  .user-card :deep(.el-card__header),
  .user-card :deep(.el-card__body) {
    padding: 16px;
  }

  .action-buttons {
    flex-wrap: wrap;
  }
}
</style>
