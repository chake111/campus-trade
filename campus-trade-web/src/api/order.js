import request from '../utils/request'

export function createOrder(data) {
  return request({
    url: '/order/create',
    method: 'post',
    data
  })
}

export function getOrderList(userId, role) {
  const params = { userId }
  if (role) params.role = role
  return request({
    url: '/order/list',
    method: 'get',
    params
  })
}

export function updateOrder(orderId, newStatus, role) {
  const data = { orderId, status: newStatus }
  if (role) data.role = role
  return request({
    url: '/order/update',
    method: 'post',
    data
  })
}
