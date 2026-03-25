<template>
  <div class="personal-center">
    <el-card class="user-card">
      <template #header>
        <div class="card-header">
          <el-icon size="24"><User /></el-icon>
          <span>我的主页</span>
        </div>
      </template>

      <div v-loading="loading" class="user-info">
        <section class="profile-header">
          <div class="profile-main">
            <el-avatar :size="70" :src="profileAvatar" />
            <div class="profile-text">
              <div class="name-row">
                <h2>{{ userInfo.username || '未命名用户' }}</h2>
                <el-tag :type="getCreditTagType(scoreForDisplay)">信用{{ creditLevelText }}</el-tag>
              </div>
              <p>用户编号：{{ userInfo.id || '-' }}</p>
              <p>注册时间：{{ formatTime(userInfo.createTime) }}</p>
            </div>
          </div>
          <div class="profile-actions">
            <el-button class="edit-profile-btn" type="primary" plain @click="openEditDialog">
              编辑资料
            </el-button>
            <el-button type="warning" plain @click="goToProducts">
              去逛商品广场
            </el-button>
          </div>
        </section>

        <section class="my-products-section">
          <div class="section-title">
            <div class="section-title-main">
              <el-icon><Goods /></el-icon>
              <span>我发布的商品</span>
            </div>
            <span class="section-tip">共 {{ myProducts.length }} 件</span>
          </div>

          <el-alert
            v-if="productsState === 'error'"
            :title="productsError || '我的商品加载失败'"
            type="error"
            :closable="false"
            class="credit-alert"
          />

          <div v-if="productsState === 'success' && myProducts.length" class="all-products-grid">
            <article
              v-for="item in myProducts"
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
                <p class="description">{{ item.description }}</p>
                <div class="footer">
                  <span class="price">¥{{ item.price }}</span>
                  <span class="selling-point">{{ getSellingPoint(item) }}</span>
                </div>
              </div>
            </article>
          </div>

          <el-empty
            v-else-if="productsState === 'success'"
            description="暂无数据"
          >
            <el-button type="primary" @click="router.push('/product/create')">发布商品</el-button>
          </el-empty>
        </section>

      </div>
    </el-card>

    <el-dialog v-model="editDialogVisible" title="编辑个人资料" width="460px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名" required>
          <el-input
            v-model="editForm.username"
            maxlength="30"
            show-word-limit
            placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item label="头像 URL">
          <el-input
            v-model="editForm.avatar"
            placeholder="https://example.com/avatar.png"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="submitProfile">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Goods } from '@element-plus/icons-vue'
import { updateMyProfile } from '../api/user'
import { getProductList } from '../api/product'
import { getCreditScore } from '../api/credit'
import {
  AUTH_CHANGED_EVENT,
  dispatchAuthChanged,
  getUserId,
  getUserInfo,
  removeUserInfo,
  setUserInfo
} from '../utils/user'
import { getToken, removeToken } from '../utils/request'
import { getProductStatusMeta, normalizeProductResponseList } from '../utils/productNormalizer'
import defaultAvatar from '../assets/default-avatar.svg'

const router = useRouter()
const loading = ref(false)
const creditScore = ref(null)
const myProducts = ref([])
const productsState = ref('idle')
const productsError = ref('')

const userInfo = ref({
  id: null,
  username: '',
  avatar: '',
  createTime: '',
  status: 1
})
const profileAvatar = computed(() => userInfo.value.avatar || defaultAvatar)
const editDialogVisible = ref(false)
const savingProfile = ref(false)
const editForm = ref({
  username: '',
  avatar: ''
})

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

const scoreForDisplay = computed(() => (creditScore.value == null ? 0 : Number(creditScore.value) || 0))

const creditLevelText = computed(() => {
  if (creditScore.value == null) return '-'
  if (scoreForDisplay.value >= 80) return '优秀'
  if (scoreForDisplay.value >= 60) return '良好'
  return '待提升'
})

const getApiData = (res) => (res && typeof res === 'object' && 'data' in res ? res.data : res)

const toNumber = (value, defaultValue = 0) => {
  const n = Number(value)
  return Number.isFinite(n) ? n : defaultValue
}

const normalizeCreditScore = (payload) => {
  const data = getApiData(payload)
  if (typeof data === 'number') return data
  if (typeof payload === 'number') return payload
  return (
    toNumber(data?.creditScore, NaN) ||
    toNumber(data?.score, NaN) ||
    toNumber(data?.credit, NaN) ||
    toNumber(data?.value, NaN) ||
    0
  )
}

const loadUserInfo = () => {
  const localUserInfo = getUserInfo()
  if (!localUserInfo) return
  userInfo.value = {
    id: localUserInfo.id ?? localUserInfo.userId ?? null,
    username: localUserInfo.username || '',
    avatar: localUserInfo.avatar || '',
    createTime: localUserInfo.createTime || '',
    status: localUserInfo.status ?? 1
  }
}

const openEditDialog = () => {
  editForm.value = {
    username: userInfo.value.username || '',
    avatar: userInfo.value.avatar || ''
  }
  editDialogVisible.value = true
}

const submitProfile = async () => {
  const username = editForm.value.username?.trim()
  const avatar = editForm.value.avatar?.trim() || ''
  if (!username) {
    ElMessage.warning('用户名不能为空')
    return
  }

  savingProfile.value = true
  try {
    const res = await updateMyProfile({ username, avatar })
    const latestUser = {
      ...userInfo.value,
      ...(res?.data || {}),
      username,
      avatar
    }
    userInfo.value = latestUser
    setUserInfo(latestUser)
    dispatchAuthChanged()
    editDialogVisible.value = false
    ElMessage.success('保存成功')
  } finally {
    savingProfile.value = false
  }
}

