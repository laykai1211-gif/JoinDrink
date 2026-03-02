import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // 💡 確保有匯入你的 router/index.js

const app = createApp(App)

app.use(router) // 💡 這是關鍵！沒加這行，useRoute() 就會報錯
app.mount('#app')