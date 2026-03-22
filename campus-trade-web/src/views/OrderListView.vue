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

        <el-table-column label="订单进度" min-width="250">
          <template #default="{ row }">
            <el-steps :active="getStepActive(row.status)" finish-status="success" align-center>
              <el-step title="待支付" />
              <el-step title="待收货" />
              <el-step title="已完成" />
            </el-steps>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="actions">
              <el-button
                v-if="row.status === 'PENDING'"
                type="success"
                size="small"
                :loading="confirmingId === row.id"
                @click="handleConfirm(row)"
              >
                确认收货
              </el-button>
              <el-button
                v-if="row.status === 'PENDING'"
                type="danger"
                size="small"
                :loading="cancellingId === row.id"
                @click="handleCancel(row)"
              >
                取消订单
              </el-button>
              <el-tag v-if="row.status === 'CONFIRMED'" type="success" size="small">
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, confirmOrder, cancelOrder } from '../api/order'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const confirmingId = ref(null)
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
    'CONFIRMED': '已完成',
    'FINISHED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status] || status
}

const getStepActive = (status) => {
  const steps = {
    'PENDING': 1,
    'PAID': 2,
    'CONFIRMED': 3,
    'FINISHED': 3,
    'CANCELLED': 0
  }
  return steps[status] || 0
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

const handleConfirm = async (order) => {
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
      ElMessage.error('确认失败')
    }
  } finally {
    confirmingId.value = null
  }
}

const handleCancel = async (order) => {
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
      ElMessage.error('取消失败')
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

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
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
