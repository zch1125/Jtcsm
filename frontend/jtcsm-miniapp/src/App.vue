<script setup lang="ts">
import { onLaunch, onShow } from "@dcloudio/uni-app"
import { useUserStore } from "@/store/index"
import { login } from "@/api/index"

const userStore = useUserStore()

/**
 * 小程序启动时自动登录（开发环境使用 mock 登录）
 */
onLaunch(async () => {
  console.log("App Launch — 今天吃什么启动")

  // 尝试从本地存储读取已有 token
  const hasToken = userStore.loadFromStorage()

  if (hasToken) {
    console.log("已有登录 token，跳过登录流程")
    return
  }

  // 没有 token 则自动执行 mock 登录
  try {
    const res = await login("mock", "美食家", "")
    userStore.setLoginInfo(
      res.token,
      res.userId,
      "mock_openid",
      res.nickname,
      res.avatar
    )
    console.log("自动登录成功:", res.nickname)
  } catch (e) {
    console.warn("自动登录失败:", e)
  }
})

onShow(() => {
  console.log("App Show")
})
</script>

<style lang="scss">
/* 全局样式 */
page {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 14px;
  color: #333;
  background-color: #f8f8f8;
}
</style>
