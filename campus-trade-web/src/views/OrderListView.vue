<template>
  <div class="order-list">
    <div class="order-flow" v-loading="loading">
      <div class="toolbar">
        <el-radio-group v-model="activeRole" size="default" @change="handleRoleChange">
          <el-radio-button label="buyer">我买到的</el-radio-button>
          <el-radio-button label="seller">我卖出的</el-radio-button>
        </el-radio-group>

        <div class="toolbar-right">
          <el-select v-model="statusFilter" placeholder="状态筛选" class="status-filter">
            <el-option
              v-for="item in statusTabs"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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

      <el-tabs v-model="statusFilter" class="status-tabs">
        <el-tab-pane
          v-for="item in statusTabs"
          :key="item.value"
          :label="item.label"
          :name="item.value"
        />
      </el-tabs>

      <div v-if="filteredOrders.length" class="order-cards">
        <el-card
          v-for="row in filteredOrders"
          :key="row.id"
          class="order-card"
          shadow="hover"
        >
          <div class="card-top">
            <div class="aux-meta">
              <span>{{ getCounterpartLabel(row) }}</span>
              <span class="dot">·</span>
              <span>下单时间：{{ formatTime(row.createTime || row.createdAt) }}</span>
              <span class="dot">·</span>
              <span>订单号：{{ row.id || '-' }}</span>
            </div>
            <el-tag class="status-badge" :type="getStatusType(row.status)" effect="dark" size="large">
              {{ getStatusText(row.status) }}
            </el-tag>
          </div>

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
              <div class="goods-price-wrap">
                <span class="price-label">成交价</span>
                <span class="goods-price">¥{{ formatPrice(row.product?.price ?? row.productPrice ?? 0) }}</span>
              </div>
            </div>

            <div class="order-side">
              <div class="actions">
                <el-button
                  v-if="getPrimaryAction(row)"
                  :type="getActionButtonType(getPrimaryAction(row))"
                  class="action-primary"
                  :loading="getActionLoading(row.id, getPrimaryAction(row))"
                  :disabled="getActionDisabled(row, getPrimaryAction(row))"
                  @click="handleOrderAction(row, getPrimaryAction(row))"
                >
                  {{ getPrimaryAction(row).label }}
                </el-button>
                <div class="secondary-actions">
                  <el-button
                    v-for="action in getSecondaryActions(row)"
                    :key="action.key"
                    class="action-secondary"
                    :text="action.isText"
                    :loading="getActionLoading(row.id, action)"
                    :disabled="getActionDisabled(row, action)"
                    @click="handleOrderAction(row, action)"
                  >
                    {{ action.label }}
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="!filteredOrders.length && !loading" class="empty-state">
        <el-empty description="暂无数据" />
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
import {
  getOrderActions,
  getOrderStatusTagType,
  getOrderStatusText,
  getOrderTabOptions,
  getPrimaryOrderAction,
  matchesOrderStatusTab,
} from '../utils/orderDisplay'

const router = useRouter()
const route = useRoute()
const orders = ref([])
const loading = ref(false)
const actionLoading = ref({ id: null, status: '' })
const statusFilter = ref('ALL')
const keyword = ref('')
const activeRole = ref(getInitialRole())
const statusTabs = getOrderTabOptions()

const triggerLoginDialog = (message, redirectPath) => {
  window.dispatchEvent(
    new CustomEvent('require-login', {
      detail: {
        message,
        redirectPath,
      },
    })
  )
}

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
    const statusMatched =
      matchesOrderStatusTab(order, statusFilter.value)
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
    triggerLoginDialog('请先登录后再操作', '/orders')
    return
  }

  loading.value = true
  try {
    const res = await getOrderList(userId, activeRole.value)
    orders.value = extractOrderList(res)
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('加载失败，请刷新后重试')
    orders.value = []
  } finally {
    loading.value = false
  }
}

const handleRoleChange = () => {
  fetchOrders()
}

