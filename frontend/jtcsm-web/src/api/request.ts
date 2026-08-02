import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 创建 axios 实例，配置 baseURL 和拦截器
 */
const request: AxiosInstance = axios.create({
  baseURL: '/api/admin',
  timeout: 15000
})

// 请求拦截器 —— 注入 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin-token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器 —— 统一错误处理
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    // 成功响应
    if (code === 200) return data
    // 业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem('admin-token')
        window.location.href = '/login'
      }
      ElMessage.error(`请求错误: ${status}`)
    } else {
      ElMessage.error('网络错误，请检查服务是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
