import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

/**
 * uni-app 入口 —— 创建 Vue 3 应用并挂载 Pinia 状态管理
 */
export function createApp() {
  const app = createSSRApp(App)

  // Pinia 状态管理
  const pinia = createPinia()
  app.use(pinia)

  return { app }
}
