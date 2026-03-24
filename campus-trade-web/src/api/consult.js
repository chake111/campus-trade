import request from '../utils/request'

export function ensureConsultSession(productId, counterpartId) {
  const data = { productId }
  if (counterpartId !== undefined && counterpartId !== null && counterpartId !== '') {
    data.counterpartId = counterpartId
  }
  return request({
    url: '/api/consult/sessions/ensure',
    method: 'post',
    data,
  })
}

export function getMyConsultSessions() {
  return request({
    url: '/api/consult/sessions',
    method: 'get',
  })
}

export function getConsultMessages(sessionId) {
  return request({
    url: `/api/consult/sessions/${sessionId}/messages`,
    method: 'get',
  })
}

export function getConsultUnreadCount() {
  return request({
    url: '/api/consult/sessions/unread-count',
    method: 'get',
  })
}

export function sendConsultMessage(sessionId, content) {
  return request({
    url: `/api/consult/sessions/${sessionId}/messages`,
    method: 'post',
    data: { content },
  })
}
