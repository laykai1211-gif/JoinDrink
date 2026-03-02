import { createRouter, createWebHistory } from 'vue-router'
// 💡 修正這裡：路徑必須與你的實際檔案名稱 Resetpassword.vue 完全一致
import ResetPasswordView from '../components/Resetpassword.vue'
import Auth from '../components/Auth.vue'

const routes = [
    {
        path: '/',
        name: 'Login',
        component: Auth
    },
    {
        // 💡 建議將路徑改為 /Resetpassword，與你的檔案名稱保持直覺一致
        path: '/Resetpassword',
        name: 'ResetPassword',
        component: ResetPasswordView
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router