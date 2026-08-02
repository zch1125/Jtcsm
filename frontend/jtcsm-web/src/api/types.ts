/**
 * 分页响应结构
 */
export interface PageResult<T> {
  /** 当前页记录列表 */
  records: T[]
  /** 总记录数 */
  total: number
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}

/** 系统监控数据 */
export interface SystemMonitor {
  system: {
    osName: string; osVersion: string; osArch: string
    availableProcessors: number; systemCpuLoad: number; processCpuLoad: number
  }
  jvm: {
    javaVersion: string; jvmName: string; jvmVendor: string
    startTime: string; uptime: string; uptimeMillis: number; pid: number
    inputArgs: string
  }
  memory: {
    heapUsed: number; heapMax: number; heapCommitted: number; heapUsagePercent: number
    nonHeapUsed: number; nonHeapCommitted: number
    physicalTotal: number; physicalFree: number; physicalUsagePercent: number
  }
  disk: Array<{ path: string; total: number; free: number; usable: number; usagePercent: number }>
  threads: { liveCount: number; daemonCount: number; peakCount: number }
  classes: { loadedCount: number; unloadedCount: number }
  db: { activeCount: number; idleCount: number; totalCount: number; maxActive: number }
  redis: { connected: boolean; info: string }
  es: {
    connected: boolean
    clusterName: string; nodeName: string; version: string; status: string
    indexCount: number; totalDocCount: number; totalStoreSizeBytes: number
    indices: Array<{ name: string; docCount: number; storeSizeBytes: number; health: string }>
  }
}

/**
 * 仪表盘统计数据
 */
export interface AdminDashboard {
  totalRecipes: number
  totalUsers: number
  todayOrders: number
  vipUsers: number
  todayRevenue: number
  totalOrders: number
  totalRevenue: number
}

/**
 * 菜谱项
 */
export interface RecipeItem {
  id?: number
  name: string
  coverImage?: string
  description?: string
  cuisine?: string
  difficulty?: string
  cookMethod?: string
  cookTime?: number
  calories?: number
  isVipOnly?: boolean
  viewCount?: number
  favoriteCount?: number
  status?: number
  source?: string
  createTime?: string
  updateTime?: string
}

/**
 * 用户项
 */
export interface UserItem {
  id?: number
  openid: string
  nickname?: string
  avatar?: string
  phone?: string
  gender?: number
  isVip?: number
  vipExpireTime?: string
  status?: number
  createTime?: string
  updateTime?: string
}

/**
 * 订单项
 */
export interface OrderItem {
  id?: number
  orderNo: string
  userId?: number
  planId?: number
  amount: number
  status?: number
  payTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * 套餐项
 */
export interface PlanItem {
  id?: number
  name: string
  price: number
  originalPrice: number
  days: number
  description?: string
  isEnabled?: number
  createTime?: string
  updateTime?: string
}
