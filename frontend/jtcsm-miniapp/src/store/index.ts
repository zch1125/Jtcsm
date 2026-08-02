import { defineStore } from "pinia"
import { ref, computed } from "vue"

/**
 * 全局用户状态管理
 * 登录后自动同步到 uni 本地存储
 */
export const useUserStore = defineStore("user", () => {
  // ==================== 状态 ====================

  /** 用户 token */
  const token = ref<string>("")

  /** 用户 ID */
  const userId = ref<number>(0)

  /** 用户 openId */
  const openId = ref<string>("")

  /** 用户昵称 */
  const nickname = ref<string>("")

  /** 用户头像 */
  const avatar = ref<string>("")

  /** 是否已初始化过登录 */
  const initialized = ref(false)

  // ==================== 计算属性 ====================

  /** 是否已登录 */
  const isLogin = computed(() => !!token.value)

  // ==================== 方法 ====================

  /** 设置登录信息（同步写入本地存储） */
  function setLoginInfo(
    t: string,
    uid: number,
    oid: string,
    nick: string,
    ava: string
  ) {
    token.value = t
    userId.value = uid
    openId.value = oid
    nickname.value = nick
    avatar.value = ava
    initialized.value = true

    // 持久化 token 到本地存储
    try {
      uni.setStorageSync("token", t)
      uni.setStorageSync("userId", uid)
    } catch (e) {
      // 忽略存储错误
    }
  }

  /** 清除登录信息 */
  function clearLoginInfo() {
    token.value = ""
    userId.value = 0
    openId.value = ""
    nickname.value = ""
    avatar.value = ""

    try {
      uni.removeStorageSync("token")
      uni.removeStorageSync("userId")
    } catch (e) {
      // 忽略存储错误
    }
  }

  /** 初始化时读取本地存储的 token */
  function loadFromStorage() {
    try {
      const savedToken = uni.getStorageSync("token")
      if (savedToken) {
        token.value = savedToken
        initialized.value = true
        return true
      }
    } catch (e) {
      // 忽略
    }
    initialized.value = true
    return false
  }

  return {
    token,
    userId,
    openId,
    nickname,
    avatar,
    initialized,
    isLogin,
    setLoginInfo,
    clearLoginInfo,
    loadFromStorage,
  }
})
