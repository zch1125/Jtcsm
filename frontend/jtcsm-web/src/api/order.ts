import request from './request'
import type { PageResult, OrderItem } from './types'

/**
 * 订单列表（分页 + 搜索 + 状态筛选）
 */
export function getOrderList(params: {
  page: number
  size: number
  keyword?: string
  status?: number
}) {
  return request.get<any, PageResult<OrderItem>>('/orders', { params })
}

/**
 * 订单详情
 */
export function getOrder(id: number) {
  return request.get<any, OrderItem>(`/orders/${id}`)
}
