<template>
  <div class="dashboard-page" v-loading="loading">
    <el-card class="hero-card" shadow="never">
      <h1>校园二手交易系统总览</h1>
      <p>聚焦答辩展示的关键业务数据、订单流转与系统能力（优先复用现有接口）。</p>
      <div class="hero-meta">
        <span class="hero-badge">轻咸鱼风 · 统一视觉</span>
        <span class="hero-badge subtle">订单流 + 信用 + 推荐一页可讲清</span>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col v-for="item in statCards" :key="item.label" :xs="24" :sm="12" :md="8" :lg="12" :xl="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-top">
            <div class="stat-label">{{ item.label }}</div>
            <el-tag size="small" effect="plain" class="stat-tag">{{ item.tag }}</el-tag>
          </div>
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

    <el-card class="flow-card mt-16">
      <template #header>
        <div class="section-title">订单状态流转展示区</div>
      </template>
      <div class="flow-track">
        <template v-for="(item, index) in orderFlowCards" :key="item.status">
          <div class="flow-node">
            <div class="flow-name">{{ item.label }}</div>
            <div class="flow-count">{{ item.value }}</div>
            <div class="flow-tip">{{ item.tip }}</div>
          </div>
          <div v-if="index !== orderFlowCards.length - 1" class="flow-arrow">→</div>
        </template>
      </div>
      <div class="flow-note">
        说明：该区域展示平台级订单在各状态的分布，用于证明系统订单流程已完整接入。
      </div>
    </el-card>

    <el-card class="feature-card mt-16">
      <template #header>
        <div class="section-title">系统能力展示</div>
      </template>
      <el-row :gutter="20">
        <el-col :xs="24" :md="12" :lg="8">
          <div class="feature-item">
            <h3>订单状态机</h3>
            <p>支持待支付→已支付→已确认→已完成/已取消全链路状态流转。</p>
          </div>
        </el-col>
        <el-col :xs="24" :md="12" :lg="8">
          <div class="feature-item">
            <h3>信用系统联动</h3>
            <p>信用分已接入看板展示，可与个人中心信用日志共同说明信任约束机制。</p>
          </div>
        </el-col>
        <el-col :xs="24" :md="12" :lg="8">
          <div class="feature-item">
            <h3>个性化推荐</h3>
            <p>推荐模块已在系统中可用，可结合看板能力说明“交易 + 推荐”双驱动。</p>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../utils/request'

const loading = ref(false)
const errorTips = ref([])

const userTotal = ref('--')
const productTotal = ref('--')
const orderTotal = ref('--')
const finishedOrderTotal = ref('--')

const orderStatusCount = ref({
  PENDING: '--',
  PAID: '--',
  CONFIRMED: '--',
  FINISHED: '--',
  CANCELLED: '--'
})

const displayCount = (value) => {
  if (value === '--') return '--'
  return `${value}`
}

const statCards = computed(() => [
  {
    label: '平台注册用户数',
    value: displayCount(userTotal.value),
    tag: '用户侧',
    tip: '展示平台级用户规模'
  },
  {
    label: '平台商品总数',
    value: displayCount(productTotal.value),
    tag: '交易侧',
    tip: '展示平台级商品总览'
  },
  {
    label: '平台订单总数',
    value: displayCount(orderTotal.value),
    tag: '交易侧',
    tip: '展示平台级订单总览'
  },
  {
    label: '已完成订单数',
    value: displayCount(finishedOrderTotal.value),
    tag: '履约侧',
    tip: '用于证明交易闭环可达“完成”状态'
  }
])

const orderFlowCards = computed(() => [
  { status: 'PENDING', label: '待支付', value: displayCount(orderStatusCount.value.PENDING), tip: '下单后初始状态' },
  { status: 'PAID', label: '已支付', value: displayCount(orderStatusCount.value.PAID), tip: '支付完成待确认' },
  { status: 'CONFIRMED', label: '已确认', value: displayCount(orderStatusCount.value.CONFIRMED), tip: '卖家确认发货/交易' },
  { status: 'FINISHED', label: '已完成', value: displayCount(orderStatusCount.value.FINISHED), tip: '交易闭环完成' },
  { status: 'CANCELLED', label: '已取消', value: displayCount(orderStatusCount.value.CANCELLED), tip: '异常/放弃订单终止' }
])

const initOrderStatusCount = () => ({
  PENDING: '--',
  PAID: '--',
  CONFIRMED: '--',
  FINISHED: '--',
  CANCELLED: '--'
})

const fetchDashboardSummary = async () => {
  try {
    const res = await request({
      url: '/api/admin/dashboard/summary',
      method: 'get',
    })
    const data = res?.data || {}
    userTotal.value = data.userTotal ?? '--'
    productTotal.value = data.productTotal ?? '--'
    orderTotal.value = data.orderTotal ?? '--'
    finishedOrderTotal.value = data.finishedOrderTotal ?? '--'
    orderStatusCount.value = {
      ...initOrderStatusCount(),
      ...(data.orderStatusCount || {}),
    }
  } catch (error) {
    userTotal.value = '--'
    productTotal.value = '--'
    orderTotal.value = '--'
    finishedOrderTotal.value = '--'
    orderStatusCount.value = initOrderStatusCount()
    errorTips.value.push('平台级统计获取失败（请确认管理员权限与接口可用）')
  }
}

const initDashboard = async () => {
  loading.value = true
  errorTips.value = []
  await fetchDashboardSummary()

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

.hero-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.hero-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: #7d5a06;
  background: #ffefbe;
  border: 1px solid #efd18a;
}

.hero-badge.subtle {
  color: #8c6a1d;
  background: #fff4d9;
}

.stats-row {
  margin-top: 6px;
}

.stat-card {
  margin-bottom: 20px;
  min-height: 164px;
  border-radius: 14px;
  border: 1px solid #f2e7c9;
  padding: 6px 4px;
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.stat-label {
  font-size: 15px;
  color: #909399;
}

.stat-tag {
  border-color: #edd8aa;
  color: #967028;
  background: #fff8e5;
}

.stat-value {
  margin-top: 12px;
  font-size: 38px;
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

.flow-card,
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

.flow-track {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.flow-node {
  min-width: 190px;
  flex: 1;
  background: #fffaf0;
  border: 1px solid #f3e5c1;
  border-radius: 12px;
  padding: 14px;
}

.flow-name {
  color: #8f6a1f;
  font-size: 14px;
}

.flow-count {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #eb653c;
}

.flow-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

.flow-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #c6a55c;
  min-width: 22px;
}

.flow-note {
  margin-top: 12px;
  font-size: 13px;
  color: #8a7441;
  background: #fff7df;
  border: 1px dashed #eacb88;
  border-radius: 8px;
  padding: 8px 12px;
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

  .flow-node {
    min-width: 160px;
  }
}
</style>
