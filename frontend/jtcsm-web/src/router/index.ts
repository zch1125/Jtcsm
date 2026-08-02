import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

/**
 * 管理后台路由配置
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'recipes',
        name: 'Recipes',
        component: () => import('@/views/RecipeList.vue'),
        meta: { title: '菜谱管理', icon: 'Food' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/SystemMonitor.vue'),
        meta: { title: '系统监控', icon: 'Monitor' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局前置守卫 —— 未登录时跳转到登录页
 */
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  document.title = (to.meta.title as string) || '今天吃什么 · 管理后台'

  // 需要认证的页面，检查 token
  if (to.matched.some(r => r.meta.requiresAuth)) {
    const token = localStorage.getItem('admin-token')
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
  }

  // 已登录用户访问登录页，重定向到仪表盘
  if (to.name === 'Login' && localStorage.getItem('admin-token')) {
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router
