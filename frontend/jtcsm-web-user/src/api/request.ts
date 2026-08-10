import axios from "axios"
import type { AxiosInstance, AxiosResponse } from "axios"
import { getToken, setToken } from "@/utils/token"

export const BASE_URL = ""

export function resolveImageUrl(url?: string): string {
  if (!url) return ""
  if (/^https?:\/\//.test(url)) return url
  if (url.startsWith("/")) return url
  return "/" + url
}

let reloginPromise: Promise<string> | null = null

function mockLogin(): Promise<string> {
  if (!reloginPromise) {
    reloginPromise = axios
      .post("/api/v1/user/login", {
        code: "mock",
        nickname: "美食家",
        avatar: ""
      })
      .then((res) => {
        const body = res.data
        if (body && body.code === 200 && body.data && body.data.token) {
          setToken(body.data.token)
          return body.data.token
        }
        throw new Error(body?.message || "重新登录失败")
      })
      .finally(() => {
        reloginPromise = null
      })
  }
  return reloginPromise
}

const request: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data
    if (body && body.code === 200) {
      return body.data
    }
    if (body && (body.code === 401 || response.status === 401)) {
      return Promise.reject(new Error(body.message || "未登录"))
    }
    return Promise.reject(new Error(body?.message || "请求失败"))
  },
  async (error) => {
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401 || body?.code === 401) {
      try {
        await mockLogin()
        const original = error.config
        if (original) {
          original.headers.Authorization = `Bearer ${getToken()}`
          return request(original)
        }
      } catch {
        // 自动登录失败，交给调用方处理
      }
    }
    return Promise.reject(new Error(body?.message || "网络异常，请稍后重试"))
  }
)

export default request
