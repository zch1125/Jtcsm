/**
 * 统一 HTTP 请求封装
 * 基于 uni.request，自动注入 JWT token
 */

// 后端 API 基础地址
const BASE_URL = 'http://localhost:8081'

// 请求拦截器：自动注入 token
function getHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }
  // 从 uni 本地存储读取 token
  try {
    const token = uni.getStorageSync('token')
    if (token) {
      headers['Authorization'] = 'Bearer ' + token
    }
  } catch (e) { /* 无 token 时忽略 */ }
  return headers
}

// 通用请求方法
function request<T>(options: {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
}): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      header: getHeaders(),
      data: options.data,
      success: (res: any) => {
        const body = res.data
        if (res.statusCode === 200 && body.code === 200) {
          resolve(body.data)
        } else if (res.statusCode === 401 || body.code === 401) {
          // 未登录，清除本地 token 并跳转
          uni.removeStorageSync('token')
          uni.showToast({ title: '请先登录', icon: 'none' })
          reject(new Error('未登录'))
        } else {
          uni.showToast({ title: body.message || '请求失败', icon: 'none' })
          reject(new Error(body.message || '请求失败'))
        }
      },
      fail: (err: any) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      },
    })
  })
}

export default {
  get<T>(url: string, data?: any): Promise<T> {
    return request<T>({ url, method: 'GET', data })
  },
  post<T>(url: string, data?: any): Promise<T> {
    return request<T>({ url, method: 'POST', data })
  },
  put<T>(url: string, data?: any): Promise<T> {
    return request<T>({ url, method: 'PUT', data })
  },
  delete<T>(url: string, data?: any): Promise<T> {
    return request<T>({ url, method: 'DELETE', data })
  },
}
