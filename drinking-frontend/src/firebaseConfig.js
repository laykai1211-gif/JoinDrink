import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, FacebookAuthProvider } from "firebase/auth";
// 💡 這些資訊在你的 Firebase Console -> 專案設定 -> 一般 -> 你的應用程式
const firebaseConfig = {
    apiKey: "AIzaSyCFQlAen8XIY0VTxzl0usNvz-FxO-gsRZM",
    authDomain: "project-b5e05.firebaseapp.com", // 修正處
    projectId: "project-b5e05",                  // 修正處
    storageBucket: "project-b5e05.appspot.com",  // 修正處
    messagingSenderId: "302990497638",
    appId: "1:302990497638:web:4ad06a28fce00b5e5d5aa7",
};

const app = initializeApp(firebaseConfig);

// 第二步：才能使用初始化後的 app 來獲取服務
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
export const facebookProvider = new FacebookAuthProvider();