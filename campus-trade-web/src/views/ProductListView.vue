<template>
  <div class="product-list">
    <section class="trade-hero">
      <div class="hero-main">
        <p class="hero-badge">CAMPUS MARKET</p>
        <h1>校园二手交易广场</h1>
        <p class="hero-subtitle">在校内快速搜索好物、浏览最新挂售、一键发布你的闲置</p>
        <div class="hero-entrance-tags">
          <span class="entry-tag">可搜索</span>
          <span class="entry-tag">可浏览</span>
          <span class="entry-tag">可发布</span>
        </div>
      </div>

      <div class="hero-actions">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索教材、数码、日用..."
          clearable
          @keyup.enter="searchProducts"
        >
          <template #prepend>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="warning" @click="searchProducts">搜索商品</el-button>
          </template>
        </el-input>
        <el-button v-if="isLoggedIn" class="publish-btn" @click="createProduct">+ 发布闲置</el-button>
        <el-button v-else class="publish-btn" @click="goLogin">登录后发布闲置</el-button>
      </div>

      <div class="hero-meta">
        <span class="meta-item">
          <b>{{ products.length }}</b>
          在售商品
        </span>
        <span class="meta-item">
          <b>{{ recommendProducts.length }}</b>
          今日推荐
        </span>
        <span class="meta-item">校园内可当面交易</span>
      </div>
    </section>

    <div v-loading="loading" class="product-grid">
      <el-card class="recommend-section">
        <template #header>
          <div class="recommend-head">
            <div class="section-header">
              <span class="recommend-icon-wrap"><el-icon size="18" color="#d99a00"><Star /></el-icon></span>
              <div>
                <p class="section-main-title">运营精选推荐</p>
                <p class="recommend-subtitle">结合你的浏览 / 点赞 / 购买偏好，优先展示更可能感兴趣的商品</p>
              </div>
            </div>
            <span class="recommend-tag">平台精选</span>
          </div>
        </template>

        <template v-if="!currentUserId">
          <div v-if="fallbackRecommendProducts.length > 0" class="recommend-list">
            <el-card
              v-for="item in fallbackRecommendProducts"
              :key="`fallback-${getCardKey(item)}`"
              class="recommend-card"
              @click="viewDetail(item)"
            >
              <div class="image-container">
                <img v-if="item.image" :src="item.image" alt="商品图片" />
                <div v-else class="recommend-placeholder">精选推荐</div>
                <span class="corner-tag">推荐</span>
              </div>
              <div class="content">
                <h3 class="title">{{ item.title }}</h3>
                <p class="description">{{ item.description }}</p>
                <div class="reason-box">
                  <el-icon size="14"><InfoFilled /></el-icon>
                  <span class="reason-text">登录后可查看个性化推荐</span>
                </div>
                <div class="footer">
                  <span class="price">¥{{ item.price }}</span>
                  <span class="quality-pill">{{ getQualityTag(item) }}</span>
                </div>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="登录后查看个性化推荐，当前暂无可展示商品" />
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
            <el-card
              v-for="item in recommendProducts"
              :key="getCardKey(item)"
              class="recommend-card"
              @click="viewDetail(item)"
            >
              <div class="image-container">
                <img v-if="item.image" :src="item.image" alt="商品图片" />
                <div v-else class="recommend-placeholder">精选推荐</div>
                <span class="corner-tag">推荐</span>
              </div>
              <div class="content">
                <h3 class="title">{{ item.title }}</h3>
                <p class="description">{{ item.description }}</p>
                <div class="reason-box">
                  <el-icon size="14"><InfoFilled /></el-icon>
                  <span class="reason-text">{{ getRecommendReason(item) }}</span>
                </div>
                <div class="footer">
                  <span class="price">¥{{ item.price }}</span>
                  <span class="quality-pill">{{ getQualityTag(item) }}</span>
                </div>
              </div>
            </el-card>
          </div>

          <el-empty v-else description="暂无推荐商品" />
        </template>
      </el-card>

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
        <el-card
          v-for="item in products"
          :key="getCardKey(item)"
          class="product-card"
          @click="viewDetail(item)"
        >
          <div class="image-container">
            <img v-if="item.image" :src="item.image" alt="商品图片" />
            <div v-else class="image-placeholder">暂无图片</div>
            <div class="card-badges">
              <span class="badge sale-badge">在售</span>
              <span class="badge quality-badge">{{ getQualityTag(item) }}</span>
            </div>
          </div>
          <div class="content">
            <h3 class="title">{{ item.title }}</h3>
            <p class="description">{{ item.description }}</p>
            <div class="footer">
              <span class="price">¥{{ item.price }}</span>
              <span class="selling-point">{{ getSellingPoint(item) }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="!products.length && !loading" class="empty-state">
        <el-empty description="暂无商品" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductList } from '../api/product'
