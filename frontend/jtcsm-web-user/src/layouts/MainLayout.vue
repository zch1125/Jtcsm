<template>
  <div class="layout">
    <header class="topbar">
      <div class="container topbar-inner">
        <router-link to="/home" class="brand">
          <span class="brand-mark">吃</span>
          <span class="brand-name">今天吃什么</span>
          <span class="brand-badge">网页版</span>
        </router-link>

        <nav class="desktop-nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: route.path.startsWith(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="top-actions">
          <router-link to="/profile" class="user-chip">
            <span class="avatar-dot">{{ avatarChar }}</span>
            <span class="user-name">{{ userStore.user?.nickname || "美食家" }}</span>
          </router-link>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <nav class="mobile-nav">
      <router-link
        v-for="item in mobileItems"
        :key="item.path"
        :to="item.path"
        class="mobile-item"
        :class="{ active: route.path.startsWith(item.path) }"
      >
        <span class="mobile-icon" v-html="item.icon"></span>
        <span class="mobile-label">{{ item.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useRoute } from "vue-router"
import { useUserStore } from "@/store/user"

const route = useRoute()
const userStore = useUserStore()

const navItems = [
  { path: "/home", label: "首页" },
  { path: "/recipes", label: "菜谱" },
  { path: "/ai", label: "AI 厨艺导师" },
  { path: "/favorites", label: "我的收藏" },
  { path: "/profile", label: "个人中心" }
]

const mobileItems = [
  { path: "/home", label: "首页", icon: `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/></svg>` },
  { path: "/recipes", label: "菜谱", icon: `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 21h16"/><path d="M5 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1"/><path d="M15 7h2a2 2 0 0 1 2 2v12"/><path d="M9 7v2M9 12v2M9 17v2"/></svg>` },
  { path: "/ai", label: "AI 导师", icon: `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v3M5.6 5.6l2.1 2.1M3 12h3M5.6 18.4l2.1-2.1M12 21v-3M18.4 18.4l-2.1-2.1M21 12h-3M18.4 5.6l-2.1 2.1"/><circle cx="12" cy="12" r="3"/></svg>` },
  { path: "/favorites", label: "收藏", icon: `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20.5 4.8 13a4.4 4.4 0 0 1 6.2-6.2L12 8l1-1.2A4.4 4.4 0 0 1 19.2 13Z"/></svg>` },
  { path: "/profile", label: "我的", icon: `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 21c.8-3.5 4-5 8-5s7.2 1.5 8 5"/></svg>` }
]

const avatarChar = computed(() => {
  const nick = userStore.user?.nickname || "美"
  return nick.slice(0, 1)
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(246, 241, 231, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
}

.topbar-inner {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.brand-mark {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: var(--green);
  color: #fff;
  font-size: 18px;
  font-weight: 800;
}

.brand-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--ink);
  letter-spacing: 0;
}

.brand-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: var(--green);
  background: #e3ecdf;
}

.desktop-nav {
  flex: 1;
  display: flex;
  gap: 6px;
  min-width: 0;
}

.nav-link {
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: #5c655e;
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-link:hover {
  background: #ece5d6;
}

.nav-link.active {
  color: #fff;
  background: var(--green);
}

.top-actions {
  flex-shrink: 0;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px 6px 6px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--line);
}

.avatar-dot {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--amber);
  color: #5c4300;
  font-size: 14px;
  font-weight: 700;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main {
  flex: 1;
}

.mobile-nav {
  display: none;
}

@media (max-width: 860px) {
  .desktop-nav {
    display: none;
  }

  .user-name {
    display: none;
  }
}

@media (max-width: 640px) {
  .topbar-inner {
    height: 56px;
    gap: 12px;
  }

  .brand-name {
    font-size: 16px;
  }

  .brand-badge {
    display: none;
  }

  .user-chip {
    padding: 4px;
    border: 0;
    background: transparent;
  }

  .mobile-nav {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 60;
    display: flex;
    background: rgba(255, 253, 248, 0.96);
    border-top: 1px solid var(--line);
    padding-bottom: env(safe-area-inset-bottom);
  }

  .mobile-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    padding: 8px 0 7px;
    color: #9aa29c;
    font-size: 11px;
  }

  .mobile-item.active {
    color: var(--green);
    font-weight: 700;
  }
}
</style>
