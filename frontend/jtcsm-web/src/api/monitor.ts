import request from './request'
import type { SystemMonitor } from './types'

/**
 * 获取系统监控数据
 */
export function getSystemMonitor() {
  return request.get<any, SystemMonitor>('/monitor')
}
