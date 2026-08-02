import { defineConfig } from "vite"
import vue from "@vitejs/plugin-vue"
import { resolve } from "path"

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": resolve(__dirname, "src")
    }
  },
  server: {
    port: 5173,
    // 代理请求到网关（网关统一分发到各后端服务）
    proxy: {
      "/api/v1": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/admin-api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      // 管理后台 API 直连 admin 服务（绕过网关，开发环境）
      "/api/admin": {
        target: "http://localhost:8082",
        changeOrigin: true
      }
    }
  }
})
