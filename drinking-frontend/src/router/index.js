import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../components/Auth.vue'
import RegisterView from '../components/Register.vue'
import ResetPasswordView from '../components/Resetpassword.vue'
import StoreEdit from "../components/StoreEdit.vue";
import Auth from "../components/Auth.vue";

const routes = [
    { path: '/', name: 'Login', component: LoginView },
    { path: '/register', name: 'Register', component: RegisterView },
    { path: '/auth', name: 'Auth', component: Auth },
    { path: '/reset-password', name: 'ResetPassword', component: ResetPasswordView },
    { path: '/store-edit', name: 'StoreEdit', component: StoreEdit }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router