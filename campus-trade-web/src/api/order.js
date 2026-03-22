import request from '../utils/request'

export function getOrderList(params = {}) {
  return request({
    url: '/order/list',
    method: 'get',
    params
  })
}

export function createOrder(data) {
  return request({
    url: '/order/create',
    method: 'post',
    data
  })
}

export function payOrder(id) {
  return request({
    url: `/order/${id}/pay`,
    method: 'put'
  })
}

export function confirmOrder(id) {
  return request({
    url: `/order/${id}/confirm`,
    method: 'put'
  })
}

export function finishOrder(id) {
  return request({
    url: `/order/${id}/finish`,
    method: 'put'
  })
}

export function cancelOrder(id, reason = '') {
  return request({
    url: `/order/${id}/cancel`,
    method: 'put',
    data: { reason }
  })
}
