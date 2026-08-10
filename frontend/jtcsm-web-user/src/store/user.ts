import { defineStore } from "pinia"
import { computed, ref } from "vue"
import {
  clearToken,
  getStoredUser,
  getToken,
  setStoredUser,
  setToken,
  type StoredUser
} from "@/utils/token"

export const useUserStore = defineStore("user", () => {
  const user = ref<StoredUser | null>(getStoredUser())
  const token = ref(getToken())
  const isLogin = computed(() => !!token.value)

  function setLogin(tokenValue: string, userValue: StoredUser) {
    token.value = tokenValue
    user.value = userValue
    setToken(tokenValue)
    setStoredUser(userValue)
  }

  function logout() {
    token.value = ""
    user.value = null
    clearToken()
  }

  return { user, token, isLogin, setLogin, logout }
})
