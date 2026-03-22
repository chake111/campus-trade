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
              <div class="product-title">{{ row.product?.title || '商品已下架' }}</div>
              <div class="product-price">¥{{ row.product?.price || 0 }}</div>
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

        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <!-- PENDING: 只能支付或取消 -->
            <template v-if="row.status === 'PENDING'">
              <el-button
                type="primary"
                size="small"
                :loading="payingId === row.id"
                @click="handlePay(row)"
              >
                去支付
              </el-button>
              <el-button
                type="danger"
                size="small"
                :loading="cancellingId === row.id"
                @click="handleCancel(row)"
              >
                取消订单
              </el-button>
            </template>

            <!-- PAID: 只能确认收货 -->
            <el-button
              v-else-if="row.status === 'PAID'"
              type="success"
              size="small"
              :loading="confirmingId === row.id"
              @click="handleConfirm(row)"
            >
              确认收货
            </el-button>

            <!-- CONFIRMED: 只能完成订单 -->
            <el-button
              v-else-if="row.status === 'CONFIRMED'"
              type="warning"
              size="small"
              :loading="finishingId === row.id"
              @click="handleFinish(row)"
            >
              完成订单
            </el-button>

            <!-- 其他状态不显示按钮 -->
            <el-tag v-else :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, payOrder, confirmOrder, finishOrder, cancelOrder } from '../api/order'

const orders = ref([])
const loading = ref(false)
const payingId = ref(null)
const confirmingId = ref(null)
const finishingId = ref(null)
const cancellingId = ref(null)

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList()
    orders.value = res.data || []
  } catch (error) {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const types = {
    'PENDING': 'warning',
    'PAID': 'primary',
    'CONFIRMED': 'success',
    'FINISHED': 'success',
    'CANCELLED': 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'PENDING': '待支付',
    'PAID': '待收货',
    'CONFIRMED': '待完成',
    'FINISHED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status] || status
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

const handlePay = async (order) => {
  if (payingId.value) return
  if (order.status !== 'PENDING') {
    ElMessage.warning('订单状态异常，请刷新页面')
    fetchOrders()
    return
  }

  try {
    payingId.value = order.id
    await payOrder(order.id)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch (error) {
    const msg = error.response?.data?.message || '支付失败'
    ElMessage.error(msg)
    fetchOrders()
  } finally {
    payingId.value = null
  }
}

const handleConfirm = async (order) => {
  if (confirmingId.value) return
  if (order.status !== 'PAID') {
    ElMessage.warning('订单状态异常，请刷新页面')
    fetchOrders()
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认已收到商品吗？确认后订单将无法取消',
      '确认收货',
      { type: 'warning' }
    )

    confirmingId.value = order.id
    await confirmOrder(order.id)
    ElMessage.success('确认收货成功，信用分 +2')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error.response?.data?.message || '确认失败'
      ElMessage.error(msg)
      fetchOrders()
    }
  } finally {
    confirmingId.value = null
  }
}

const handleFinish = async (order) => {
  if (finishingId.value) return
  if (order.status !== 'CONFIRMED') {
    ElMessage.warning('订单状态异常，请刷新页面')
    fetchOrders()
    return
  }

  try {
    finishingId.value = order.id
    await finishOrder(order.id)
    ElMessage.success('订单已完成')
    fetchOrders()
  } catch (error) {
    const msg = error.response?.data?.message || '操作失败'
    ElMessage.error(msg)
    fetchOrders()
  } finally {
    finishingId.value = null
  }
}

const handleCancel = async (order) => {
  if (cancellingId.value) return
  if (order.status !== 'PENDING') {
    ElMessage.warning('订单状态异常，请刷新页面')
    fetchOrders()
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要取消订单吗？取消后会影响您的信用记录',
      '取消订单',
      { type: 'warning' }
    )

    cancellingId.value = order.id
    await cancelOrder(order.id, '')
    ElMessage.success('订单已取消，信用分 -3')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error.response?.data?.message || '取消失败'
      ElMessage.error(msg)
      fetchOrders()
    }
  } finally {
    cancellingId.value = null
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

.empty-state {
  padding: 60px 0;
}
</style>
