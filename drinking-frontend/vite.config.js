import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    allowedHosts: [
      'karan-nonsequacious-karina.ngrok-free.dev'
    ],
    strictPort: true,
    hmr: {
      protocol: 'ws',
      host: 'localhost',
      port: 5173,
      // 💡 加上這個可以防止 WebSocket 頻繁斷線
      clientPort: 5173
    }
  }
})