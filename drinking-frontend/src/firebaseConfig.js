import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { getAuth } from "firebase/auth"; // 💡 1. 必須引入 getAuth

const firebaseConfig = {
    apiKey: "AIzaSyCFQlAen8XIY0VTxzl0usNvz-FxO-gsRZM",
    authDomain: "project-b5e05.firebaseapp.com",
    projectId: "project-b5e05",
    storageBucket: "project-b5e05.firebasestorage.app",
    messagingSenderId: "302990497638",
    appId: "1:302990497638:web:4ad06a28fce00b5e5d5aa7",
    measurementId: "G-EZ7MPPE8ZN"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);

// 💡 2. 必須建立並匯出 auth，這樣 Auth.vue 才拿得到它
export const auth = getAuth(app);