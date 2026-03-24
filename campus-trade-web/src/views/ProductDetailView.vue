<template>
  <div class="product-detail-page" v-loading="loading">
    <el-card class="detail-card" shadow="never">
      <template #header>
        <div class="page-header">
          <div class="title-wrap">
            <h2>商品详情</h2>
            <p>查看商品信息并完成下单</p>
          </div>
          <el-button text @click="goBack">返回列表</el-button>
        </div>
      </template>

      <el-empty
        v-if="!loading && !product"
        description="商品不存在或已下架"
      />

      <div v-else-if="product" class="detail-content">
        <div class="image-panel">
          <el-image
            v-if="product.displayImage"
            :src="product.displayImage"
            fit="cover"
            class="product-image"
            :preview-src-list="[product.displayImage]"
          />
          <div v-else class="image-placeholder">暂无商品图片</div>
        </div>

        <div class="info-panel">
          <div class="name-row">
            <h3>{{ product.title || product.name || '未命名商品' }}</h3>
            <el-tag :type="statusTagType" effect="light">{{ statusText }}</el-tag>
          </div>

          <div class="price-row">
            <span class="label">价格</span>
            <span class="price">¥{{ product.price ?? '--' }}</span>
          </div>

          <div class="decision-row">
            <span class="status-chip">商品状态：{{ statusText }}</span>
            <span class="trade-location-chip">校内交易地点：{{ tradeLocationText }}</span>
          </div>

          <el-descriptions :column="1" border class="meta-info">
            <el-descriptions-item label="商品ID">{{ product.id || routeProductId }}</el-descriptions-item>
            <el-descriptions-item label="卖家ID">{{ sellerIdDisplay }}</el-descriptions-item>
          </el-descriptions>

          <div class="description-block">
            <div class="block-title">商品描述</div>
            <p>{{ product.description || '卖家暂无描述信息' }}</p>
          </div>

          <div class="actions">
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              :disabled="!canCreateOrder"
              @click="handleCreateOrder"
            >
              立即下单
            </el-button>
            <el-button size="large" @click="handleContactSeller">联系卖家</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductDetail } from '../api/product'
import { createOrder } from '../api/order'
import { getUserId } from '../utils/user'
import { getToken } from '../utils/request'
import { getProductStatusMeta, isProductOrderableStatus, normalizeProductResponseDetail } from '../utils/productNormalizer'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const product = ref(null)

const routeProductId = computed(() => route.params.id)
const buyerId = computed(() => getUserId())

const sellerId = computed(() => {
  if (!product.value) return null
  return product.value.sellerId ?? product.value.userId ?? product.value.seller?.id ?? null
})

const sellerIdDisplay = computed(() => sellerId.value ?? '未知')
const tradeLocationText = computed(() => {
  const location = product.value?.tradeLocation
  if (!location || !String(location).trim()) return '校内面交（地点待协商）'
  return location
})

const isProductAvailable = computed(() => {
  return isProductOrderableStatus(product.value?.status)
})

const isOwnProduct = computed(() => {
  if (!sellerId.value || !buyerId.value) return false
  return String(sellerId.value) === String(buyerId.value)
})

const canCreateOrder = computed(() => {
  if (!product.value || !buyerId.value) return false
  if (!sellerId.value) return false
  if (!isProductAvailable.value) return false
  if (isOwnProduct.value) return false
  return true
})

const statusMeta = computed(() => getProductStatusMeta(product.value?.status))

const statusText = computed(() => {
  return statusMeta.value.label
})

const statusTagType = computed(() => {
  if (statusMeta.value.type === 'on-sale') return 'success'
  if (statusMeta.value.type === 'sold') return 'info'
  if (statusMeta.value.type === 'off-shelf') return 'warning'
  return 'primary'
})

const fetchProductDetail = async () => {
  const id = routeProductId.value
  if (!id) {
    ElMessage.error('缺少商品ID，无法查看详情')
    product.value = null
    return
  }

  loading.value = true
  try {
    const res = await getProductDetail(id)
    product.value = normalizeProductResponseDetail(res)

    if (!product.value) {
      ElMessage.warning('未找到该商品信息')
    }
  } catch (error) {
    product.value = null
    const msg = error?.response?.data?.message || error?.message || '获取商品详情失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const handleCreateOrder = async () => {
  if (!product.value) {
    ElMessage.warning('商品信息不存在，无法下单')
    return
  }

  const token = getToken()
  if (!token || !buyerId.value) {
    ElMessage.warning('请先登录后再下单')
    router.push('/login')
    return
  }

  if (!sellerId.value) {
    ElMessage.error('卖家信息缺失，暂时无法下单')
    return
  }

  if (!isProductAvailable.value) {
    ElMessage.warning('商品当前状态不可下单')
    return
  }

  if (isOwnProduct.value) {
    ElMessage.warning('不能购买自己发布的商品')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认立即下单该商品吗？提交后可在订单页继续支付和跟进状态。',
      '下单确认',
      { type: 'warning', confirmButtonText: '确认下单', cancelButtonText: '再看看' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = {
      productId: product.value.id ?? routeProductId.value,
      userId: buyerId.value,
    }
    await createOrder(payload)
    ElMessage.success('下单成功，正在跳转到订单页')
    router.push('/orders')
  } catch (error) {
    const msg = error?.message || error?.response?.data?.message || error?.data?.message || '下单失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

const handleContactSeller = () => {
  ElMessage.info('当前版本暂未接入聊天功能')
}

const goBack = () => {
  router.push('/products')
}

onMounted(() => {
  fetchProductDetail()
})
</script>

<style scoped>
.product-detail-page {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px;
}

.detail-card {
  border-radius: 12px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-wrap h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.title-wrap p {
  margin: 6px 0 0;
  font-size: 14px;
  color: #909399;
}

.detail-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.image-panel {
  min-height: 360px;
}

.product-image {
  width: 100%;
  height: 100%;
  min-height: 360px;
  border-radius: 10px;
  border: 1px solid #ebeef5;
}

.image-placeholder {
  height: 360px;
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #fafafa;
}

.info-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.name-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.name-row h3 {
  margin: 0;
  font-size: 26px;
  color: #303133;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 14px;
  background: #f5f7fa;
  border-radius: 8px;
}

.price-row .label {
  font-size: 14px;
  color: #606266;
}

.price-row .price {
  font-size: 34px;
  color: #f56c6c;
  font-weight: 700;
}

.meta-info {
  margin-top: 4px;
}

.decision-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.trade-location-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 13px;
  color: #7a4c00;
  background: #fff2d8;
  border: 1px solid #f6d7a6;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 13px;
  color: #1f7a3d;
  background: #edf9f1;
  border: 1px solid #b8ebc8;
}

.description-block {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.block-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.description-block p {
  margin: 0;
  line-height: 1.7;
  color: #606266;
  white-space: pre-wrap;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

@media (max-width: 900px) {
  .detail-content {
    grid-template-columns: 1fr;
  }
}
</style>
