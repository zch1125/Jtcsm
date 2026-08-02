import request from './request'
import type { PageResult, UserItem } from './types'

/**
 * 用户列表（分页 + 搜索）
 */
export function getUserList(params: { page: number; size: number; keyword?: string }) {
  return request.get<any, PageResult<UserItem>>('/users', { params })
}

/**
 * 切换用户状态（启用/禁用）
 */
export function toggleUserStatus(id: number, status: number) {
  return request.put(`/users/${id}/status`, { status })
}
