import request from '../utils/request'

export function getSmartRecommend(limit = 10) {
  return request({
    url: '/api/recommend/smart/1',
    method: 'get',
    params: { limit }
  })
}

export function getPopularRecommend(limit = 10) {
  return request({
    url: '/api/recommend/popular',
    method: 'get',
    params: { limit }
  })
}

export function getRecommendExplain(productIds) {
  return request({
    url: '/api/recommend/explain/1',
    method: 'get',
    params: { productIds }
  })
}
