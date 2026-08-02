import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// uni-app Vite 配置
// https://uniapp.dcloud.net.cn/quickstart-cli.html
export default defineConfig({
  plugins: [uni()],
})
