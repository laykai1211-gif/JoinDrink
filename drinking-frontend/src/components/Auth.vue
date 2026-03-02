<template>
  <div class="auth-wrapper">
    <div class="auth-card">
      <div class="auth-header">
        <h1>會員登入</h1>
        <p>歡迎回來，請選擇登入方式</p>
      </div>

      <div class="auth-body">
        <div class="form-group">
          <label>手機號碼</label>
          <input v-model="loginForm.phoneNumber" type="text" placeholder="0912345678" />
        </div>
        <div class="form-group">
          <label>密碼</label>
          <input v-model="loginForm.password" type="password" placeholder="請輸入密碼" @keyup.enter="handleLogin" />
        </div>
        <div class="form-options">
          <router-link to="/reset-password" class="forgot-link">忘記密碼？</router-link>
        </div>
        <button @click="handleLogin" class="btn-primary" :disabled="isLoading">
          {{ isLoading ? '登入中...' : '立即登入' }}
        </button>
      </div>

      <div class="social-divider"><span>或使用以下方式登入</span></div>

      <div class="social-login-group">
        <button @click="loginWithGoogle" class="btn-social google">
          <img src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg" alt="Google" />
          Google 登入
        </button>
        <button @click="loginWithFacebook" class="btn-social facebook">
          <img src="https://upload.wikimedia.org/wikipedia/en/0/04/Facebook_f_logo_%282021%29.svg" alt="FB" />
          Facebook 登入
        </button>
      </div>

      <div class="auth-footer">
        <p>還沒有帳號嗎？ <router-link to="/register" class="register-link">立即註冊</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { auth } from '../firebaseConfig'; // 💡 關鍵 1：確認這個 export 真的存在
import {
  GoogleAuthProvider,
  FacebookAuthProvider,
  signInWithRedirect,
  getRedirectResult
} from "firebase/auth";
import axios from 'axios';

const router = useRouter();
const isLoading = ref(false);
const loginForm = reactive({ phoneNumber: '', password: '' });

// 🚀 頁面掛載：檢查是否有從 Google 跳轉回來的 Token
onMounted(async () => {
  if (!auth) {
    console.error("❌ Firebase Auth 未正確初始化，請檢查 firebaseConfig.js");
    return;
  }

  try {
    const result = await getRedirectResult(auth);
    if (result) {
      console.log("✅ 成功抓到跳轉結果！");
      const idToken = await result.user.getIdToken();
      await handleSocialLoginSuccess(idToken, result.providerId);
    }
  } catch (error) {
    console.error("跳轉處理失敗:", error);
  }
});

// 🚀 三方登入跳轉成功後的後端對接
const handleSocialLoginSuccess = async (idToken, provider) => {
  try {
    const response = await axios.post('http://localhost:8081/api/auth/social-login', {
      idToken: idToken,
      provider: provider
    });
    if (response.data.code === "200" || response.data.code === 200) {
      alert("✅ 登入成功！");
      router.push('/dashboard');
    }
  } catch (error) {
    alert("後端驗證失敗，請確認後端已開啟三方登入接口");
  }
};

// 🚀 Google 登入
const loginWithGoogle = async () => {
  console.log("嘗試發起 Google 跳轉...");
  if (!auth) return alert("Firebase 未載入");

  const provider = new GoogleAuthProvider();
  try {
    // 💡 關鍵 2：直接執行跳轉
    await signInWithRedirect(auth, provider);
  } catch (error) {
    console.error("Google 跳轉失敗:", error);
    alert("跳轉出錯: " + error.code);
  }
};

// 🚀 Facebook 登入
const loginWithFacebook = async () => {
  const provider = new FacebookAuthProvider();
  await signInWithRedirect(auth, provider);
};

// 🚀 手機登入
const handleLogin = async () => {
  if (!loginForm.phoneNumber || !loginForm.password) return alert("請填寫手機與密碼");
  isLoading.value = true;
  try {
    const response = await axios.post('http://localhost:8081/api/auth/login', loginForm);
    if (response.data.code === "200") {
      alert("✅ 登入成功！");
      router.push('/dashboard');
    } else {
      alert("❌ " + response.data.msg);
    }
  } catch (error) {
    alert("伺服器連線失敗");
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
/* 樣式保持原樣，這部分沒問題 */
.auth-wrapper { display: flex; justify-content: center; align-items: center; min-height: 100vh; background-color: #f8fafc; }
.auth-card { width: 100%; max-width: 420px; padding: 2.5rem; background: white; border-radius: 20px; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05); }
.auth-header { text-align: center; margin-bottom: 2rem; }
.form-group { margin-bottom: 1.2rem; }
.form-group label { display: block; font-size: 0.85rem; font-weight: 600; color: #475569; margin-bottom: 0.5rem; }
input { width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 10px; box-sizing: border-box; }
.btn-primary { width: 100%; padding: 14px; background: #3b82f6; color: white; border: none; border-radius: 10px; font-weight: bold; cursor: pointer; }
.social-divider { margin: 2rem 0; text-align: center; border-bottom: 1px solid #e2e8f0; position: relative; }
.social-divider span { position: absolute; top: -10px; left: 50%; transform: translateX(-50%); background: white; padding: 0 15px; color: #94a3b8; font-size: 0.8rem; }
.social-login-group { display: flex; flex-direction: column; gap: 12px; }
.btn-social { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 12px; border: 1px solid #e2e8f0; background: white; border-radius: 10px; cursor: pointer; }
.btn-social img { width: 20px; }
.auth-footer { text-align: center; margin-top: 2rem; font-size: 0.9rem; color: #64748b; }
.register-link { color: #3b82f6; font-weight: bold; text-decoration: none; }
</style>