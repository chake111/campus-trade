<template>
  <div class="order-list">
    <div class="header">
      <h1>我的订单</h1>
    </div>

    <div v-loading="loading" class="order-table">
      <el-table :data="orders" border style="width: 100%">
        <el-table-column prop="id" label="订单编号" width="100" />

        <el-table-column label="商品信息" width="300">
          <template #default="{ row }">
            <div class="product-info">
              <div class="product-title">
                {{ row.product?.title || row.productTitle || `商品ID: ${row.productId || '-'} ` }}
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
                v-if="row.status === 'PENDING'"
                type="primary"
                size="small"
                :loading="isActionLoading(row.id, 'PAID')"
                :disabled="isAnyActionLoading(row.id)"
                @click="handleStatusUpdate(row, 'PAID', '支付')"
              >
                支付
              </el-button>
              <el-button
                v-if="row.status === 'PENDING'"
                type="danger"
                size="small"
                :loading="isActionLoading(row.id, 'CANCELLED')"
                :disabled="isAnyActionLoading(row.id)"
                @click="handleStatusUpdate(row, 'CANCELLED', '取消')"
              >
                取消
              </el-button>
              <el-button
                v-if="row.status === 'PAID'"
                type="success"
                size="small"
                :loading="isActionLoading(row.id, 'CONFIRMED')"
                :disabled="isAnyActionLoading(row.id)"
                @click="handleStatusUpdate(row, 'CONFIRMED', '确认')"
              >
                确认
              </el-button>
              <el-button
                v-if="row.status === 'CONFIRMED'"
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

      <div v-if="!orders.length && !loading" class="empty-state">
        <el-empty description="暂无订单" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, updateOrder } from '../api/order'
import { getUserId } from '../utils/user'

const router = useRouter()
const route = useRoute()
const orders = ref([])
const loading = ref(false)
const actionLoading = ref({ id: null, status: '' })

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

const getCurrentRole = () => {
  const role = route.query?.role
  return typeof role === 'string' && role.trim() ? role.trim() : undefined
}

const fetchOrders = async () => {
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  loading.value = true
  try {
    const role = getCurrentRole()
    const res = await getOrderList(userId, role)
    orders.value = extractOrderList(res)
  } catch (error) {
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.data?.message ||
      error?.message ||
      '加载订单列表失败'
    ElMessage.error(message)
    orders.value = []
  } finally {
    loading.value = false
  }
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
  max-width: 1400px;
  margin: 40px auto;
  padding: 20px;
}

.header {
  margin-bottom: 30px;
}

.header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.order-table {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
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
</style>
