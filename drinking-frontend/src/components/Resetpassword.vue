<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2>重設會員密碼</h2>
      <p class="subtitle">請驗證手機以修改您的密碼</p>

      <div class="mode-switch">
        <label>
          <input type="checkbox" v-model="isMockMode" />
          Mock 模式（不發真實簡訊）
        </label>
      </div>

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
          <label>簡訊驗證碼{{ isMockMode ? "（Mock 模式：輸入任意 6 碼）" : "" }}</label>
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
const isMockMode = ref(true); // 💡 預設 Mock 模式

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
    // 💡 先確認帳號是否存在
    const checkRes = await axios.get('http://localhost:8081/api/auth/check-phone', {
      params: { phone: phoneNumber.value }
    });
    if (checkRes.data.data === false) {
      alert("❌ 此號碼尚未註冊，請先建立帳號");
      return;
    }

    if (isMockMode.value) {
      // ✅ Mock 模式：直接跳過 Firebase
      otpSent.value = true;
      alert("【Mock 模式】驗證碼已模擬發送！請輸入任意 6 碼繼續");
    } else {
      // ✅ 真實模式：打 Firebase 簡訊
      cleanRecaptcha();
      window.recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', { size: 'invisible' });
      const formatPhone = "+886" + phoneNumber.value.replace(/^0/, '');
      confirmationResult.value = await signInWithPhoneNumber(auth, formatPhone, window.recaptchaVerifier);
      otpSent.value = true;
      alert("驗證碼已發送！如果是測試白名單，請輸入預設碼。");
    }
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
    let idToken;
    if (isMockMode.value) {
      // ✅ Mock 模式：直接用 MOCK_TOKEN
      idToken = "MOCK_TOKEN";
    } else {
      // ✅ 真實模式：Firebase 驗證 OTP
      const result = await confirmationResult.value.confirm(otpCode.value);
      idToken = await result.user.getIdToken(true);
    }

    // 呼叫後端 API
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

<style scoped>
.mode-switch {
  background: #fef9c3;
  border: 1px solid #fde047;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #713f12;
}
.mode-switch input { margin-right: 6px; cursor: pointer; }
</style>