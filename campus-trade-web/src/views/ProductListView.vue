<template>
  <div class="product-list">
    <div v-loading="loading" class="product-grid">
      <section class="recommend-section">
          <div class="recommend-head">
            <div class="section-header">
              <span class="recommend-icon-wrap"><el-icon size="18" color="#d99a00"><Star /></el-icon></span>
              <div>
                <p class="section-main-title">运营精选推荐</p>
              </div>
            </div>
            <span class="recommend-tag">平台精选</span>
          </div>

        <template v-if="!currentUserId">
          <div v-if="fallbackRecommendProducts.length > 0" class="recommend-list">
            <article
              v-for="(item, index) in fallbackRecommendProducts"
              :key="`fallback-${getCardKey(item)}-${index}`"
              class="recommend-item"
              @click="viewDetail(item)"
            >
              <div class="image-container">
                <img v-if="item.displayImage" :src="item.displayImage" alt="商品图片" />
                <div v-else class="recommend-placeholder">精选推荐</div>
                <span class="corner-tag">推荐</span>
              </div>
              <div class="content">
                <h3 class="title">{{ item.title }}</h3>
                <p class="location">{{ getTradeLocation(item) }}</p>
                <div class="reason-box">
                  <el-icon size="14"><InfoFilled /></el-icon>
                  <span class="reason-text">登录后可查看商品咨询与个性化推荐</span>
                </div>
                <div class="footer">
                  <span class="price">¥{{ item.price }}</span>
                  <span class="quality-pill">{{ getQualityTag(item) }}</span>
                </div>
                <p class="description muted">{{ item.description }}</p>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无推荐商品" />
        </template>

        <template v-else>
          <div v-if="recommendLoading" class="recommend-loading">
            <el-skeleton animated :rows="3" />
          </div>

          <el-alert
            v-else-if="recommendError"
            type="warning"
            :closable="false"
            title="推荐服务暂时不可用，先看看全部商品吧"
            show-icon
          />

          <div v-else-if="recommendProducts.length > 0" class="recommend-list">
            <article
              v-for="item in recommendProducts"
              :key="getCardKey(item)"
              class="recommend-item"
              @click="viewDetail(item)"
            >
              <div class="image-container">
                <img v-if="item.displayImage" :src="item.displayImage" alt="商品图片" />
                <div v-else class="recommend-placeholder">精选推荐</div>
                <span class="corner-tag">推荐</span>
              </div>
              <div class="content">
                <h3 class="title">{{ item.title }}</h3>
                <p class="location">{{ getTradeLocation(item) }}</p>
                <div class="reason-box">
                  <span class="reason-text">{{ getRecommendReason(item) }}</span>
                </div>
                <div class="footer">
                  <span class="price">¥{{ item.price }}</span>
                  <span class="quality-pill">{{ getQualityTag(item) }}</span>
                </div>
              </div>
            </article>
          </div>

          <el-empty v-else description="暂无推荐商品" />
        </template>
      </section>

      <div class="section-title">
        <span class="section-icon">
          <el-icon size="18"><ShoppingCart /></el-icon>
        </span>
        <span class="section-title-text">全部商品</span>
        <span class="section-tip">持续上新</span>
      </div>
      <div class="stream-hint">
        <span>在售 {{ onSaleCount }} 件</span>
        <span>·</span>
        <span>{{ keyword ? `“${keyword}”搜索结果 ${products.length} 件` : `当前可浏览 ${products.length} 件` }}</span>
      </div>

      <div v-if="products.length" class="all-products-grid">
        <article
          v-for="item in products"
          :key="getCardKey(item)"
          class="product-item"
          @click="viewDetail(item)"
        >
          <div class="image-container">
            <img v-if="item.displayImage" :src="item.displayImage" alt="商品图片" />
            <div v-else class="image-placeholder">暂无图片</div>
            <div class="card-badges">
              <span class="badge" :class="getStatusBadgeClass(item)">{{ getStatusLabel(item) }}</span>
              <span class="badge quality-badge">{{ getQualityTag(item) }}</span>
            </div>
          </div>
          <div class="content">
            <h3 class="title">{{ item.title }}</h3>
            <p class="location">{{ getTradeLocation(item) }}</p>
            <div class="footer">
              <span class="price">¥{{ item.price }}</span>
              <span class="selling-point">{{ getSellingPoint(item) }}</span>
            </div>
          </div>
        </article>
      </div>

      <div v-if="!products.length && !loading" class="empty-state">
        <el-empty description="暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProductList } from '../api/product'
