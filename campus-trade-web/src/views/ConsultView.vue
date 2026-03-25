<template>
  <div class="consult-page" v-loading="loading">
    <el-card shadow="never" class="consult-card">
      <div class="consult-layout">
        <aside class="session-list">
          <el-empty v-if="!sessions.length" description="暂无咨询会话" :image-size="70" />
          <el-scrollbar v-else max-height="520px">
            <button
              v-for="item in sessions"
              :key="item.id"
              class="session-item"
              :class="{ active: String(activeSessionId) === String(item.id) }"
              @click="selectSession(item.id)"
            >
              <el-avatar :size="44" :src="getSessionCounterpartAvatar(item)" class="session-avatar" />
              <div class="session-main">
                <div class="session-row session-row-top">
                  <div class="session-title">{{ getSessionCounterpartName(item) }}</div>
                  <el-tag
                    size="small"
                    effect="plain"
                    :type="getSessionStatusTag(item).type"
                    class="session-tag"
                  >
                    {{ getSessionStatusTag(item).text }}
                  </el-tag>
                </div>
                <div class="session-row session-last">{{ getSessionSubtitle(item) }}</div>
                <div class="session-row session-meta">{{ getSessionMeta(item) }}</div>
              </div>
              <div class="session-thumb-wrap">
                <img
                  v-if="item.productImage"
                  :src="item.productImage"
                  alt="商品缩略图"
                  class="session-thumb"
                />
                <div v-else class="session-thumb session-thumb-placeholder">无图</div>
              </div>
            </button>
          </el-scrollbar>
        </aside>

        <section class="message-panel">
          <el-empty v-if="!activeSessionId" description="请选择左侧会话" />
          <template v-else>
            <div
              v-if="activeSession"
              class="product-context"
              :class="{ 'is-clickable': !!getSessionProductId(activeSession) }"
              @click="goProductDetail(activeSession)"
            >
              <img
                v-if="activeSession.productImage"
                :src="activeSession.productImage"
                alt="商品图片"
                class="context-image"
              />
              <div v-else class="context-image context-image-placeholder">暂无图片</div>
              <div class="context-main">
                <div class="context-title">{{ activeSession.productTitle || `商品 #${activeSession.productId}` }}</div>
                <div class="context-meta">
                  <span class="context-price">¥{{ formatPrice(activeSession.productPrice) }}</span>
                  <span>状态：{{ formatProductStatus(activeSession.productStatus) }}</span>
                  <span>地点：{{ activeSession.tradeLocation || '校内面交（地点待协商）' }}</span>
                </div>
              </div>
            </div>

            <el-scrollbar ref="messageScrollbarRef" class="message-list">
              <div v-if="!messages.length" class="empty-message">暂无消息，开始咨询吧</div>
              <div
                v-for="msg in messages"
                :key="msg.id"
                class="message-row"
                :class="{ mine: String(msg.senderId) === String(currentUserId) }"
              >
                <div class="bubble">
                  <p>{{ msg.content }}</p>
                  <span>{{ formatTime(msg.createTime) }}</span>
                </div>
              </div>
            </el-scrollbar>

            <div class="composer">
              <el-input
                v-model="draft"
                type="textarea"
                :rows="2"
                maxlength="1000"
                show-word-limit
                placeholder="请输入咨询内容（纯文本）"
                @keyup.ctrl.enter="handleSend"
              />
              <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
            </div>
          </template>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ensureConsultSession, getConsultMessages, getMyConsultSessions, sendConsultMessage } from '../api/consult'
import { getUserById } from '../api/user'
import defaultAvatar from '../assets/default-avatar.svg'
import { getUserId } from '../utils/user'
import { getProductStatusMeta } from '../utils/productNormalizer'

const POLL_INTERVAL_MS = 4000

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const sending = ref(false)
const sessions = ref([])
const activeSessionId = ref(null)
const messages = ref([])
const draft = ref('')
const messageScrollbarRef = ref(null)
const currentUserId = ref(getUserId())
let pollingTimer = null

const activeSession = ref(null)
const counterpartUser = ref(null)
const userInfoCache = ref({})

const counterpartId = computed(() => {
  const session = activeSession.value
  if (!session || currentUserId.value == null) return null
  const isBuyer = String(session.buyerId) === String(currentUserId.value)
  if (isBuyer) return session.sellerId ?? null
  const isSeller = String(session.sellerId) === String(currentUserId.value)
  if (isSeller) return session.buyerId ?? null
  return null
})

