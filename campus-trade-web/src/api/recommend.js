import request from '../utils/request'

export function getRecommendProducts(userId, limit = 10) {
  return request({
    url: `/api/recommend/smart/${userId}`,
    method: 'get',
    params: { limit }
  })
}

export function getRecommendDetails(userId, productIds) {
  return request({
    url: `/api/recommend/explain/${userId}`,
    method: 'get',
    params: { productIds }
  })
}