import { getRecommendProducts, getRecommendDetails } from '../api/recommend'
import { AUTH_CHANGED_EVENT, getUserId } from '../utils/user'
import { Star, ShoppingCart, InfoFilled } from '@element-plus/icons-vue'
import {
  getProductStatusMeta,
  isProductOrderableStatus,
  normalizeProductResponseList,
} from '../utils/productNormalizer'

const router = useRouter()
const route = useRoute()
const products = ref([])
const recommendProducts = ref([])
const recommendExplainMap = reactive({})
const keyword = ref('')
const loading = ref(false)
const recommendLoading = ref(false)
const recommendError = ref(false)
const currentUserId = ref(getUserId())

const getProductId = (item) => {
  if (!item || typeof item !== 'object') return null
  return item.id ?? item.productId ?? item.goodsId ?? item.product_id ?? null
}

const getCardKey = (item) => {
  const id = getProductId(item)
  return id ?? `${item?.title || 'product'}-${item?.price || '0'}-${item?.displayImage || 'noimg'}`
}

const onSaleCount = computed(() => {
  return products.value.filter((item) => isProductOrderableStatus(item?.status)).length
})

const fallbackRecommendProducts = computed(() => products.value.slice(0, 6))

const fetchProducts = async (params = {}) => {
  loading.value = true
  try {
    const res = await getProductList(params)
    products.value = normalizeProductResponseList(res)
  } finally {
    loading.value = false
  }
}

const fetchRecommendations = async () => {
  currentUserId.value = getUserId()
  recommendProducts.value = []
  Object.keys(recommendExplainMap).forEach((key) => delete recommendExplainMap[key])
  recommendError.value = false

  if (!currentUserId.value) {
    return
  }

  recommendLoading.value = true
  try {
    const res = await getRecommendProducts(currentUserId.value, 6)
    recommendProducts.value = normalizeProductResponseList(res)

    if (recommendProducts.value.length > 0) {
      const productIds = recommendProducts.value.map((item) => getProductId(item)).filter(Boolean).join(',')
      if (productIds) {
        try {
          const explainRes = await getRecommendDetails(currentUserId.value, productIds)
          const explainData = explainRes?.data ?? explainRes
          if (explainData && typeof explainData === 'object' && !Array.isArray(explainData)) {
            Object.assign(recommendExplainMap, explainData)
          }
        } catch (error) {
          console.error('获取推荐解释失败:', error)
        }
      }
    }
  } catch (error) {
    recommendError.value = true
    console.error('获取推荐商品失败:', error)
  } finally {
    recommendLoading.value = false
  }
}

const getRecommendReason = (item) => {
  const productId = getProductId(item)
  if (!productId) return '根据你的历史行为为你推荐'
  return recommendExplainMap[productId] || recommendExplainMap[String(productId)] || '根据你的历史行为为你推荐'
}

const getQualityTag = (item) => {
  const text = `${item.title || ''} ${item.description || ''}`
  if (/全新|未拆|未使用/.test(text)) return '近全新'
  if (/95新|九五新|9成新/.test(text)) return '9成新'
  if (/8成新|七成新|旧/.test(text)) return '实用型'
  return '成色良好'
}

const getSellingPoint = (item) => {
  const num = Number(item.price)
  if (!Number.isNaN(num) && num <= 50) return '学生友好价'
  if (!Number.isNaN(num) && num <= 300) return '性价比不错'
  return '可小刀详聊'
}

const getStatusMeta = (item) => getProductStatusMeta(item?.status)

const getStatusLabel = (item) => getStatusMeta(item).label

const getStatusBadgeClass = (item) => {
  const type = getStatusMeta(item).type
  if (type === 'sold') return 'sold-badge'
  if (type === 'off-shelf') return 'off-shelf-badge'
  return 'sale-badge'
}

const getTradeLocation = (item) => {
  const location = item?.tradeLocation
  if (!location || !String(location).trim()) return '校内面交（地点待协商）'
  return location
}

const viewDetail = (item) => {
  const id = getProductId(item)
  if (!id) return
  router.push({ name: 'product-detail', params: { id } })
}

const syncAuthState = () => {
  currentUserId.value = getUserId()
  fetchRecommendations()
}

onMounted(() => {
  const initialKeyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  keyword.value = initialKeyword
  fetchProducts(initialKeyword ? { keyword: initialKeyword } : {})
  fetchRecommendations()
  window.addEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})

onUnmounted(() => {
  window.removeEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})

watch(
  () => route.query.keyword,
  (newKeyword) => {
    const nextKeyword = typeof newKeyword === 'string' ? newKeyword : ''
    if (nextKeyword === keyword.value) return
    keyword.value = nextKeyword
    fetchProducts(nextKeyword ? { keyword: nextKeyword } : {})
  }
)
</script>