const fetchCreditScore = async (userId) => {
  const res = await getCreditScore({ userId })
  creditScore.value = normalizeCreditScore(res)
}

const normalizeMineProducts = (products, userId) => {
  if (!Array.isArray(products)) return []

  const ownFields = products.some((item) =>
    [item?.sellerId, item?.userId, item?.ownerId, item?.publisherId].some((id) => id !== null && id !== undefined && id !== '')
  )

  if (!ownFields) {
    return products
  }

  return products.filter((item) => {
    const ownerId = item?.sellerId ?? item?.userId ?? item?.ownerId ?? item?.publisherId ?? null
    return ownerId !== null && String(ownerId) === String(userId)
  })
}

const fetchMyProducts = async (userId) => {
  productsState.value = 'loading'
  productsError.value = ''
  const res = await getProductList({ userId, sellerId: userId })
  const normalized = normalizeProductResponseList(res)
  myProducts.value = normalizeMineProducts(normalized, userId)
  productsState.value = 'success'
}

const fetchPageData = async () => {
  const token = getToken()
  const userId = getUserId() ?? userInfo.value.id
  if (!token || !userId) {
    triggerLoginDialog('请先登录后再操作', '/profile')
    return
  }

  loading.value = true

  try {
    await fetchMyProducts(userId)
  } catch (error) {
    productsState.value = 'error'
    console.error('获取我的商品失败:', error)
    productsError.value = '加载失败，请刷新后重试'
    myProducts.value = []
  }

  try {
    await fetchCreditScore(userId)
  } catch {
    creditScore.value = null
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time !== 'string') return String(time)
  return time.replace('T', ' ')
}

const getCreditTagType = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

const getProductId = (item) => {
  if (!item || typeof item !== 'object') return null
  return item.id ?? item.productId ?? item.goodsId ?? item.product_id ?? null
}

const getCardKey = (item) => {
  const id = getProductId(item)
  return id ?? `${item?.title || 'product'}-${item?.price || '0'}-${item?.displayImage || 'noimg'}`
}

const getStatusMeta = (item) => getProductStatusMeta(item?.status)
const getStatusLabel = (item) => getStatusMeta(item).label

const getStatusBadgeClass = (item) => {
  const type = getStatusMeta(item).type
  if (type === 'sold') return 'sold-badge'
  if (type === 'off-shelf') return 'off-shelf-badge'
  return 'sale-badge'
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

const viewDetail = (item) => {
  const id = getProductId(item)
  if (!id) return
  router.push({ name: 'product-detail', params: { id } })
}

const goToProducts = () => {
  router.push('/products')
}

const handleLogout = () => {
  removeToken()
  removeUserInfo()
  dispatchAuthChanged()
  ElMessage.success('已退出登录')
  router.push('/products')
}

const syncAuthState = () => {
  loadUserInfo()
  fetchPageData()
}

onMounted(() => {
  loadUserInfo()
  fetchPageData()
  window.addEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})

onUnmounted(() => {
  window.removeEventListener(AUTH_CHANGED_EVENT, syncAuthState)
})
</script>

<style scoped>
.personal-center {
  max-width: 1320px;
  margin: 10px auto 26px;
  padding: 12px 8px 24px;
}

.user-card {
  border-radius: 16px;
  border: 1px solid #f0e3be;
  box-shadow: 0 10px 24px rgba(67, 53, 26, 0.08);
}

.user-card :deep(.el-card__header) {
  padding: 16px 24px;
}

.user-card :deep(.el-card__body) {
  padding: 22px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #4a3915;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #f0e3be;
  background: #fffdf6;
}

.profile-main {
  display: flex;
  gap: 14px;
  align-items: center;
}

.profile-text h2 {
  margin: 0;
  font-size: 24px;
  color: #3d3220;
}

.profile-text p {
  margin: 4px 0 0;
  color: #7c6b45;
  font-size: 14px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title-main {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #4a3915;
}

.section-tip {
  font-size: 13px;
  color: #8e7e56;
}

.my-products-section {
  border: 1px solid #f0e4c5;
  border-radius: 12px;
  padding: 16px;
}

.all-products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.product-item {
  border: 1px solid #f3e7cb;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  cursor: pointer;
  transition: all .2s ease;
}

.product-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(163, 132, 59, 0.16);
}

.image-container {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #f8f3e6;
  overflow: hidden;
}

.image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #9c8c64;
  font-size: 14px;
}

.card-badges {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  display: flex;
  gap: 8px;
}

.badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
  background: #67c23a;
}

.sale-badge { background: #67c23a; }
.sold-badge { background: #f56c6c; }
.off-shelf-badge { background: #909399; }
.quality-badge { background: rgba(61, 50, 32, 0.78); }

.content {
  padding: 12px;
}

.title {
  margin: 0;
  font-size: 15px;
  color: #2d2a23;
}

.description {
  margin: 6px 0 10px;
  color: #7f7768;
  font-size: 13px;
  line-height: 1.45;
  min-height: 38px;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #e36a00;
  font-size: 20px;
  font-weight: 700;
}

.selling-point {
  color: #9b7d38;
  font-size: 12px;
}

.credit-alert {
  margin-bottom: 10px;
}

@media (max-width: 992px) {
  .profile-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .page-hero h1 {
    font-size: 28px;
  }

  .all-products-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
</style>
