import request from './request'
import type { PageResult, PlanItem } from './types'

/**
 * 套餐列表（分页）
 */
export function getPlanList(params: { page: number; size: number }) {
  return request.get<any, PageResult<PlanItem>>('/plans', { params })
}

/**
 * 新增套餐
 */
export function createPlan(data: Partial<PlanItem>) {
  return request.post('/plans', data)
}

/**
 * 更新套餐
 */
export function updatePlan(id: number, data: Partial<PlanItem>) {
  return request.put(`/plans/${id}`, data)
}

/**
 * 删除套餐
 */
export function deletePlan(id: number) {
  return request.delete(`/plans/${id}`)
}
