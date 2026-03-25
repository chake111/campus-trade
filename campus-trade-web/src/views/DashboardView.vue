<template>
  <div class="dashboard-page" v-loading="loading">
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
        <div class="admin-products-header">
          <div class="section-title">管理员商品管理</div>
          <div class="admin-products-filters">
            <el-input
              v-model="adminKeyword"
              placeholder="按商品标题/描述/发布人搜索"
              clearable
              style="width: 240px"
              @keyup.enter="fetchAdminProducts"
            />
            <el-select v-model="adminStatus" style="width: 140px" @change="fetchAdminProducts">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button type="primary" @click="fetchAdminProducts">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="adminProducts" stripe border v-loading="adminLoading">
        <el-table-column label="商品图片" width="110">
          <template #default="{ row }">
            <img :src="row.displayImage" alt="商品图片" class="thumb" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" width="140">
          <template #default="{ row }">{{ row.publisherName || `用户#${row.userId || '-'}` }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">{{ getStatusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewProduct(row)">查看</el-button>
            <el-button v-if="Number(row.status) === 1" link type="warning" @click="handleOffShelf(row)">下架</el-button>
            <el-button v-else link type="success" @click="handleRestore(row)">恢复上架</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import {
  deleteProductByAdmin,
  getAdminProductList,
  offShelfProduct,
  restoreProduct,
} from '../api/product'
import { getProductStatusMeta, normalizeProductResponseList } from '../utils/productNormalizer'

const loading = ref(false)
const errorTips = ref([])
const router = useRouter()

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

const adminLoading = ref(false)
const adminProducts = ref([])
const adminKeyword = ref('')
const adminStatus = ref('')
const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 1, label: '在售' },
  { value: 0, label: '已下架' },
  { value: 2, label: '已售出' },
]

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

const getAdminQuery = () => {
  const query = {}
  const keyword = adminKeyword.value?.trim()
  if (keyword) query.keyword = keyword
  if (adminStatus.value !== '' && adminStatus.value !== null && adminStatus.value !== undefined) {
    query.status = adminStatus.value
  }
  return query
}

const fetchAdminProducts = async () => {
  adminLoading.value = true
  try {
    const res = await getAdminProductList(getAdminQuery())
    adminProducts.value = normalizeProductResponseList(res)
  } catch (error) {
    adminProducts.value = []
    console.error('管理员商品列表获取失败:', error)
    ElMessage.error('加载失败，请刷新后重试')
  } finally {
    adminLoading.value = false
  }
}

const getStatusLabel = (status) => getProductStatusMeta(status).label

const formatDateTime = (value) => {
  if (!value) return '-'
  if (typeof value === 'string') return value.replace('T', ' ')
  return String(value)
}

const getProductId = (row) => row?.id ?? row?.productId ?? null

const viewProduct = (row) => {
  const id = getProductId(row)
  if (!id) return
  router.push(`/product/${id}`)
}

const refreshAfterAction = async (msg) => {
  ElMessage.success(msg)
  await fetchAdminProducts()
}

const handleOffShelf = async (row) => {
  const id = getProductId(row)
  if (!id) return
  await offShelfProduct(id)
  await refreshAfterAction('提交成功')
}

const handleRestore = async (row) => {
  const id = getProductId(row)
  if (!id) return
  await restoreProduct(id)
  await refreshAfterAction('提交成功')
}

const handleDelete = async (row) => {
  const id = getProductId(row)
  if (!id) return
  await deleteProductByAdmin(id)
  await refreshAfterAction('提交成功')
}

const initDashboard = async () => {
  loading.value = true
  errorTips.value = []
  await Promise.all([fetchDashboardSummary(), fetchAdminProducts()])

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

.admin-products-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.admin-products-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.thumb {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #f0e1b9;
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
