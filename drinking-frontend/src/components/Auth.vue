<template>
  <div class="auth-wrapper">
    <div class="auth-card">
      <div class="auth-header">
        <h1>{{ showPhoneInput ? '帳號綁定' : '會員登入' }}</h1>
        <p>{{ showPhoneInput ? '請完成手機綁定以建立帳號' : '歡迎回來，請選擇登入方式' }}</p>
      </div>

      <div class="auth-body">
        <div v-if="!showPhoneInput">
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
        </div>

        <div v-else class="phone-binding-step">
          <div class="form-group">
            <label>手機號碼</label>
            <input
                v-model="loginForm.phoneNumber"
                type="text"
                placeholder="請輸入 10 碼手機號碼"
                maxlength="10"
                autoFocus
            />
          </div>
          <button @click="handleSocialLoginSuccess(tempIdToken, 'google.com')" class="btn-primary">
            完成綁定並登入
          </button>
          <button @click="showPhoneInput = false" class="btn-text" style="width: 100%; margin-top: 10px;">返回登入方式</button>
        </div>
      </div>

      <div v-if="!showPhoneInput" class="auth-footer">
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
import { signInWithPopup } from "firebase/auth"; // 💡 增加這個匯入

const router = useRouter();
const isLoading = ref(false);
const loginForm = reactive({ phoneNumber: '', password: '' });
const showPhoneInput = ref(false);
const tempIdToken = ref('');

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
      console.log("複製這串到 Postman:", idToken);
      await handleSocialLoginSuccess(idToken, result.providerId);
    }
  } catch (error) {
    console.error("跳轉處理失敗:", error);
  }
});

// 🚀 三方登入跳轉成功後的後端對接
const handleSocialLoginSuccess = async (idToken, provider) => {
  try {
    const payload = {
      idToken: idToken,
      phoneNumber: loginForm.phoneNumber, // 如果是第一次，這裡是空字串
      name: auth.currentUser?.displayName || "新用戶"
    };

    console.log("正在發送請求至後端...", payload);
    const res = await axios.post('http://localhost:8081/api/auth/social-login', payload);

    if (res.data.code === "200" || res.data.code === 200) {
      const { token, name, role, userId, storeStatus, storeId } = res.data.data;
      localStorage.setItem('token', token);
      localStorage.setItem('name', name);
      localStorage.setItem('role', role);
      localStorage.setItem('userId', userId);
      localStorage.setItem('storeStatus', storeStatus);
      if (storeId) localStorage.setItem('storeId', storeId);
      alert("✅ 登入成功！");
      router.push('/dashboard');
    }

    else if (res.data.code === "201" || res.data.code === 201) {
      // 💡 這裡要用 .value 賦值
      tempIdToken.value = idToken;
      showPhoneInput.value = true;
      alert("首次登入，請輸入手機號碼以完成綁定");
    }
    else {
      alert("❌ " + res.data.msg);
    }
  } catch (error) {
    // 這裡就是你剛才跳「驗證失敗」的地方，因為上面的代碼報錯直接進到 catch
    console.error("❌ 發生錯誤:", error);
    alert("驗證失敗: " + (error.response?.data?.msg || error.message));
  }
};

const loginWithGoogle = async () => {
  const provider = new GoogleAuthProvider();
  try {
    const result = await signInWithPopup(auth, provider); // 💡 改用 Popup
    const idToken = await result.user.getIdToken();
    console.log("✅ 拿到 Google Token:", idToken);

    // 直接發送給後端
    await handleSocialLoginSuccess(idToken, 'google.com');
  } catch (error) {
    console.error("Google 失敗:", error);
    alert("登入失敗: " + error.message);
  }
};

// 🚀 Facebook 登入
const loginWithFacebook = async () => {
  console.log("嘗試發起 Facebook 登入...");
  if (!auth) return alert("Firebase Auth 未載入");

  const provider = new FacebookAuthProvider();

  // // 💡 選填設定：如果需要抓取使用者的電子郵件
  // provider.addScope('email');

  try {
    // 💡 使用 Popup 模式，測試最穩
    const result = await signInWithPopup(auth, provider);

    // 成功後拿到 Token
    const idToken = await result.user.getIdToken();
    console.log("✅ Facebook 登入成功，拿到 Token!");

    // 傳給你的 Java 後端驗證
    await handleSocialLoginSuccess(idToken, 'facebook.com');

  } catch (error) {
    console.error("Facebook 登入失敗:", error);

    // 💡 Facebook 常見錯誤：帳號電子郵件重複
    if (error.code === 'auth/account-exists-with-different-credential') {
      alert("此信箱已關聯其他登入方式（如 Google），請改用原方式登入。");
    } else {
      alert("Facebook 登入出錯: " + error.message);
    }
  }
};
// 🚀 手機登入
const handleLogin = async () => {
  if (!loginForm.phoneNumber || !loginForm.password) return alert("請填寫手機與密碼");
  isLoading.value = true;
  try {
    const response = await axios.post('http://localhost:8081/api/auth/login', loginForm);
    if (response.data.code === "200") {
      const { token, name, role, userId, storeStatus, storeId } = response.data.data;
      localStorage.setItem('token', token);
      localStorage.setItem('name', name);
      localStorage.setItem('role', role);
      localStorage.setItem('userId', userId);
      localStorage.setItem('storeStatus', storeStatus);
      if (storeId) localStorage.setItem('storeId', storeId);
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