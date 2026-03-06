<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2>手機號碼註冊</h2>
      <p class="subtitle">建立新帳號以開始使用</p>

      <div class="mode-switch">
        <label>
          <input type="checkbox" v-model="isMockMode" />
          Mock 模式（不發真實簡訊）
        </label>
      </div>

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
          <label>簡訊驗證碼{{ isMockMode ? "（Mock 模式：輸入任意 6 碼）" : "" }}</label>
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
const isMockMode = ref(true);

const handleSendSms = async () => {
  if (!phoneNumber.value || phoneNumber.value.length < 10) return alert("請輸入正確的手機號碼");
  isSending.value = true;

  try {
    // 💡 先檢查手機號碼是否已註冊
    const checkRes = await axios.get('http://localhost:8081/api/auth/check-phone', {
      params: { phone: phoneNumber.value }
    });
    if (checkRes.data.data === true) {
      alert("❌ 此號碼已註冊過，請直接登入");
      return;
    }

    if (isMockMode.value) {
      otpSent.value = true;
      alert("【Mock 模式】驗證碼已模擬發送！請輸入任意 6 碼繼續");
    } else {
      if (window.recaptchaVerifier) window.recaptchaVerifier.clear();
      window.recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', { size: 'invisible' });
      const formatPhone = "+886" + phoneNumber.value.replace(/^0/, '');
      console.log("發送至號碼:", formatPhone);
      const result = await signInWithPhoneNumber(auth, formatPhone, window.recaptchaVerifier);
      confirmationResult.value = result;
      otpSent.value = true;
      alert("驗證碼已發送！");
    }
  } catch (err) {
    console.error("SMS 錯誤詳情:", err);
    alert("發送失敗: " + (err.response?.data?.msg || err.code || err.message));
  } finally {
    isSending.value = false;
  }
};

const handleRegister = async () => {
  if (!otpCode.value || !name.value || !password.value) return alert("請填寫完整註冊資料");
  isSubmitting.value = true;

  try {
    let idToken;
    if (isMockMode.value) {
      idToken = "MOCK_TOKEN";
    } else {
      const result = await confirmationResult.value.confirm(otpCode.value);
      idToken = await result.user.getIdToken();
    }

    const res = await axios.post('http://localhost:8081/api/auth/register', {
      idToken,
      phoneNumber: phoneNumber.value,
      name: name.value,
      password: password.value
    });

    if (res.data.code === "200" || res.data.code === 200) {
      alert("註冊成功！");
      router.push('/');
    } else {
      alert("❌ " + res.data.msg);
    }
  } catch (err) {
    console.error("註冊錯誤:", err);
    alert("❌ " + (err.response?.data?.msg || "驗證碼錯誤或系統繁忙"));
  } finally {
    isSubmitting.value = false;
  }
};

onBeforeUnmount(() => {
  if (window.recaptchaVerifier) window.recaptchaVerifier.clear();
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