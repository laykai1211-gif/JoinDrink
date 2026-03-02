import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue' // 💡 這行是關鍵！

export default defineConfig({
  plugins: [
    vue() // 💡 告訴 Vite：看到 .vue 檔案請用這個插件處理
  ],
  server: {
    port: 5173,
    strictPort: true,
    hmr: {
      overlay: false // 💡 順便關閉錯誤遮罩，讓畫面清爽一點
    }
  }
})