import request from '../utils/request'

export function getCreditScore(params) {
  return request({
    url: '/credit/score',
    method: 'get',
    params
  })
}

export function getCreditLogs(params) {
  return request({
    url: '/credit/log',
    method: 'get',
    params
  })
}