const counterpartRoleLabel = computed(() => {
  const session = activeSession.value
  if (!session || currentUserId.value == null) return '沟通对象'
  if (String(session.buyerId) === String(currentUserId.value)) return '卖家'
  if (String(session.sellerId) === String(currentUserId.value)) return '买家'
  return '沟通对象'
})

const counterpartName = computed(() => {
  const name = counterpartUser.value?.username?.trim()
  if (name) return name
  return counterpartId.value ? `用户#${counterpartId.value}` : '未知用户'
})

const counterpartAvatar = computed(() => {
  const avatar = counterpartUser.value?.avatar
  if (avatar && String(avatar).trim()) return avatar
  return defaultAvatar
})

const goProducts = () => {
  router.push('/products')
}

const getSessionProductId = (session) => {
  const rawId = session?.productId
  if (rawId === null || rawId === undefined || rawId === '') return null
  const id = Number(rawId)
  if (!Number.isInteger(id) || id <= 0) return null
  return id
}

const goProductDetail = (session) => {
  const productId = getSessionProductId(session)
  if (!productId) return
  router.push({ name: 'product-detail', params: { id: productId } })
}

const formatTime = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

const loadSessions = async () => {
  const res = await getMyConsultSessions()
  sessions.value = Array.isArray(res?.data) ? res.data : []
  await loadSessionCounterpartUsers(sessions.value)
  activeSession.value =
    sessions.value.find((item) => String(item.id) === String(activeSessionId.value)) || null
}

const getSessionCounterpartId = (session) => {
  if (!session || currentUserId.value == null) return null
  if (String(session.buyerId) === String(currentUserId.value)) return session.sellerId ?? null
  if (String(session.sellerId) === String(currentUserId.value)) return session.buyerId ?? null
  return null
}

const loadSessionCounterpartUsers = async (sessionList) => {
  if (!Array.isArray(sessionList) || !sessionList.length) return
  const ids = [...new Set(sessionList.map((item) => getSessionCounterpartId(item)).filter(Boolean))]
  const pendingIds = ids.filter((id) => !userInfoCache.value[String(id)])
  if (!pendingIds.length) return

  await Promise.all(
    pendingIds.map(async (id) => {
      try {
        const res = await getUserById(id)
        userInfoCache.value[String(id)] = res?.data || null
      } catch (error) {
        userInfoCache.value[String(id)] = null
      }
    })
  )
}

const getSessionCounterpartUser = (session) => {
  const id = getSessionCounterpartId(session)
  if (!id) return null
  return userInfoCache.value[String(id)] || null
}

const getSessionCounterpartName = (session) => {
  const user = getSessionCounterpartUser(session)
  const name = user?.username?.trim()
  if (name) return name
  const id = getSessionCounterpartId(session)
  return id ? `用户#${id}` : '未知用户'
}

const getSessionCounterpartAvatar = (session) => {
  const user = getSessionCounterpartUser(session)
  const avatar = user?.avatar
  if (avatar && String(avatar).trim()) return avatar
  return defaultAvatar
}

const getSessionStatusTag = (session) => {
  const productMeta = getProductStatusMeta(session?.productStatus)
  const hasLastMessage = Boolean(session?.lastMessage && String(session.lastMessage).trim())

  if (productMeta.type === 'sold' || productMeta.type === 'off-shelf') {
    return { text: '已完成', type: 'success' }
  }
  if (hasLastMessage) {
    return { text: '咨询中', type: 'warning' }
  }
  return { text: '待沟通', type: 'info' }
}

const getSessionSubtitle = (session) => {
  const last = session?.lastMessage
  if (last && String(last).trim()) return last
  return '你们正在沟通这件商品，继续沟通吧'
}

const getSessionMeta = (session) => {
  if (session?.lastMessageTime) return formatTime(session.lastMessageTime)
  return session?.productTitle || `商品 #${session?.productId ?? '--'}`
}

const scrollToBottom = async () => {
  await nextTick()
  const scrollbar = messageScrollbarRef.value
  if (!scrollbar) return
  scrollbar.setScrollTop?.(Number.MAX_SAFE_INTEGER)
}

const loadMessages = async () => {
  if (!activeSessionId.value) {
    messages.value = []
    return
  }
  const res = await getConsultMessages(activeSessionId.value)
  messages.value = Array.isArray(res?.data) ? res.data : []
  scrollToBottom()
}

const startPolling = () => {
  stopPolling()
  pollingTimer = window.setInterval(() => {
    loadMessages().catch(() => {})
    loadSessions().catch(() => {})
  }, POLL_INTERVAL_MS)
}