import { getRecommendProducts, getRecommendDetails } from '../api/recommend'
import { getToken } from '../utils/request'
import { AUTH_CHANGED_EVENT, getUserId } from '../utils/user'
import { Star, ShoppingCart, InfoFilled, Search } from '@element-plus/icons-vue'

const router = useRouter()
const products = ref([])
const recommendProducts = ref([])
const recommendExplainMap = reactive({})
const keyword = ref('')
const loading = ref(false)
const recommendLoading = ref(false)
const recommendError = ref(false)
const currentUserId = ref(getUserId())
const isLoggedIn = ref(Boolean(getToken() && currentUserId.value))
const SOLD_STATUS = ['SOLD', 'FINISHED', '已售出', 'OFF_SHELF', 'DISABLED', '已下架']

const getProductId = (item) => {
  if (!item || typeof item !== 'object') return null
  return item.id ?? item.productId ?? item.goodsId ?? item.product_id ?? null
}

const getCardKey = (item) => {
  const id = getProductId(item)
  return id ?? `${item?.title || 'product'}-${item?.price || '0'}-${item?.image || 'noimg'}`
}

const onSaleCount = computed(() => {
  return products.value.filter((item) => {
    const status = String(item?.status ?? '').toUpperCase()
    return !SOLD_STATUS.includes(status)
  }).length
})

const fallbackRecommendProducts = computed(() => products.value.slice(0, 6))

const normalizeList = (res) => {
  if (Array.isArray(res?.data)) return res.data
  if (Array.isArray(res)) return res
  return []
}

const normalizeProduct = (item) => ({
  ...item,
  id: getProductId(item),
  title: item.title ?? item.name ?? '未命名商品',
  description: item.description ?? item.desc ?? '暂无描述',
  price: item.price ?? 0,
  image: item.image ?? item.imageUrl ?? item.cover ?? ''
})

const fetchProducts = async (params = {}) => {
  loading.value = true
  try {
    const res = await getProductList(params)
    products.value = normalizeList(res).map(normalizeProduct)
  } finally {
    loading.value = false
  }
}

