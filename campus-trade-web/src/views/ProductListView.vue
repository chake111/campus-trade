<template>
  <div class="product-list">
    <div class="header">
      <h1>校园二手交易</h1>
      <div class="controls">
        <el-input v-model="keyword" placeholder="搜索商品..." clearable @keyup.enter="searchProducts" style="width:300px">
          <template #append>
            <el-button type="primary" @click="searchProducts">搜索</el-button>
          </template>
        </el-input>
        <el-button type="success" @click="createProduct">发布商品</el-button>
      </div>
    </div>

    <div v-loading="loading" class="product-grid">
      <el-card class="recommend-section">
        <template #header>
          <div class="section-header">
            <el-icon size="20" color="#e6a23c"><Star /></el-icon>
            <span>智能推荐</span>
          </div>
        </template>

        <div v-if="recommendProducts.length > 0" class="recommend-list">
          <el-card
            v-for="item in recommendProducts"
            :key="item.id"
            class="recommend-card"
            @click="viewDetail(item.id)"
          >
            <div class="recommend-placeholder">推荐商品</div>
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
      </el-card>

      <div class="section-title">
        <el-icon size="20"><ShoppingCart /></el-icon>
        <span>全部商品</span>
      </div>

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

      <div v-if="!products.length && !loading" class="empty-state">
        <el-empty description="暂无商品" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductList } from '../api/product'
import { getSmartRecommend, getRecommendExplain } from '../api/recommend'
import { Star, ShoppingCart, InfoFilled } from '@element-plus/icons-vue'

const router = useRouter()
const products = ref([])
const recommendProducts = ref([])
const recommendExplainMap = reactive({})
const keyword = ref('')
const loading = ref(false)

const fetchProducts = async (params = {}) => {
  loading.value = true
  try {
    const res = await getProductList(params)
    products.value = res.data || []
  } finally {
    loading.value = false
  }
}

const fetchRecommendations = async () => {
  try {
    const res = await getSmartRecommend(4)
    recommendProducts.value = res.data || []

    if (recommendProducts.value.length > 0) {
      const productIds = recommendProducts.value.map(item => item.id).join(',')
      try {
        const explainRes = await getRecommendExplain(productIds)
        if (explainRes.data && typeof explainRes.data === 'object') {
          Object.assign(recommendExplainMap, explainRes.data)
        }
      } catch (error) {
        console.error('获取推荐解释失败:', error)
      }
    }
  } catch (error) {
    console.error('获取推荐商品失败:', error)
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

onMounted(() => {
  fetchProducts()
  fetchRecommendations()
})
</script>

<style scoped>
.product-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
}

.header h1 {
  font-size: 28px;
  margin: 0;
}

.controls {
  display: flex;
  gap: 15px;
  align-items: center;
}

.product-grid {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.recommend-section {
  border: 2px solid #e6a23c;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff9f0 0%, #fff 100%);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #e6a23c;
}

.recommend-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.recommend-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 8px;
}

.recommend-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.3);
}

.recommend-placeholder {
  height: 120px;
  background: linear-gradient(135deg, #ffd89b 0%, #e6a23c 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px 8px 0 0;
}

.content {
  padding: 10px 0;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.description {
  font-size: 14px;
  color: #909399;
  margin: 0 0 15px 0;
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
  padding: 10px;
  background: #fdf6ec;
  border-radius: 6px;
  margin-bottom: 12px;
  border-left: 3px solid #e6a23c;
}

.reason-text {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.5;
  flex: 1;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.product-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 12px;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.image-container {
  width: 100%;
  height: 200px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 15px;
}

.image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .image-container img {
  transform: scale(1.1);
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 14px;
}

.empty-state {
  padding: 60px 0;
}
</style>
