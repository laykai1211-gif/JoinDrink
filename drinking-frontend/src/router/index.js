import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../components/Auth.vue'
import RegisterView from '../components/Register.vue'
import ResetPasswordView from '../components/Resetpassword.vue'

const routes = [
    { path: '/', name: 'Login', component: LoginView },
    { path: '/register', name: 'Register', component: RegisterView },
    { path: '/reset-password', name: 'ResetPassword', component: ResetPasswordView }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router