const fetchRecommendations = async () => {
  currentUserId.value = getUserId()
  isLoggedIn.value = Boolean(getToken() && currentUserId.value)
  recommendProducts.value = []
  Object.keys(recommendExplainMap).forEach((key) => delete recommendExplainMap[key])
  recommendError.value = false

  if (!currentUserId.value) {
    return
  }

  recommendLoading.value = true
  try {
    const res = await getRecommendProducts(currentUserId.value, 6)
    recommendProducts.value = normalizeList(res).map(normalizeProduct)

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

const searchProducts = () => {
  fetchProducts({ keyword: keyword.value })
}

const viewDetail = (item) => {
  const id = getProductId(item)
  if (!id) return
  router.push({ name: 'product-detail', params: { id } })
}

const createProduct = () => {
  router.push('/product/create')
}

const goLogin = () => {
  router.push('/login')
}

const syncAuthState = () => {
  currentUserId.value = getUserId()
  isLoggedIn.value = Boolean(getToken() && currentUserId.value)
  fetchRecommendations()
}

onMounted(() => {
  fetchProducts()
  fetchRecommendations()
  window.addEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})

onUnmounted(() => {
  window.removeEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})
</script>

<style scoped>
.product-list {
  width: min(100%, 1440px);
  margin: 0 auto;
  padding: 16px 20px 26px;
}

.trade-hero {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 16px;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px 22px;
  border-radius: 18px;
  border: 1px solid #efddad;
  background: linear-gradient(125deg, #fffdf4 0%, #fff8e3 58%, #fff4d3 100%);
  box-shadow: 0 14px 26px rgba(117, 91, 45, 0.08);
}

.hero-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-badge {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: #a68343;
  font-weight: 700;
}

.trade-hero h1 {
  font-size: clamp(28px, 2.6vw, 34px);
  margin: 0;
  line-height: 1.18;
  color: #31260f;
}

.hero-subtitle {
  margin: 0;
  font-size: 14px;
  color: #735b33;
}

.hero-entrance-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.entry-tag {
  padding: 4px 10px;
  font-size: 12px;
  color: #775013;
  border-radius: 999px;
  border: 1px solid #f0d59a;
  background: #fff7dd;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
}

.search-input {
  flex: 1 1 360px;
  min-width: 250px;
  max-width: 540px;
}

.search-input :deep(.el-input-group__prepend) {
  color: #8b6c30;
  border-radius: 10px 0 0 10px;
  background: #fff8e7;
  box-shadow: 0 0 0 1px #efd8a3 inset;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #efd8a3 inset;
  min-height: 44px;
}

.search-input :deep(.el-input-group__append .el-button) {
  min-width: 92px;
  min-height: 44px;
  border-radius: 0 10px 10px 0;
  background: #ffd768;
  border-color: #ffd768;
  color: #4a390f;
  font-weight: 600;
}

.search-input :deep(.el-input-group__append .el-button:hover) {
  background: #ffca33;
  border-color: #ffca33;
}

.publish-btn {
  min-height: 44px;
  min-width: 138px;
  color: #5e3b00;
  background: linear-gradient(135deg, #ffe59d 0%, #ffd167 100%);
  border: 1px solid #f2bf4c;
  font-weight: 700;
  box-shadow: 0 6px 14px rgba(242, 180, 57, 0.26);
}

.publish-btn:hover {
  color: #4b2f00;
  border-color: #e0a92f;
  background: linear-gradient(135deg, #ffdc81 0%, #ffc94d 100%);
  transform: translateY(-1px);
}

.hero-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  grid-column: 1 / -1;
}

.meta-item {
  font-size: 12px;
  color: #705627;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 251, 238, 0.85);
  border: 1px solid #f0dfb3;
}

.meta-item b {
  margin-right: 3px;
  color: #4a360e;
}

.product-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.recommend-section {
  border: 1px solid #eadfc5;
  border-radius: 14px;
  background: linear-gradient(180deg, #fffefb 0%, #fff9ed 100%);
  box-shadow: 0 10px 24px rgba(81, 64, 32, 0.07);
}

.recommend-section :deep(.el-card__header) {
  padding-top: 20px;
  padding-bottom: 16px;
}

.recommend-section :deep(.el-card__body) {
  padding-top: 16px;
  padding-bottom: 20px;
}

.recommend-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.recommend-icon-wrap {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffecb3 0%, #ffd976 100%);
}

.section-main-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #3f2f13;
}

.recommend-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #7f6638;
}

.recommend-tag {
  font-size: 12px;
  font-weight: 700;
  color: #89611f;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff2cc;
  border: 1px solid #f2dd9b;
}

.recommend-loading {
  padding: 16px 8px;
}

.recommend-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.recommend-card {
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  border-radius: 12px;
  border: 1px solid #f2ebde;
  background: #fffdf8;
  box-shadow: none;
  --el-card-box-shadow: none;
}

.recommend-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(82, 74, 57, 0.08);
  border-color: #ead9b2;
}

.recommend-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #fff1c4 0%, #f5e3b0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a6d2f;
  font-size: 15px;
  font-weight: 600;
}

.corner-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(55, 42, 13, 0.8);
  color: #ffe4a4;
  font-size: 11px;
  font-weight: 600;
}

