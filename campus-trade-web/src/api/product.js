import request from '../utils/request'

export function getProductList(params) {
  return request({
    url: '/product/list',
    method: 'get',
    params
  })
}

export function getProductDetail(id) {
  return request({
    url: `/product/${id}`,
    method: 'get'
  })
}

export function createProduct(data) {
  return request({
    url: '/product/add',
    method: 'post',
    data
  })
}

export function getAdminProductList(params) {
  return request({
    url: '/api/admin/products',
    method: 'get',
    params
  })
}

export function offShelfProduct(id) {
  return request({
    url: `/api/admin/products/${id}/off-shelf`,
    method: 'put'
  })
}

export function restoreProduct(id) {
  return request({
    url: `/api/admin/products/${id}/restore`,
    method: 'put'
  })
}

export function deleteProductByAdmin(id) {
  return request({
    url: `/api/admin/products/${id}`,
    method: 'delete'
  })
}
