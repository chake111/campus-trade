<template>
  <div class="product-list">
    <div class="header">
      <div class="hero-copy">
        <p class="hero-badge">CAMPUS MARKET</p>
        <h1>校园二手交易</h1>
        <p class="hero-subtitle">发现校内好物，轻松发布闲置</p>
      </div>
      <div class="controls">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索教材、数码、日用..."
          clearable
          @keyup.enter="searchProducts"
        >
          <template #append>
            <el-button type="warning" @click="searchProducts">搜索</el-button>
          </template>
        </el-input>
        <el-button v-if="isLoggedIn" class="publish-btn" @click="createProduct">发布闲置</el-button>
        <el-button v-else class="publish-btn" @click="goLogin">登录后发布闲置</el-button>
      </div>
      <div class="hero-meta">
        <span class="meta-item">当前在售 {{ products.length }} 件</span>
        <span class="meta-item">精选推荐 {{ recommendProducts.length }} 件</span>
      </div>
    </div>

    <div v-loading="loading" class="product-grid">
      <el-card class="recommend-section">
        <template #header>
          <div class="recommend-head">
            <div class="section-header">
              <el-icon size="20" color="#d99a00"><Star /></el-icon>
              <span>为你推荐</span>
            </div>
            <span class="recommend-tag">精选推荐</span>
          </div>
          <div class="recommend-subtitle">根据你的浏览 / 点赞 / 购买行为动态推荐</div>
        </template>

        <el-empty v-if="!currentUserId" description="登录后查看个性化推荐" />

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
              :key="item.id"
              class="recommend-card"
              @click="viewDetail(item.id)"
            >
              <div class="image-container">
                <img v-if="item.image" :src="item.image" alt="商品图片" />
                <div v-else class="recommend-placeholder">推荐商品</div>
              </div>
              <div class="content">
                <h3 class="title">{{ item.title }}</h3>
                <p class="description">{{ item.description }}</p>
                <div class="reason-box">
                  <el-icon size="14"><InfoFilled /></el-icon>
                  <span class="reason-text">{{ getRecommendReason(item.id) }}</span>
                </div>
                <div class="footer">
                  <span class="price">¥{{ item.price }}</span>
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
        <span>全部商品</span>
        <span class="section-tip">新鲜上架</span>
      </div>

      <div class="all-products-grid" v-if="products.length">
        <el-card
          v-for="item in products"
          :key="item.id"
          class="product-card"
          @click="viewDetail(item.id)"
        >
          <div class="image-container">
            <img v-if="item.image" :src="item.image" alt="商品图片" />
            <div v-else class="image-placeholder">暂无图片</div>
          </div>
          <div class="content">
            <h3 class="title">{{ item.title }}</h3>
            <p class="description">{{ item.description }}</p>
            <div class="footer">
              <span class="price">¥{{ item.price }}</span>
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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductList } from '../api/product'
import { getRecommendProducts, getRecommendDetails } from '../api/recommend'
import { getToken } from '../utils/request'
import { AUTH_CHANGED_EVENT, getUserId } from '../utils/user'
import { Star, ShoppingCart, InfoFilled } from '@element-plus/icons-vue'

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

const normalizeList = (res) => {
  if (Array.isArray(res?.data)) return res.data
  if (Array.isArray(res)) return res
  return []
}

const normalizeProduct = (item) => ({
  ...item,
  id: item.id,
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
      const productIds = recommendProducts.value.map((item) => item.id).join(',')
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
  } catch (error) {
    recommendError.value = true
    console.error('获取推荐商品失败:', error)
  } finally {
    recommendLoading.value = false
  }
}

const getRecommendReason = (productId) => {
  return recommendExplainMap[productId] || '根据你的历史行为为你推荐'
}

const searchProducts = () => {
  fetchProducts({ keyword: keyword.value })
}