const getStatusType = (status) => getOrderStatusTagType(status)
const getStatusText = (status) => getOrderStatusText(status)

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

const getProductId = (order) => {
  return order?.product?.id || order?.productId || null
}

const getActionsForOrder = (order) => {
  return getOrderActions(order, activeRole.value, { canBuyerOperate: canBuyerOperate(order) })
}

const getPrimaryAction = (order) => getPrimaryOrderAction(getActionsForOrder(order))
const getSecondaryActions = (order) =>
  getActionsForOrder(order).filter((action) => action.level !== 'primary')

const getActionButtonType = (action) => action?.buttonType || 'primary'
const getActionLoading = (orderId, action) => {
  if (!action || action.kind !== 'status') return false
  return isActionLoading(orderId, action.targetStatus)
}

const getActionDisabled = (order, action) => {
  if (!action) return true
  if (action.kind === 'status') return isAnyActionLoading(order.id)
  if (action.kind === 'view' || action.kind === 'contact') return !getProductId(order)
  return false
}

const handleViewProduct = (order) => {
  const productId = getProductId(order)
  if (!productId) {
    ElMessage.warning('未找到商品信息')
    return
  }
  router.push(`/product/${productId}`)
}

const handleContact = (order) => {
  const productId = getProductId(order)
  const counterpartId =
    activeRole.value === 'buyer' ? (order?.sellerId ?? null) : (order?.userId ?? null)
  if (!productId) {
    ElMessage.warning('未找到商品信息')
    return
  }
  if (!counterpartId) {
    ElMessage.warning('未找到对方用户信息')
    return
  }
  router.push({ path: '/messages', query: { productId, counterpartId, fromOrderId: order?.id } })
}

const handleOrderAction = (order, action) => {
  if (!action) return
  if (action.kind === 'status') {
    handleStatusUpdate(order, action.targetStatus, action.actionText)
    return
  }
  if (action.kind === 'view') {
    handleViewProduct(order)
    return
  }
  if (action.kind === 'contact') {
    handleContact(order)
  }
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
    ElMessage.success(resolveMessage(res, '提交成功'))
    await fetchOrders()
  } catch (error) {
    if (error === 'cancel' || error?.name === 'Cancel') {
      return
    }
    console.error(`${actionText}操作失败:`, error)
    ElMessage.error('操作失败，请稍后重试')
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

.status-tabs {
  margin-bottom: 14px;
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

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.aux-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 12px;
  flex-wrap: wrap;
}

.status-badge {
  letter-spacing: 0.5px;
  font-weight: 700;
}

.order-main {
  display: grid;
  grid-template-columns: 152px minmax(0, 1fr) 280px;
  gap: 18px;
  align-items: stretch;
}

.goods-media {
  width: 152px;
  height: 152px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0e2bf;
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
  justify-content: center;
  gap: 14px;
}

.goods-title {
  font-size: 20px;
  line-height: 1.45;
  font-weight: 700;
  color: var(--theme-text-primary);
}

.goods-price-wrap {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-label {
  font-size: 13px;
  color: #909399;
}

.goods-price {
  font-size: 30px;
  font-weight: 700;
  color: #f56c6c;
  line-height: 1;
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

.actions {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  flex-direction: column;
}

.action-primary {
  min-width: 120px;
  font-weight: 600;
}

.secondary-actions {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.empty-state {
  padding: 60px 0;
}

@media (max-width: 992px) {
  .order-list {
    max-width: 100%;
    padding: 4px 0 16px;
  }

  .order-flow {
    padding: 14px;
  }

  .order-main {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .card-top {
    align-items: flex-start;
    flex-direction: column-reverse;
  }

  .goods-media {
    width: 100%;
    height: 180px;
  }

  .order-side {
    justify-self: start;
    align-items: flex-start;
  }

  .actions {
    align-items: flex-start;
  }

  .secondary-actions {
    justify-content: flex-start;
  }

  .keyword-input,
  .status-filter {
    width: 100%;
  }
}
</style>
