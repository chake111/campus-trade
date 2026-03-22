<template>
  <div class="dashboard-page" v-loading="loading">
    <el-card class="hero-card" shadow="never">
      <h1>校园二手交易系统数据看板</h1>
      <p>用于毕业答辩展示的平台运营概览（演示级聚合统计，优先复用现有接口）。</p>
      <span class="hero-badge">轻咸鱼风 · 统一视觉</span>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col v-for="item in statCards" :key="item.label" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
          <div v-if="item.tip" class="stat-tip">{{ item.tip }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      v-if="errorTips.length"
      title="部分统计数据获取失败，已自动降级展示"
      type="warning"
      :closable="false"
      show-icon
      class="mt-16"
    >
      <template #default>
        <div v-for="tip in errorTips" :key="tip">- {{ tip }}</div>
      </template>
    </el-alert>

    <el-card class="feature-card mt-16">
      <template #header>
        <div class="section-title">平台亮点 / 系统价值</div>
      </template>
      <el-row :gutter="20">
        <el-col :xs="24" :md="12" :lg="8">
          <div class="feature-item">
            <h3>订单状态机</h3>
            <p>覆盖 PENDING → PAID → CONFIRMED → FINISHED，并支持 CANCELLED，交易过程可追踪。</p>
          </div>
        </el-col>
        <el-col :xs="24" :md="12" :lg="8">
          <div class="feature-item">
            <h3>信用系统联动</h3>
            <p>个人中心展示信用分与信用日志，帮助约束交易行为，强化校园交易信任机制。</p>
          </div>
        </el-col>
        <el-col :xs="24" :md="12" :lg="8">
          <div class="feature-item">
            <h3>个性化推荐</h3>
            <p>基于用户行为提供推荐商品与解释信息，提升浏览效率和成交转化潜力。</p>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../utils/request'
import { getProductList } from '../api/product'
import { getOrderList } from '../api/order'
import { getUserId } from '../utils/user'

const loading = ref(false)
const errorTips = ref([])

const productTotal = ref('--')
const orderTotal = ref('--')
const finishedOrderTotal = ref('--')
const cancelledOrderTotal = ref('--')
const creditScore = ref('--')

const currentUserId = ref(getUserId())

const statCards = computed(() => [
  { label: '商品总数', value: productTotal.value },
  {
    label: '订单总数',
    value: orderTotal.value,
    tip: currentUserId.value ? '基于当前用户可见订单聚合' : '登录后可展示订单统计'
  },
  {
    label: '已完成订单数',
    value: finishedOrderTotal.value,
    tip: currentUserId.value ? '' : '登录后可展示'
  },
  {
    label: '已取消订单数',
    value: cancelledOrderTotal.value,
    tip: currentUserId.value ? '' : '登录后可展示'
  },
  {
    label: '当前信用分',
    value: creditScore.value,
    tip: currentUserId.value ? '' : '登录后可展示'
  }
])

const getApiData = (res) => (res && typeof res === 'object' && 'data' in res ? res.data : res)

const extractList = (res) => {
  const data = getApiData(res)
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.data)) return data.data
  return []
}

const normalizeCreditScore = (payload) => {
  const data = getApiData(payload)
  if (typeof data === 'number') return data
  const candidate =
    data?.creditScore ??
    data?.score ??
    data?.credit ??
    data?.value

  const n = Number(candidate)
  return Number.isFinite(n) ? n : '--'
}

const fetchProductStats = async () => {
  try {
    const list = extractList(await getProductList())
    productTotal.value = list.length
  } catch (error) {
    productTotal.value = '--'
    errorTips.value.push('商品总数统计失败（已降级）')
  }
}

const deduplicateOrders = (orders) => {
  const map = new Map()
  orders.forEach((item) => {
    const key = item?.id ?? `${item?.orderId ?? ''}-${item?.createTime ?? ''}`
    if (!map.has(key)) map.set(key, item)
  })
  return Array.from(map.values())
}

const fetchOrderStats = async () => {
  if (!currentUserId.value) {
    orderTotal.value = '--'
    finishedOrderTotal.value = '--'
    cancelledOrderTotal.value = '--'
    return
  }

  try {
    const [buyerRes, sellerRes] = await Promise.all([
      getOrderList(currentUserId.value, 'buyer'),
      getOrderList(currentUserId.value, 'seller')
    ])

    const merged = deduplicateOrders([...extractList(buyerRes), ...extractList(sellerRes)])
    orderTotal.value = merged.length
    finishedOrderTotal.value = merged.filter((item) => item?.status === 'FINISHED').length
    cancelledOrderTotal.value = merged.filter((item) => item?.status === 'CANCELLED').length
  } catch (error) {
    orderTotal.value = '--'
    finishedOrderTotal.value = '--'
    cancelledOrderTotal.value = '--'
    errorTips.value.push('订单统计失败（请确认 /order/list 接口可用）')
  }
}

const fetchCreditScore = async () => {
  if (!currentUserId.value) {
    creditScore.value = '--'
    return
  }

  try {
    const res = await request({
      url: '/credit/score',
      method: 'get',
      params: { userId: currentUserId.value }
    })
    creditScore.value = normalizeCreditScore(res)
  } catch (error) {
    creditScore.value = '--'
    errorTips.value.push('信用分获取失败（请确认 /credit/score 接口可用）')
  }
}

const initDashboard = async () => {
  loading.value = true
  errorTips.value = []
  currentUserId.value = getUserId()

  await Promise.allSettled([
    fetchProductStats(),
    fetchOrderStats(),
    fetchCreditScore()
  ])

  loading.value = false
}

onMounted(() => {
  initDashboard()
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1480px;
  margin: 0 auto;
}

.hero-card {
  margin-bottom: 22px;
  border-radius: 16px;
  border: 1px solid #efdca8;
  background: linear-gradient(135deg, #fffdf6 0%, #fff4cf 100%);
  color: #3c3c3c;
  padding: 8px 6px;
}

.hero-card h1 {
  margin: 0;
  font-size: 34px;
}

.hero-card p {
  margin: 12px 0 0;
  font-size: 16px;
  color: #6d5a30;
}

.hero-badge {
  display: inline-block;
  margin-top: 16px;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: #7d5a06;
  background: #ffefbe;
  border: 1px solid #efd18a;
}

.stats-row {
  margin-top: 6px;
}

.stat-card {
  margin-bottom: 20px;
  min-height: 158px;
  border-radius: 14px;
  border: 1px solid #f2e7c9;
  padding: 6px 4px;
}

.stat-label {
  font-size: 15px;
  color: #909399;
}

.stat-value {
  margin-top: 12px;
  font-size: 40px;
  font-weight: 700;
  color: #f0673a;
  line-height: 1.1;
}

.stat-tip {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
  min-height: 20px;
}

.feature-card {
  border-radius: 14px;
  border: 1px solid #f0e1b9;
  margin-top: 8px;
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  color: #4a3a18;
}

.feature-item {
  margin-bottom: 14px;
  padding: 18px 18px 16px;
  background: #fffaf0;
  border: 1px solid #f3e5c1;
  border-radius: 10px;
  height: 100%;
}

.feature-item h3 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #303133;
}

.feature-item p {
  margin: 0;
  font-size: 15px;
  color: #606266;
  line-height: 1.8;
}

.mt-16 {
  margin-top: 16px;
}

@media (max-width: 992px) {
  .dashboard-page {
    max-width: 100%;
  }

  .hero-card h1 {
    font-size: 29px;
  }

  .stat-value {
    font-size: 34px;
  }
}
</style>