const stopPolling = () => {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const selectSession = async (sessionId) => {
  activeSessionId.value = sessionId
  activeSession.value =
    sessions.value.find((item) => String(item.id) === String(activeSessionId.value)) || null
  await loadCounterpartUser()
  await loadMessages()
  startPolling()
}

const loadCounterpartUser = async () => {
  const id = counterpartId.value
  if (!id) {
    counterpartUser.value = null
    return
  }
  const cacheKey = String(id)
  if (userInfoCache.value[cacheKey]) {
    counterpartUser.value = userInfoCache.value[cacheKey]
    return
  }
  try {
    const res = await getUserById(id)
    const user = res?.data || null
    userInfoCache.value[cacheKey] = user
    counterpartUser.value = user
  } catch (error) {
    counterpartUser.value = null
  }
}

const ensureByRouteProduct = async () => {
  const productId = route.query.productId
  const counterpartId = route.query.counterpartId
  if (!productId) return null
  const res = await ensureConsultSession(productId, counterpartId)
  return res?.data?.id || null
}

const formatPrice = (value) => {
  const price = Number(value)
  if (Number.isNaN(price)) return '--'
  return price.toFixed(2)
}

const formatProductStatus = (status) => {
  return getProductStatusMeta(status).label
}

const handleSend = async () => {
  if (!activeSessionId.value) {
    ElMessage.warning('请先选择会话')
    return
  }

  const content = draft.value.trim()
  if (!content) {
    ElMessage.warning('消息内容不能为空')
    return
  }

  sending.value = true
  try {
    await sendConsultMessage(activeSessionId.value, content)
    draft.value = ''
    await loadMessages()
    await loadSessions()
  } finally {
    sending.value = false
  }
}

const initPage = async () => {
  loading.value = true
  try {
    const ensuredSessionId = await ensureByRouteProduct()
    await loadSessions()

    const initialSessionId =
      ensuredSessionId || (sessions.value.length ? sessions.value[0].id : null)

    if (initialSessionId) {
      await selectSession(initialSessionId)
    }
  } catch (error) {
    console.error('加载咨询页面失败:', error)
    ElMessage.error('加载失败，请刷新后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initPage()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.consult-page {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 16px;
}

.consult-card {
  border-radius: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-row h2 {
  margin: 0;
}

.header-row p {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.consult-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  min-height: 580px;
}

.session-list {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px;
}

.section-title {
  font-weight: 600;
  margin-bottom: 10px;
  padding: 0 4px;
}

.session-item {
  width: 100%;
  text-align: left;
  border: 0px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 10px;
  padding: 10px;
  cursor: pointer;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 10px;
}

.session-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.session-title {
  font-weight: 600;
  color: #303133;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-last {
  margin-top: 4px;
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-main {
  min-width: 0;
}

.session-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.session-row-top {
  justify-content: space-between;
}

.session-tag {
  flex-shrink: 0;
}

.session-avatar {
  flex-shrink: 0;
}

.session-thumb-wrap {
  width: 50px;
  height: 50px;
  flex-shrink: 0;
}

.session-thumb {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid #ebeef5;
}

.session-thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #909399;
  background: #f5f7fa;
}

.message-panel {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px;
  display: flex;
  flex-direction: column;
}

.counterpart-header {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  background: #fff;
}

.counterpart-main {
  min-width: 0;
}

.counterpart-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.counterpart-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-context {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.product-context.is-clickable {
  cursor: pointer;
}

.product-context.is-clickable:hover {
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.12);
  transform: translateY(-1px);
}

.context-image {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #ebeef5;
}

.context-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
}

.context-main {
  min-width: 0;
}

.context-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.context-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #606266;
  font-size: 13px;
}

.context-price {
  color: #f56c6c;
  font-weight: 600;
}

.message-list {
  flex: 1;
  min-height: 450px;
  max-height: 470px;
  padding: 6px;
}

.empty-message {
  color: #909399;
  text-align: center;
  margin-top: 120px;
}

.message-row {
  display: flex;
  margin-bottom: 10px;
}

.message-row.mine {
  justify-content: flex-end;
}

.bubble {
  max-width: 72%;
  background: #f5f7fa;
  border-radius: 10px;
  padding: 8px 12px;
}

.message-row.mine .bubble {
  background: #e1f3d8;
}

.bubble p {
  margin: 0;
  color: #303133;
  line-height: 1.4;
  white-space: pre-wrap;
}

.bubble span {
  display: block;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.composer {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: end;
}

@media (max-width: 992px) {
  .consult-layout {
    grid-template-columns: 1fr;
  }

  .message-list {
    max-height: 380px;
  }
}
</style>
