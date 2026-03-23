<template>
  <div class="order-list">
    <div class="header page-hero">
      <h1>我的订单</h1>
      <p>统一查看买入与卖出订单，状态清晰、流程可追踪。</p>
    </div>

    <div class="order-table" v-loading="loading">
      <div class="toolbar">
        <el-radio-group v-model="activeRole" size="default" @change="handleRoleChange">
          <el-radio-button label="buyer">我买到的</el-radio-button>
          <el-radio-button label="seller">我卖出的</el-radio-button>
        </el-radio-group>

        <div class="toolbar-right">
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable class="status-filter">
            <el-option label="全部" value="ALL" />
            <el-option label="PENDING" value="PENDING" />
            <el-option label="PAID" value="PAID" />
            <el-option label="CONFIRMED" value="CONFIRMED" />
            <el-option label="FINISHED" value="FINISHED" />
            <el-option label="CANCELLED" value="CANCELLED" />
          </el-select>

          <el-input
            v-model="keyword"
            placeholder="搜索商品名 / 订单号"
            clearable
            class="keyword-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-button type="primary" plain @click="fetchOrders">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredOrders" border style="width: 100%">
        <el-table-column prop="id" label="订单编号" width="100" />

        <el-table-column label="商品信息" width="300">
          <template #default="{ row }">
            <div class="product-info">
              <div class="product-title">
                {{ row.product?.title || row.productName || row.name || row.productTitle || `商品ID: ${row.productId || '-'} ` }}
              </div>
              <div class="product-price">¥{{ row.product?.price ?? row.productPrice ?? 0 }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="订单状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="买家/卖家" width="220">
          <template #default="{ row }">
            <div class="user-info">
              <div>买家：{{ row.buyerName || row.buyerId || '-' }}</div>
              <div>卖家：{{ row.sellerName || row.sellerId || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime || row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="订单进度" min-width="250">
          <template #default="{ row }">
            <el-steps :active="getStepActive(row.status)" finish-status="success" align-center>
              <el-step title="待支付" />
              <el-step title="已支付" />
              <el-step title="已确认" />
              <el-step title="已完成" />
            </el-steps>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="actions">
              <el-button
                v-if="canBuyerOperate(row) && row.status === 'PENDING'"
                type="primary"
                size="small"
                :loading="isActionLoading(row.id, 'PAID')"
                :disabled="isAnyActionLoading(row.id)"
                @click="handleStatusUpdate(row, 'PAID', '支付')"
              >
                支付
              </el-button>
              <el-button
                v-if="canBuyerOperate(row) && row.status === 'PENDING'"
                type="danger"
                size="small"
                :loading="isActionLoading(row.id, 'CANCELLED')"
                :disabled="isAnyActionLoading(row.id)"
                @click="handleStatusUpdate(row, 'CANCELLED', '取消')"
              >
                取消
              </el-button>
              <el-button
                v-if="canBuyerOperate(row) && row.status === 'PAID'"
                type="success"
                size="small"
                :loading="isActionLoading(row.id, 'CONFIRMED')"
                :disabled="isAnyActionLoading(row.id)"
                @click="handleStatusUpdate(row, 'CONFIRMED', '确认')"
              >
                确认
              </el-button>
              <el-button
                v-if="canBuyerOperate(row) && row.status === 'CONFIRMED'"
                type="warning"
                size="small"
                :loading="isActionLoading(row.id, 'FINISHED')"
                :disabled="isAnyActionLoading(row.id)"
                @click="handleStatusUpdate(row, 'FINISHED', '完成')"
              >
                完成
              </el-button>
              <el-tag v-if="row.status === 'FINISHED'" type="success" size="small">
                已完成
              </el-tag>
              <el-tag v-if="row.status === 'CANCELLED'" type="info" size="small">
                已取消
              </el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!filteredOrders.length && !loading" class="empty-state">
        <el-empty description="暂无订单" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getOrderList, updateOrder } from '../api/order'
import { getUserId } from '../utils/user'

const router = useRouter()
const route = useRoute()
const orders = ref([])
const loading = ref(false)
const actionLoading = ref({ id: null, status: '' })
const statusFilter = ref('ALL')
const keyword = ref('')
const activeRole = ref(getInitialRole())

function getInitialRole() {
  const role = route.query?.role
  if (typeof role !== 'string') return 'buyer'
  if (role === 'seller' || role === 'buyer') return role
  return 'buyer'
}

const getApiData = (res) => (res && typeof res === 'object' && 'data' in res ? res.data : res)

const extractOrderList = (res) => {
  const data = getApiData(res)
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.data)) return data.data
  return []
}

const resolveMessage = (res, fallback) => {
  const data = getApiData(res)
  return data?.message || res?.message || fallback
}

const filteredOrders = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()

  return orders.value.filter((order) => {
    const statusMatched = statusFilter.value === 'ALL' || order.status === statusFilter.value
    if (!statusMatched) return false

    if (!normalizedKeyword) return true

    const productName = String(
      order.product?.title || order.productName || order.name || order.productTitle || ''
    ).toLowerCase()
    const orderId = String(order.id ?? '').toLowerCase()

    return productName.includes(normalizedKeyword) || orderId.includes(normalizedKeyword)
  })
})

