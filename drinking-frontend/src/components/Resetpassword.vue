<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2>重設會員密碼</h2>
      <p class="subtitle">請驗證手機以修改您的密碼</p>

      <div class="form-group">
        <label>手機號碼</label>
        <div class="input-row">
          <input v-model="phoneNumber" type="tel" placeholder="0912345678" :disabled="otpSent" />
          <button @click="handleSendSms" :disabled="otpSent || isSending" class="secondary-btn">
            {{ isSending ? '發送中' : (otpSent ? '已發送' : '驗證手機') }}
          </button>
        </div>
      </div>

      <div id="recaptcha-container"></div>

      <div v-if="otpSent" class="slide-in">
        <div class="form-group">
          <label>簡訊驗證碼</label>
          <input v-model="otpCode" type="text" placeholder="6 位數驗證碼" />
        </div>
        <hr />
        <div class="form-group">
          <label>設定新密碼</label>
          <input v-model="newPassword" type="password" placeholder="請輸入新密碼" />
        </div>
        <div class="form-group">
          <label>重複新密碼</label>
          <input v-model="confirmPassword" type="password" placeholder="請再次輸入新密碼" />
        </div>
        <button @click="handleUpdatePassword" class="primary-btn" :disabled="isSubmitting">
          {{ isSubmitting ? '重設中...' : '確認修改密碼' }}
        </button>
      </div>

      <div class="footer">
        <router-link to="/">返回登入</router-link>
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
const newPassword = ref('');
const confirmPassword = ref('');
const otpSent = ref(false);
const isSending = ref(false);
const isSubmitting = ref(false);
const confirmationResult = ref(null);

// 提取清理邏輯
const cleanRecaptcha = () => {
  const container = document.getElementById('recaptcha-container');
  if (container) container.innerHTML = '';

  if (window.recaptchaVerifier) {
    try {
      window.recaptchaVerifier.clear();
    } catch (e) {
      console.warn("清理驗證器時發生預期外的錯誤，忽略並繼續");
    }
    window.recaptchaVerifier = null;
  }
};

const handleSendSms = async () => {
  if (!phoneNumber.value || phoneNumber.value.length < 10) return alert("請輸入正確的手機號碼");
  isSending.value = true;

  try {
    // 💡 徹底清理，防止 auth/internal-error
    cleanRecaptcha();

    // 重新建立驗證器
    window.recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', {
      'size': 'normal'
    });

    const formatPhone = "+886" + phoneNumber.value.replace(/^0/, '');

    // 發送簡訊
    confirmationResult.value = await signInWithPhoneNumber(auth, formatPhone, window.recaptchaVerifier);

    otpSent.value = true;
    alert("驗證碼已發送！如果是測試白名單，請輸入預設碼。");
  } catch (err) {
    console.error("發送失敗詳情:", err);
    if (err.code === 'auth/captcha-check-failed') {
      alert("網域驗證失敗，請確認 Firebase Authorized domains 包含 localhost 與 127.0.0.1");
    } else if (err.code === 'auth/too-many-requests') {
      alert("請求太頻繁！請換個號碼或改用白名單測試。");
    } else {
      alert("發送失敗: " + err.code);
    }
    cleanRecaptcha(); // 失敗也要清理
  } finally {
    isSending.value = false;
  }
};

const handleUpdatePassword = async () => {
  if (!otpCode.value) return alert("請輸入驗證碼");
  if (newPassword.value !== confirmPassword.value) return alert("兩次密碼輸入不一致");
  if (newPassword.value.length < 6) return alert("新密碼長度建議至少 6 位");

  isSubmitting.value = true;
  try {
    // 1. Firebase 驗證
    const result = await confirmationResult.value.confirm(otpCode.value);

    // 2. 💡 獲取 Token 並印出 (方便你複製到 Postman)
    const idToken = await result.user.getIdToken(true);
    console.log("-----------------------------------------");
    console.log("✅ Firebase 驗證成功！");
    console.log("🚀 你的 idToken (複製到 Postman 使用):");
    console.log(idToken);
    console.log("-----------------------------------------");

    // 3. 呼叫後端 API
    const res = await axios.post('http://localhost:8081/api/auth/reset-password-firebase', {
      idToken,
      phoneNumber: phoneNumber.value,
      newPassword: newPassword.value
    });

    // 4. 根據後端 GlobalExceptionHandler 回傳的 code 處理
    if (res.data.code === "200") {
      alert("密碼重設成功！");
      router.push('/');
    } else {
      // 這裡會顯示「找不到手機號碼」或「身份驗證不符」等後端拋出的訊息
      alert("錯誤: " + res.data.msg);
    }
  } catch (err) {
    console.error("重設過程出錯:", err);
    // 處理 Firebase 端的驗證碼錯誤
    if (err.code === 'auth/invalid-verification-code') {
      alert("驗證碼錯誤，請重新輸入");
    } else {
      // 處理後端拋出的 401/404/500 等
      const errMsg = err.response?.data?.msg || "系統失敗，請確認後端是否開啟";
      alert("失敗: " + errMsg);
    }
  } finally {
    isSubmitting.value = false;
  }
};

onBeforeUnmount(() => {
  cleanRecaptcha();
});
</script>