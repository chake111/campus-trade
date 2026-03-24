<template>
  <div class="consult-page" v-loading="loading">
    <el-card shadow="never" class="consult-card">
      <template #header>
        <div class="header-row">
          <div>
            <h2>商品咨询</h2>
            <p>围绕具体商品与买/卖家沟通（轮询刷新）</p>
          </div>
          <el-button text @click="goProducts">返回商品列表</el-button>
        </div>
      </template>

      <div class="consult-layout">
        <aside class="session-list">
          <div class="section-title">我的会话</div>
          <el-empty v-if="!sessions.length" description="暂无咨询会话" :image-size="70" />
          <el-scrollbar v-else max-height="520px">
            <button
              v-for="item in sessions"
              :key="item.id"
              class="session-item"
              :class="{ active: String(activeSessionId) === String(item.id) }"
              @click="selectSession(item.id)"
            >
              <div class="session-title">{{ item.productTitle || `商品 #${item.productId}` }}</div>
              <div class="session-meta">买家 {{ item.buyerId }} · 卖家 {{ item.sellerId }}</div>
              <div class="session-last">{{ item.lastMessage || '暂无消息，开始咨询吧' }}</div>
            </button>
          </el-scrollbar>
        </aside>

        <section class="message-panel">
          <el-empty v-if="!activeSessionId" description="请选择左侧会话" />
          <template v-else>
            <el-scrollbar ref="messageScrollbarRef" class="message-list">
              <div v-if="!messages.length" class="empty-message">暂无消息，发送第一条咨询吧</div>
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
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ensureConsultSession, getConsultMessages, getMyConsultSessions, sendConsultMessage } from '../api/consult'
import { getUserId } from '../utils/user'

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

const goProducts = () => {
  router.push('/products')
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
  await loadMessages()
  startPolling()
}

const ensureByRouteProduct = async () => {
  const productId = route.query.productId
  if (!productId) return null
  const res = await ensureConsultSession(productId)
  return res?.data?.id || null
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
    const msg = error?.data?.message || error?.message || '加载咨询页面失败'
    ElMessage.error(msg)
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
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 10px;
  padding: 10px;
  cursor: pointer;
}

.session-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.session-title {
  font-weight: 600;
  color: #303133;
}

.session-meta {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}

.session-last {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-panel {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px;
  display: flex;
  flex-direction: column;
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
