import request from '../utils/request'

export function createOrder(data) {
  return request({
    url: '/order/create',
    method: 'post',
    data
  })
}

export function getOrderList(userId, role) {
  return request({
    url: '/order/list',
    method: 'get',
    params: { userId, role }
  })
}

export function updateOrder(orderId, newStatus) {
  return request({
    url: '/order/update',
    method: 'put',
    data: { orderId, newStatus }
  })
}