.content {
  padding: 6px 2px 2px;
}

.title {
  font-size: 15px;
  font-weight: 600;
  color: #302718;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.description {
  font-size: 12px;
  color: #8f8779;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.reason-box {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 7px;
  background: #fff9ee;
  border-radius: 7px;
  margin-bottom: 9px;
  border-left: 3px solid #f2c253;
}

.reason-text {
  font-size: 12px;
  color: #8a6d2f;
  line-height: 1.45;
  flex: 1;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.price {
  font-size: 24px;
  font-weight: 800;
  color: #eb5d3e;
  letter-spacing: 0.01em;
  line-height: 1;
}

.quality-pill,
.selling-point {
  padding: 2px 8px;
  font-size: 11px;
  color: #7f6330;
  border-radius: 999px;
  border: 1px solid #efd7a4;
  background: #fff7e1;
  white-space: nowrap;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #fffdf7 0%, #fff7df 100%);
  border: 1px solid #f2e2ba;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(103, 81, 40, 0.05);
}

.section-title-text {
  font-size: 17px;
  font-weight: 700;
  color: #5c430e;
}

.section-icon {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffe49c 0%, #ffd875 100%);
  color: #8a6200;
}

.section-tip {
  margin-left: auto;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  color: #8a6d2f;
  background: #fff5d7;
  border: 1px solid #f4ddb1;
}

.stream-hint {
  margin-top: -4px;
  padding: 2px 4px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #8d7650;
}

.all-products-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  border-radius: 14px;
  border: 1px solid #f3ebda;
  box-shadow: 0 1px 3px rgba(67, 49, 16, 0.04);
  --el-card-box-shadow: none;
  background: linear-gradient(180deg, #ffffff 0%, #fffcf5 100%);
}

.product-card:hover {
  transform: translateY(-2px);
  border-color: #e9d4a2;
  box-shadow: 0 6px 14px rgba(82, 63, 27, 0.09);
}

.image-container {
  position: relative;
  width: 100%;
  height: 172px;
  overflow: hidden;
  border-radius: 11px;
  margin-bottom: 10px;
  background: #faf4e7;
  border: 1px solid #f5edde;
}

.image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.28s;
}

.product-card:hover .image-container img,
.recommend-card:hover .image-container img {
  transform: scale(1.04);
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fdf7e8 0%, #f7edd7 100%);
  color: #ae9162;
  font-size: 14px;
  font-weight: 500;
}

.card-badges {
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff7e1;
  backdrop-filter: blur(2px);
}

.sale-badge {
  background: rgba(40, 28, 5, 0.76);
}

.quality-badge {
  background: rgba(129, 88, 24, 0.76);
}

.all-products-grid .content {
  padding: 1px 4px 4px;
}

.empty-state {
  padding: 58px 0;
}

@media (max-width: 1024px) {
  .trade-hero {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .hero-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .search-input {
    max-width: 100%;
  }

  .all-products-grid,
  .recommend-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }
}

@media (max-width: 768px) {
  .product-list {
    padding: 12px;
  }

  .trade-hero {
    padding: 16px;
    gap: 12px;
    margin-bottom: 20px;
  }

  .hero-meta {
    width: 100%;
  }

  .search-input {
    min-width: 100%;
  }

  .all-products-grid,
  .recommend-list {
    grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
    gap: 10px;
  }

  .stream-hint {
    padding: 0 2px;
    font-size: 11px;
    gap: 6px;
  }

  .image-container {
    height: 150px;
  }

  .price {
    font-size: 22px;
  }
}
</style>