const viewDetail = (id) => {
  router.push(`/product/${id}`)
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
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 14px 20px;
  margin-bottom: 28px;
  padding: 22px 24px 18px;
  background: linear-gradient(125deg, #fffdf4 0%, #fff8e3 48%, #fff5d8 100%);
  border: 1px solid #efddad;
  border-radius: 16px;
  color: #3c3c3c;
  box-shadow: 0 14px 28px rgba(117, 91, 45, 0.08);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hero-badge {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: #a68343;
  font-weight: 700;
}

.header h1 {
  font-size: clamp(27px, 2.6vw, 34px);
  margin: 0;
  line-height: 1.2;
  color: #31260f;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.6);
}

.hero-subtitle {
  margin: 2px 0 0;
  font-size: 14px;
  color: #7b6540;
}

.hero-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-item {
  font-size: 12px;
  color: #7d6844;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 251, 238, 0.82);
  border: 1px solid #f0dfb3;
}

.controls {
  display: flex;
  gap: 12px;
  align-items: center;
  flex: 1 1 500px;
  min-width: 320px;
  justify-content: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1 1 360px;
  min-width: 240px;
  max-width: 500px;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #efd8a3 inset;
  border-radius: 10px 0 0 10px;
  min-height: 42px;
}

.search-input :deep(.el-input__inner::placeholder) {
  color: #b09a73;
}

.controls .el-button {
  min-width: 108px;
  min-height: 42px;
  border-radius: 10px;
}

.controls :deep(.el-input-group__append .el-button) {
  min-width: 72px;
  border-radius: 0 10px 10px 0;
  background: #ffd768;
  border-color: #ffd768;
  color: #4a390f;
  font-weight: 600;
}

.controls :deep(.el-input-group__append .el-button:hover) {
  background: #ffca33;
  border-color: #ffca33;
}

.publish-btn {
  min-width: 126px;
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
  box-shadow: 0 8px 16px rgba(226, 166, 45, 0.3);
}

.publish-btn:active {
  transform: translateY(0);
  box-shadow: 0 4px 10px rgba(226, 166, 45, 0.24);
}

.product-grid {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.recommend-section {
  border: 1px solid #eadfc5;
  border-radius: 14px;
  background: linear-gradient(180deg, #fffefb 0%, #fffaf0 100%);
  box-shadow: 0 10px 24px rgba(81, 64, 32, 0.07);
}

.recommend-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #3c3c3c;
}

.recommend-tag {
  font-size: 12px;
  font-weight: 600;
  color: #89611f;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff2cc;
  border: 1px solid #f2dd9b;
}

.recommend-subtitle {
  display: inline-block;
  margin-top: 10px;
  padding: 5px 12px;
  font-size: 12px;
  color: #7f6638;
  background: #fff6dc;
  border-radius: 999px;
}

.recommend-loading {
  padding: 16px 8px;
}

.recommend-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.recommend-card {
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
  border-radius: 12px;
  border: 1px solid #efeadb;
  background: #fffdf8;
}

.recommend-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 22px rgba(82, 74, 57, 0.12);
  border-color: #ecd392;
}

.recommend-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #fff1c4 0%, #f5e3b0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a6d2f;
  font-size: 16px;
  font-weight: 600;
}

.content {
  padding: 6px 2px 2px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #302718;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.description {
  font-size: 13px;
  color: #948d80;
  margin: 0 0 10px 0;
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
  padding: 8px;
  background: #fffaf0;
  border-radius: 6px;
  margin-bottom: 10px;
  border-left: 3px solid #f2c253;
}

.reason-text {
  font-size: 12px;
  color: #8a6d2f;
  line-height: 1.5;
  flex: 1;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  font-size: 21px;
  font-weight: 700;
  color: #eb5d3e;
  letter-spacing: 0.01em;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #5c430e;
  margin-top: 12px;
  padding: 13px 16px;
  background: linear-gradient(135deg, #fffdf7 0%, #fff7df 100%);
  border: 1px solid #f2e2ba;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(103, 81, 40, 0.05);
}

.section-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffe49c 0%, #ffd875 100%);
  color: #8a6200;
}

.section-tip {
  margin-left: auto;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: #8a6d2f;
  background: #fff5d7;
  border: 1px solid #f4ddb1;
}

.all-products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
  border-radius: 16px;
  border: 1px solid #f1e9d6;
  box-shadow: 0 6px 16px rgba(67, 49, 16, 0.06);
  background: linear-gradient(180deg, #ffffff 0%, #fffcf5 100%);
}

.product-card:hover {
  transform: translateY(-3px);
  border-color: #efd08b;
  box-shadow: 0 12px 22px rgba(82, 63, 27, 0.13);
}

.image-container {
  width: 100%;
  height: 186px;
  overflow: hidden;
  border-radius: 12px;
  margin-bottom: 12px;
  background: #faf4e7;
  border: 1px solid #f4e9d1;
}

.image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .image-container img {
  transform: scale(1.05);
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

.all-products-grid .content {
  padding: 2px 4px 4px;
}

.all-products-grid .title {
  color: #3d3526;
  margin-bottom: 8px;
  line-height: 1.45;
}

.all-products-grid .description {
  color: #8c8476;
  margin-bottom: 12px;
  line-height: 1.5;
}

.all-products-grid .price {
  color: #ef5f4e;
}

.empty-state {
  padding: 60px 0;
}

@media (max-width: 768px) {
  .product-list {
    padding: 12px;
  }

  .header {
    padding: 16px;
    margin-bottom: 20px;
    align-items: flex-start;
  }

  .controls {
    min-width: 100%;
    justify-content: stretch;
    gap: 8px;
  }

  .hero-meta {
    width: 100%;
  }

  .search-input {
    min-width: 100%;
    max-width: 100%;
  }

  .all-products-grid,
  .recommend-list {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 14px;
  }
}
</style>
