import request from './request'
import type { AdminDashboard } from './types'

/**
 * 获取仪表盘统计数据
 */
export function getDashboard() {
  return request.get<any, AdminDashboard>('/dashboard')
}
