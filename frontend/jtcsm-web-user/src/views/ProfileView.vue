<template>
  <div class="page-body profile-page">
    <div class="container">
      <div class="profile-grid">
        <section class="profile-card card">
          <div class="profile-head">
            <div class="avatar-lg">{{ avatarChar }}</div>
            <div>
              <h1>{{ profile?.nickname || userStore.user?.nickname || "美食家" }}</h1>
              <p v-if="profile?.phone">账号手机号：{{ profile.phone }}</p>
              <p v-else class="muted-line">微信小程序与网页版共用同一账号数据</p>
            </div>
          </div>
          <div class="profile-actions">
            <button class="btn btn-danger" @click="handleLogout">退出登录</button>
          </div>
        </section>

        <section class="menu-card card">
          <router-link to="/favorites" class="menu-item">
            <span class="menu-icon fav">♥</span>
            <span class="menu-label">我的收藏</span>
            <span class="menu-arrow">›</span>
          </router-link>
          <router-link to="/ai" class="menu-item">
            <span class="menu-icon ai">AI</span>
            <span class="menu-label">AI 厨艺导师</span>
            <span class="menu-arrow">›</span>
          </router-link>
          <router-link to="/recipes" class="menu-item">
            <span class="menu-icon recipe">菜</span>
            <span class="menu-label">菜谱搜索</span>
            <span class="menu-arrow">›</span>
          </router-link>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { getUserProfile } from "@/api"
import { useUserStore } from "@/store/user"

const router = useRouter()
const userStore = useUserStore()
const profile = ref<any>(null)

const avatarChar = computed(() => {
  const nick = profile.value?.nickname || userStore.user?.nickname || "美"
  return nick.slice(0, 1)
})

async function loadProfile() {
  try {
    profile.value = await getUserProfile()
  } catch {
    profile.value = null
  }
}

function handleLogout() {
  userStore.logout()
  router.replace("/login")
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-grid {
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-card {
  padding: 26px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar-lg {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--amber), var(--tomato));
  color: #fff;
  font-size: 28px;
  font-weight: 800;
  box-shadow: 0 10px 24px rgba(231, 111, 81, 0.3);
}

.profile-head h1 {
  margin: 0 0 6px;
  font-size: 24px;
  color: var(--green-dark);
}

.profile-head p {
  margin: 0;
  font-size: 13px;
  color: var(--muted);
}

.profile-actions {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #f0ead9;
  display: flex;
  justify-content: flex-end;
}

.menu-card {
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0ead9;
  transition: background 0.15s ease;
}

.menu-item:last-child {
  border-bottom: 0;
}

.menu-item:hover {
  background: #faf6ec;
}

.menu-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 800;
}

.menu-icon.fav {
  background: #fdeee8;
  color: var(--tomato);
}

.menu-icon.ai {
  background: #e3ecdf;
  color: var(--green);
}

.menu-icon.recipe {
  background: #fff1d6;
  color: #9a5b00;
}

.menu-label {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}

.menu-arrow {
  font-size: 20px;
  color: #b9b1a1;
}
</style>
