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

const initRecaptcha = () => {
  if (window.recaptchaVerifier) {
    window.recaptchaVerifier.clear();
  }
  window.recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', { 'size': 'invisible' });
};

const handleSendSms = async () => {
  if (!phoneNumber.value) return alert("請輸入手機號碼");
  isSending.value = true;
  initRecaptcha();

  try {
    const formatPhone = "+886" + phoneNumber.value.replace(/^0/, '');
    confirmationResult.value = await signInWithPhoneNumber(auth, formatPhone, window.recaptchaVerifier);
    otpSent.value = true;
    alert("驗證碼已發送");
  } catch (err) {
    alert("發送失敗: " + err.message); //
    if (window.recaptchaVerifier) window.recaptchaVerifier.clear();
  } finally {
    isSending.value = false;
  }
};

const handleUpdatePassword = async () => {
  if (newPassword.value !== confirmPassword.value) return alert("兩次密碼輸入不一致");

  isSubmitting.value = true;
  try {
    const result = await confirmationResult.value.confirm(otpCode.value);
    const idToken = await result.user.getIdToken();

    const res = await axios.post('http://localhost:8081/api/auth/reset-password-firebase', {
      idToken,
      phoneNumber: phoneNumber.value,
      newPassword: newPassword.value
    });

    if (res.data.code === "200") {
      alert("密碼重設成功！");
      router.push('/');
    } else {
      alert(res.data.msg);
    }
  } catch (err) {
    alert("驗證錯誤或系統失敗");
  } finally {
    isSubmitting.value = false;
  }
};

onBeforeUnmount(() => {
  if (window.recaptchaVerifier) window.recaptchaVerifier.clear();
});
</script>