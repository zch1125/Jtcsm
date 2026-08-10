<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="login-logo">吃</div>
      <h1>今天吃什么</h1>
      <p class="login-sub">网页版与微信小程序共享收藏、历史和 AI 对话记录</p>

      <form class="login-form" @submit.prevent="handleLogin">
        <label class="field">
          <span>昵称</span>
          <input v-model="nickname" placeholder="美食家" maxlength="20" />
        </label>
        <button class="btn btn-primary login-btn" :disabled="loading" type="submit">
          {{ loading ? "登录中..." : "登录 / 进入" }}
        </button>
      </form>

      <p class="login-tip">未填写昵称时使用默认账号登录，登录后即可看到两端同步的数据。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { login } from "@/api"
import { useUserStore } from "@/store/user"

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const nickname = ref("")
const loading = ref(false)

async function handleLogin() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await login(nickname.value.trim() || "美食家", "")
    userStore.setLogin(res.token, {
      userId: res.userId,
      nickname: res.nickname || nickname.value.trim() || "美食家",
      avatar: res.avatar || ""
    })
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/home"
    router.replace(redirect)
  } catch (err: any) {
    alert(err?.message || "登录失败，请稍后重试")
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background:
    radial-gradient(circle at 20% 20%, rgba(242, 177, 52, 0.18), transparent 38%),
    radial-gradient(circle at 82% 78%, rgba(78, 127, 79, 0.16), transparent 42%),
    var(--cream);
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 34px 30px;
  text-align: center;
}

.login-logo {
  width: 58px;
  height: 58px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: var(--green);
  color: #fff;
  font-size: 26px;
  font-weight: 800;
  box-shadow: 0 12px 26px rgba(47, 93, 58, 0.26);
}

.login-card h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: var(--green-dark);
}

.login-sub {
  margin: 0 0 24px;
  font-size: 13px;
  color: var(--muted);
  line-height: 1.7;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #5c655e;
}

.field input {
  width: 100%;
  height: 44px;
  border: 1px solid var(--line);
  border-radius: 12px;
  outline: 0;
  padding: 0 14px;
  font-size: 14px;
  background: #faf6ec;
}

.field input:focus {
  border-color: var(--leaf);
  background: #fff;
}

.login-btn {
  height: 44px;
  margin-top: 4px;
}

.login-tip {
  margin: 18px 0 0;
  font-size: 12px;
  color: #a6ada7;
  line-height: 1.7;
}
</style>
