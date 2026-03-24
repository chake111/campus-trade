<template>
  <div class="order-list">
    <div class="header page-hero">
      <h1>我的订单</h1>
      <p>统一查看买入与卖出订单，状态清晰、流程可追踪。</p>
    </div>

    <div class="order-flow" v-loading="loading">
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

      <div v-if="filteredOrders.length" class="order-cards">
        <el-card
          v-for="row in filteredOrders"
          :key="row.id"
          class="order-card"
          shadow="hover"
        >
          <div class="order-main">
            <div class="goods-media">
              <img
                v-if="getProductImage(row)"
                :src="getProductImage(row)"
                alt="商品图片"
              />
              <div v-else class="image-placeholder">暂无商品图片</div>
            </div>

            <div class="goods-content">
              <div class="goods-title">{{ getProductTitle(row) }}</div>
              <div class="goods-price">¥{{ formatPrice(row.product?.price ?? row.productPrice ?? 0) }}</div>
              <div class="meta-row">
                <span>{{ getCounterpartLabel(row) }}</span>
                <span class="dot">·</span>
                <span>下单时间：{{ formatTime(row.createTime || row.createdAt) }}</span>
              </div>
              <div class="meta-row minor">
                <span>订单号：{{ row.id || '-' }}</span>
              </div>
            </div>

            <div class="order-side">
              <el-tag :type="getStatusType(row.status)" size="large">
                {{ getStatusText(row.status) }}
              </el-tag>
              <div class="mini-progress">进度：{{ getStepText(row.status) }}</div>
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
            </div>
          </div>
        </el-card>
      </div>

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

const getStepText = (status) => {
  const steps = {
    PENDING: '待支付',
    PAID: '已支付',
    CONFIRMED: '已确认',
    FINISHED: '已完成',
    CANCELLED: '已取消'
  }
  return steps[status] || '处理中'
}

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time !== 'string') return String(time)
  return time.replace('T', ' ')
}

const isActionLoading = (orderId, targetStatus) => {
  return actionLoading.value.id === orderId && actionLoading.value.status === targetStatus
}

const getProductTitle = (order) => {
  return (
    order?.product?.title ||
    order?.productName ||
    order?.name ||
    order?.productTitle ||
    `商品ID: ${order?.productId || '-'}`
  )
}

const getProductImage = (order) => {
  return (
    order?.product?.displayImage ||
    order?.product?.cover ||
    order?.product?.image ||
    order?.productImage ||
    order?.cover ||
    order?.image ||
    ''
  )
}

const getCounterpartLabel = (order) => {
  if (activeRole.value === 'buyer') {
    return `卖家：${order?.sellerName || order?.sellerId || '-'}`
  }
  return `买家：${order?.buyerName || order?.buyerId || order?.userId || '-'}`
}

const formatPrice = (price) => {
  const numericPrice = Number(price)
  if (Number.isNaN(numericPrice)) return price
  return numericPrice.toFixed(2)
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

.order-flow {
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

.order-cards {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-card {
  border: 1px solid var(--el-border-color-lighter);
}

.order-main {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr) 280px;
  gap: 16px;
  align-items: center;
}

.goods-media {
  width: 132px;
  height: 132px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--theme-border);
  background: var(--theme-bg-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}

.goods-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  padding: 0 8px;
  text-align: center;
  color: var(--theme-text-secondary);
  font-size: 12px;
}

.goods-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.goods-title {
  font-size: 17px;
  line-height: 1.4;
  font-weight: 600;
  color: var(--theme-text-primary);
}

.goods-price {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--theme-text-secondary);
  font-size: 13px;
}

.meta-row.minor {
  color: #909399;
}

.dot {
  color: #c0c4cc;
}

.order-side {
  justify-self: end;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.mini-progress {
  font-size: 13px;
  color: var(--theme-text-secondary);
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
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

  .order-flow {
    padding: 14px;
  }

  .order-main {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .goods-media {
    width: 100%;
    height: 180px;
  }

  .order-side {
    justify-self: start;
    align-items: flex-start;
  }

  .keyword-input,
  .status-filter {
    width: 100%;
  }
}
</style>