<style scoped>
.product-list {
  width: min(100%, 1440px);
  margin: 0 auto;
  padding: 20px 22px 34px;
}


.product-grid { display: flex; flex-direction: column; gap: 24px; }

.recommend-section {
  border-radius: 20px;
  padding: 20px 18px 16px;
  background: linear-gradient(180deg, #fffefb 0%, #fff9ed 100%);
}

.recommend-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 16px; }
.section-header { display: flex; align-items: center; gap: 10px; }
.recommend-icon-wrap { width: 30px; height: 30px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #ffecb3 0%, #ffd976 100%); }
.section-main-title { margin: 0; font-size: 18px; font-weight: 700; color: #3f2f13; }
.recommend-subtitle { margin: 4px 0 0; font-size: 12px; color: #7f6638; }
.recommend-tag { font-size: 12px; font-weight: 700; color: #89611f; padding: 4px 10px; border-radius: 999px; background: #fff2cc; }
.recommend-loading { padding: 16px 8px; }

.recommend-list,
.all-products-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px 18px;
}

.recommend-item,
.product-item {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.recommend-item:hover,
.product-item:hover { transform: translateY(-2px); }

.image-container {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 14px;
  margin-bottom: 11px;
  background: #faf4e7;
}

.image-container img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.28s; }
.product-item:hover .image-container img,
.recommend-item:hover .image-container img { transform: scale(1.03); }

.recommend-placeholder,
.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff1c4 0%, #f5e3b0 100%);
  color: #8a6d2f;
  font-size: 14px;
  font-weight: 600;
}

.corner-tag { position: absolute; top: 9px; left: 9px; padding: 2px 8px; border-radius: 999px; background: rgba(55, 42, 13, 0.74); color: #ffe4a4; font-size: 11px; font-weight: 600; }
.card-badges { position: absolute; left: 8px; right: 8px; bottom: 8px; display: flex; justify-content: space-between; align-items: center; }
.badge { padding: 2px 8px; border-radius: 999px; font-size: 11px; color: #fff7e1; backdrop-filter: blur(2px); }
.sale-badge { background: rgba(40, 28, 5, 0.76); }
.sold-badge { background: rgba(75, 85, 99, 0.78); }
.off-shelf-badge { background: rgba(180, 83, 9, 0.78); }
.quality-badge { background: rgba(129, 88, 24, 0.76); }

.content { padding: 0 2px; }
.title {
  font-size: 15px;
  font-weight: 600;
  color: #302718;
  margin: 0 0 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
  min-height: 45px;
}
.description {
  font-size: 12px;
  color: #8f8779;
  margin: 0 0 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 36px;
  line-height: 1.5;
}
.description.muted {
  margin: 8px 0 0;
  color: #a59b8d;
  -webkit-line-clamp: 1;
  min-height: 18px;
}
.location {
  margin: 0 0 8px;
  font-size: 12px;
  color: #7b5b1e;
  line-height: 1.45;
  font-weight: 600;
}
.reason-box { display: flex; align-items: flex-start; gap: 6px; padding: 0; background: transparent; margin-bottom: 10px; }
.reason-text { font-size: 12px; color: #8a6d2f; line-height: 1.45; flex: 1; }
.footer { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.price { font-size: 24px; font-weight: 800; color: #eb5d3e; letter-spacing: 0.01em; line-height: 1; }
.quality-pill,
.selling-point { padding: 1px 0; font-size: 11px; color: #7f6330; white-space: nowrap; }

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 2px;
  background: transparent;
}
.section-title-text { font-size: 18px; font-weight: 700; color: #5c430e; }
.section-icon { width: 24px; height: 24px; border-radius: 7px; display: inline-flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #ffe49c 0%, #ffd875 100%); color: #8a6200; }
.section-tip { margin-left: auto; padding: 2px 9px; border-radius: 999px; font-size: 12px; color: #8a6d2f; background: #fff5d7; }
.stream-hint { margin-top: -8px; padding: 0 2px; display: flex; align-items: center; gap: 8px; font-size: 12px; color: #8d7650; }
.empty-state { padding: 58px 0; }

@media (max-width: 1024px) {
  .all-products-grid,
  .recommend-list { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px 14px; }
}

@media (max-width: 768px) {
  .product-list { padding: 12px; }
  .recommend-section { padding: 16px 12px; }
  .all-products-grid,
  .recommend-list { grid-template-columns: repeat(auto-fill, minmax(168px, 1fr)); gap: 16px 12px; }
  .stream-hint { font-size: 11px; gap: 6px; }
  .price { font-size: 22px; }
}
</style>
