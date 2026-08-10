import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
import { getToken } from "@/utils/token"

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/LoginView.vue"),
    meta: { title: "登录" }
  },
  {
    path: "/",
    component: () => import("@/layouts/MainLayout.vue"),
    redirect: "/home",
    children: [
      {
        path: "home",
        name: "Home",
        component: () => import("@/views/HomeView.vue"),
        meta: { title: "首页", requiresAuth: true, nav: "home" }
      },
      {
        path: "recipes",
        name: "Recipes",
        component: () => import("@/views/RecipeView.vue"),
        meta: { title: "菜谱", requiresAuth: true, nav: "recipes" }
      },
      {
        path: "ai",
        name: "AI",
        component: () => import("@/views/AiView.vue"),
        meta: { title: "AI 厨艺导师", requiresAuth: true, nav: "ai" }
      },
      {
        path: "favorites",
        name: "Favorites",
        component: () => import("@/views/FavoritesView.vue"),
        meta: { title: "我的收藏", requiresAuth: true, nav: "favorites" }
      },
      {
        path: "profile",
        name: "Profile",
        component: () => import("@/views/ProfileView.vue"),
        meta: { title: "个人中心", requiresAuth: true, nav: "profile" }
      }
    ]
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/home"
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  document.title = `${String(to.meta.title || "今天吃什么")} · 今天吃什么`
  if (to.matched.some((r) => r.meta.requiresAuth) && !getToken()) {
    return { name: "Login", query: { redirect: to.fullPath } }
  }
  if (to.name === "Login" && getToken()) {
    return { name: "Home" }
  }
  return true
})

export default router
