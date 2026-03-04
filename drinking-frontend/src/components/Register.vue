<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2>手機號碼註冊</h2>
      <p class="subtitle">建立新帳號以開始使用</p>

      <div class="form-group">
        <label>手機號碼</label>
        <div class="input-row">
          <input v-model="phoneNumber" type="tel" placeholder="0912345678" :disabled="otpSent" maxlength="10" />
          <button @click="handleSendSms" :disabled="otpSent || isSending" class="secondary-btn">
            {{ isSending ? '發送中' : (otpSent ? '已發送' : '獲取驗證碼') }}
          </button>
        </div>
      </div>

      <div id="recaptcha-container"></div>

      <div v-if="otpSent" class="slide-in">
        <div class="form-group">
          <label>簡訊驗證碼</label>
          <input v-model="otpCode" type="text" placeholder="6 位數驗證碼" maxlength="6" />
        </div>
        <div class="form-group">
          <label>用戶姓名</label>
          <input v-model="name" type="text" placeholder="請輸入您的姓名" />
        </div>
        <div class="form-group">
          <label>設定密碼</label>
          <input v-model="password" type="password" placeholder="至少 6 位數" />
        </div>
        <button @click="handleRegister" class="primary-btn" :disabled="isSubmitting">
          {{ isSubmitting ? '註冊中...' : '確認註冊' }}
        </button>
      </div>

      <div class="footer">
        <router-link to="/">已有帳號？返回登入</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { auth } from '../firebaseConfig';
import { RecaptchaVerifier, signInWithPhoneNumber } from "firebase/auth";
import axios from 'axios';

const router = useRouter();
const phoneNumber = ref('');
const otpCode = ref('');
const name = ref('');
const password = ref('');

const otpSent = ref(false);
const isSending = ref(false);
const isSubmitting = ref(false);
const confirmationResult = ref(null);

// 初始化 reCAPTCHA
const initRecaptcha = () => {
  if (window.recaptchaVerifier) {
    window.recaptchaVerifier.clear();
  }
  window.recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', {
    'size': 'invisible'
  });
};

// Register.vue 或 ResetPassword.vue 中的發送邏輯
const handleSendSms = async () => {
  try {
    // 清理舊的
    if (window.recaptchaVerifier) {
      window.recaptchaVerifier.clear();
    }

    // 初始化
    window.recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', {
      'size': 'normal'
    });

    // 💡 關鍵：手動 render
    const widgetId = await window.recaptchaVerifier.render();

    const formatPhone = "+886" + phoneNumber.value.replace(/^0/, '');
    console.log("發送至號碼:", formatPhone);

    const result = await signInWithPhoneNumber(auth, formatPhone, window.recaptchaVerifier);
    confirmationResult.value = result;
    otpSent.value = true;
    alert("驗證碼已發送！");

  } catch (err) {
    console.error("SMS 錯誤詳情:", err);
    // 如果還是報 invalid-app-credential，代表 Firebase 伺服器拒絕了這個請求
    alert("發送失敗: " + err.code);
  }
};

const handleRegister = async () => {
  if (!otpCode.value || !name.value || !password.value) return alert("請填寫完整註冊資料");

  isSubmitting.value = true;
  try {
    // 1. 驗證簡訊碼並取得 Firebase Token
    const result = await confirmationResult.value.confirm(otpCode.value);
    const idToken = await result.user.getIdToken();

    // 2. 傳給後端 API
    const res = await axios.post('http://localhost:8081/api/auth/register', {
      idToken,
      phoneNumber: phoneNumber.value,
      name: name.value,
      password: password.value
    });

    if (res.data.code === "200" || res.data.code === 200) {
      alert("註冊成功！");
      router.push('/');
    } else if(res.data.code === "400" || res.data.code === 400) {
      alert("此號碼已註冊過" );
    }else {
      alert("❌ " + res.data.msg);
    }
  } catch (err) {
    console.error("註冊錯誤:", err);
    alert("驗證碼錯誤或系統繁忙");
  } finally {
    isSubmitting.value = false;
  }
};

// 離開頁面時清理
onBeforeUnmount(() => {
  if (window.recaptchaVerifier) window.recaptchaVerifier.clear();
});
</script>