import { createRouter, createWebHistory } from 'vue-router'
import RegisterView from '../components/Register.vue'
import ResetPasswordView from '../components/Resetpassword.vue'
import StoreEdit from "../components/StoreEdit.vue";
import Auth from "../components/Auth.vue";
import MenuBatchUpload from "../components/MenuBatchUpload.vue";
import Dashboard from "../components/Dashboard.vue";

const routes = [
    { path: '/', name: 'Login', component:Auth  },
    { path: '/register', name: 'Register', component: RegisterView },
    { path: '/auth', name: 'Auth', component: Auth },
    { path: '/dashboard', name: 'Dashboard', component: Dashboard },
    { path: '/reset-password', name: 'ResetPassword', component: ResetPasswordView },
    { path: '/store-edit', name: 'StoreEdit', component: StoreEdit },
    { path: '/MenuBatchUpload', name: 'MenuBatchUpload', component: MenuBatchUpload }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router