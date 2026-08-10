/**
 * 统一 HTTP 请求封装
 * 基于 uni.request，自动注入 JWT token
 */

// 后端 API 基础地址
export const BASE_URL = 'http://localhost:8081'

/** 将后端返回的相对图片路径解析为完整 URL */
export function resolveImageUrl(url?: string): string {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  if (url.startsWith('/')) return BASE_URL + url
  return url
}

// 防止并发 401 时重复发起登录
let reloginPromise: Promise<string> | null = null

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

/**
 * 开发环境 mock 登录，成功后写入新 token
 * 用于 token 缺失或过期时自动恢复登录态
 */
function mockLogin(): Promise<string> {
  if (!reloginPromise) {
    reloginPromise = new Promise<string>((resolve, reject) => {
      uni.request({
        url: BASE_URL + '/api/v1/user/login',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        data: { code: 'mock', nickname: '美食家', avatar: '' },
        success: (res: any) => {
          const body = res.data
          if (res.statusCode === 200 && body && body.code === 200 && body.data && body.data.token) {
            try {
              uni.setStorageSync('token', body.data.token)
              uni.setStorageSync('userId', body.data.userId)
            } catch (e) {
              reject(e)
              return
            }
            resolve(body.data.token)
          } else {
            reject(new Error(body && body.message ? body.message : '重新登录失败'))
          }
        },
        fail: reject,
      })
    }).then(() => {
      reloginPromise = null
    }, () => {
      reloginPromise = null
    })
  }
  return reloginPromise
}

// ==================== SSE 流式请求 ====================

export interface StreamEventHandlers {
  onDelta?: (content: string) => void
  onDone?: (data: any) => void
  onError?: (message: string) => void
}

function decodeUtf8(bytes: number[]): string {
  let out = ''
  let i = 0
  while (i < bytes.length) {
    const b0 = bytes[i]
    if (b0 < 0x80) {
      out += String.fromCharCode(b0)
      i += 1
    } else if ((b0 & 0xe0) === 0xc0 && i + 1 < bytes.length) {
      out += String.fromCharCode(((b0 & 0x1f) << 6) | (bytes[i + 1] & 0x3f))
      i += 2
    } else if ((b0 & 0xf0) === 0xe0 && i + 2 < bytes.length) {
      out += String.fromCharCode(((b0 & 0x0f) << 12) | ((bytes[i + 1] & 0x3f) << 6) | (bytes[i + 2] & 0x3f))
      i += 3
    } else if ((b0 & 0xf8) === 0xf0 && i + 3 < bytes.length) {
      const cp = ((b0 & 0x07) << 18) | ((bytes[i + 1] & 0x3f) << 12) | ((bytes[i + 2] & 0x3f) << 6) | (bytes[i + 3] & 0x3f)
      out += String.fromCodePoint(cp)
      i += 4
    } else {
      i += 1
    }
  }
  return out
}

/** 增量解码 UTF-8，避免多字节字符被分块截断 */
class IncrementalUtf8Decoder {
  private bytes: number[] = []

  append(arrayBuffer: ArrayBuffer): string {
    const arr = new Uint8Array(arrayBuffer)
    for (let i = 0; i < arr.length; i++) this.bytes.push(arr[i])

    let end = 0
    let i = 0
    while (i < this.bytes.length) {
      const b0 = this.bytes[i]
      if (b0 < 0x80) {
        i += 1
        continue
      }
      let need = 0
      if ((b0 & 0xe0) === 0xc0) need = 2
      else if ((b0 & 0xf0) === 0xe0) need = 3
      else if ((b0 & 0xf8) === 0xf0) need = 4
      else {
        i += 1
        continue
      }
      if (i + need <= this.bytes.length) {
        i += need
        continue
      }
      break
    }
    end = i
    if (end === 0) return ''
    const complete = this.bytes.slice(0, end)
    this.bytes = this.bytes.slice(end)
    return decodeUtf8(complete)
  }

  flush(): string {
    const rest = this.bytes
    this.bytes = []
    return decodeUtf8(rest)
  }
}

/**
 * 发起 SSE 流式请求（微信小程序 enableChunked）
 * 服务端事件：delta（文字增量）、done（菜谱结果）、error（错误信息）
 */
function stream(url: string, data: any, handlers: StreamEventHandlers, retried = false): { abort: () => void } {
  const decoder = new IncrementalUtf8Decoder()
  let textBuffer = ''
  let chunksReceived = false
  let settled = false

  const settle = (fn: () => void) => {
    if (settled) return
    settled = true
    fn()
  }

  const emitEvent = (block: string) => {
    let eventName = 'message'
    const dataLines: string[] = []
    const lines = block.split('\n')
    for (const line of lines) {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
    }
    if (dataLines.length === 0) return

    let payload: any
    try {
      payload = JSON.parse(dataLines.join('\n'))
    } catch (e) {
      return
    }

    if (eventName === 'delta' && typeof payload.content === 'string') {
      handlers.onDelta && handlers.onDelta(payload.content)
    } else if (eventName === 'done') {
      settle(() => handlers.onDone && handlers.onDone(payload))
    } else if (eventName === 'error') {
      settle(() => handlers.onError && handlers.onError(payload.message || 'AI 生成失败'))
    }
  }

  const appendText = (text: string) => {
    textBuffer += text
    const parts = textBuffer.split('\n\n')
    textBuffer = parts.pop() || ''
    for (const part of parts) emitEvent(part.replace(/\r$/, ''))
  }

  const flush = () => {
    if (textBuffer.trim()) emitEvent(textBuffer.replace(/\r$/, ''))
    textBuffer = ''
    appendText(decoder.flush())
  }

  const task = uni.request({
    url: BASE_URL + url,
    method: 'POST',
    header: getHeaders(),
    data,
    enableChunked: true,
    success: (res: any) => {
      if (res.statusCode === 401) {
        if (retried) {
          settle(() => handlers.onError && handlers.onError('请先登录'))
          return
        }
        mockLogin()
          .then(() => {
            stream(url, data, handlers, true)
          })
          .catch(() => {
            settle(() => handlers.onError && handlers.onError('请先登录'))
          })
        return
      }

      flush()
      if (!chunksReceived && res.data) {
        if (typeof res.data === 'string') {
          appendText(res.data)
        } else if (res.data instanceof ArrayBuffer) {
          appendText(decoder.append(res.data))
        }
        flush()
      }
      settle(() => handlers.onError && handlers.onError('AI 生成失败，请重试'))
    },
    fail: () => {
      settle(() => handlers.onError && handlers.onError('网络异常，请重试'))
    },
  })

  const requestTask: any = task
  if (typeof requestTask.onChunkReceived === 'function') {
    requestTask.onChunkReceived((res: any) => {
      chunksReceived = true
      if (res && res.data) appendText(decoder.append(res.data))
    })
  }

  return {
    abort: () => {
      try {
        requestTask.abort()
      } catch (e) {
        // 忽略中止异常
      }
    },
  }
}

// 通用请求方法
function request<T>(options: {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
}, retried = false): Promise<T> {
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
          if (retried) {
            // 重新登录后仍 401，说明后端确实拒绝该请求
            uni.removeStorageSync('token')
            uni.showToast({ title: '请先登录', icon: 'none' })
            reject(new Error('未登录'))
            return
          }

          // 尝试自动恢复登录态，成功后重放原请求
          mockLogin()
            .then(() => {
              request<T>(options, true).then(resolve).catch(reject)
            })
            .catch(() => {
              uni.removeStorageSync('token')
              uni.showToast({ title: '请先登录', icon: 'none' })
              reject(new Error('未登录'))
            })
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
  stream,
}
