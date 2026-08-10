const TOKEN_KEY = "jtcsm-user-token"
const USER_KEY = "jtcsm-user-info"

export interface StoredUser {
  userId: number
  nickname: string
  avatar: string
}

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ""
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getStoredUser(): StoredUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredUser
  } catch {
    return null
  }
}

export function setStoredUser(user: StoredUser): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}
