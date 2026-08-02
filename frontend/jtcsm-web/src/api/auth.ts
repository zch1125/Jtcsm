import request from './request'

/**
 * 管理员登录
 */
export function adminLogin(username: string, password: string) {
  return request.post('/login', { username, password })
}