const fetchOrders = async () => {
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  loading.value = true
  try {
    const res = await getOrderList(userId, activeRole.value)
    orders.value = extractOrderList(res)
  } catch (error) {
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.data?.message ||
      error?.message ||
      '获取订单列表失败'
    ElMessage.error(message)
    orders.value = []
  } finally {
    loading.value = false
  }
}

const handleRoleChange = () => {
  fetchOrders()
}

const getStatusType = (status) => {
  const types = {
    PENDING: 'warning',
    PAID: 'primary',
    CONFIRMED: 'success',
    FINISHED: 'success',
    CANCELLED: 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    PENDING: '待支付',
    PAID: '已支付',
    CONFIRMED: '已确认',
    FINISHED: '已完成',
    CANCELLED: '已取消'
  }
  return texts[status] || status
}

const getStepActive = (status) => {
  const steps = {
    PENDING: 1,
    PAID: 2,
    CONFIRMED: 3,
    FINISHED: 4,
    CANCELLED: 0
  }
  return steps[status] || 0
}

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time !== 'string') return String(time)
  return time.replace('T', ' ')
}

const isActionLoading = (orderId, targetStatus) => {
  return actionLoading.value.id === orderId && actionLoading.value.status === targetStatus
}


const canBuyerOperate = (order) => {
  const currentUserId = Number(getUserId())
  const buyerId = Number(order?.userId)
  if (!currentUserId || !buyerId) return false

  // 与后端 /order/update 保持一致：仅买家(或管理员)可更新订单状态
  return activeRole.value === 'buyer' && buyerId === currentUserId
}

const isAnyActionLoading = (orderId) => actionLoading.value.id === orderId

const handleStatusUpdate = async (order, newStatus, actionText) => {
  try {
    await ElMessageBox.confirm(
      `确定要执行“${actionText}”操作吗？`,
      `订单${actionText}确认`,
      { type: 'warning' }
    )

    actionLoading.value = { id: order.id, status: newStatus }
    const res = await updateOrder(order.id, newStatus)
    ElMessage.success(resolveMessage(res, `${actionText}成功`))
    await fetchOrders()
  } catch (error) {
    if (error === 'cancel' || error?.name === 'Cancel') {
      return
    }
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.data?.message ||
      error?.message ||
      `${actionText}失败`
    ElMessage.error(message)
  } finally {
    actionLoading.value = { id: null, status: '' }
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-list {
  max-width: 1520px;
  margin: 10px auto 26px;
  padding: 12px 8px 24px;
}

.header {
  margin-bottom: 22px;
}

.page-hero {
  padding: 24px 28px;
  border-radius: 16px;
  border: 1px solid var(--theme-border);
  background: linear-gradient(135deg, #fffef8 0%, var(--theme-primary-soft-2) 100%);
}

.header h1 {
  font-size: 34px;
  font-weight: 600;
  color: var(--theme-text-primary);
  margin: 0;
}

.header p {
  margin: 10px 0 0;
  font-size: 16px;
  color: var(--theme-text-secondary);
}

.order-table {
  background: var(--theme-card-bg);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
}

.toolbar {
  --order-accent: var(--theme-primary);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 16px;
  background: var(--theme-bg-soft);
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
}

.toolbar :deep(.el-radio-group) {
  --el-radio-button-checked-bg-color: var(--order-accent);
  --el-radio-button-checked-text-color: var(--theme-text-primary);
  --el-radio-button-checked-border-color: var(--order-accent);
}

.toolbar :deep(.el-radio-button__inner) {
  border-color: var(--theme-border);
  color: var(--theme-text-secondary);
  background-color: var(--theme-card-bg);
}

.toolbar :deep(.el-radio-button.is-active .el-radio-button__inner),
.toolbar :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--order-accent);
  border-color: var(--order-accent);
  color: var(--theme-text-primary);
  box-shadow: -1px 0 0 0 var(--order-accent);
}

.toolbar :deep(.el-button--primary.is-plain) {
  color: var(--theme-text-secondary);
  border-color: var(--el-color-primary-light-5);
  background: var(--theme-primary-soft-3);
}

.toolbar :deep(.el-button--primary.is-plain:hover) {
  color: var(--theme-text-primary);
  border-color: var(--el-color-primary-light-3);
  background: var(--theme-primary-soft-1);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.status-filter {
  width: 180px;
}

.keyword-input {
  width: 300px;
}

.order-table :deep(.el-table) {
  font-size: 14px;
}

.order-table :deep(.el-table td.el-table__cell),
.order-table :deep(.el-table th.el-table__cell) {
  padding: 13px 0;
}

.order-table :deep(.el-table th.el-table__cell) {
  background: var(--theme-bg-soft);
  color: var(--theme-text-secondary);
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 16px;
  font-weight: 700;
  color: #f56c6c;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

:deep(.el-steps) {
  font-size: 12px;
}

:deep(.el-step__title) {
  font-size: 13px;
}

.empty-state {
  padding: 60px 0;
}

@media (max-width: 992px) {
  .order-list {
    max-width: 100%;
    padding: 4px 0 16px;
  }

  .page-hero {
    padding: 18px 20px;
  }

  .header h1 {
    font-size: 28px;
  }

  .order-table {
    padding: 14px;
  }

  .keyword-input,
  .status-filter {
    width: 100%;
  }
}
</style>
