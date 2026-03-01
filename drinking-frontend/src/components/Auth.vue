<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-50 to-blue-100 p-6 font-sans">
    <div class="bg-white p-8 rounded-3xl shadow-2xl w-full max-w-md border border-white">

      <div id="recaptcha-container"></div>

      <div class="text-center mb-6">
        <h1 class="text-4xl font-black text-indigo-600 mb-2">jo飲 揪飲</h1>
        <p class="text-gray-400 text-sm">串聯每一口美好，從登入開始</p>
      </div>

      <div v-if="regStep === 'START'" class="flex mb-6 bg-gray-100 rounded-2xl p-1">
        <button @click="resetToLogin" :class="isLoginMode ? 'bg-white shadow-md text-indigo-600' : 'text-gray-500'" class="flex-1 py-2 rounded-xl font-bold transition-all">登入</button>
        <button @click="isLoginMode = false" :class="!isLoginMode ? 'bg-white shadow-md text-indigo-600' : 'text-gray-500'" class="flex-1 py-2 rounded-xl font-bold transition-all">註冊</button>
      </div>

      <div v-if="isLoginMode" class="space-y-4 animate-in">
        <input v-model="form.phoneNumber" type="tel" placeholder="手機號碼 (+8869...)" class="w-full px-5 py-4 rounded-2xl border border-gray-100 bg-gray-50 outline-none focus:ring-4 focus:ring-indigo-100 transition-all" />
        <input v-model="form.password" type="password" placeholder="密碼" class="w-full px-5 py-4 rounded-2xl border border-gray-100 bg-gray-50 outline-none focus:ring-4 focus:ring-indigo-100 transition-all" />
        <button @click="handleLogin" :disabled="isLoading" class="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black shadow-xl hover:bg-indigo-700 transition-all">
          {{ isLoading ? '登入中...' : '立即進入商店' }}
        </button>

        <div class="relative py-4">
          <div class="absolute inset-0 flex items-center"><span class="w-full border-t border-gray-200"></span></div>
          <div class="relative flex justify-center text-xs uppercase"><span class="bg-white px-2 text-gray-400">或</span></div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <button @click="handleSocialAuth('Google')" class="flex items-center justify-center py-3 border-2 border-gray-100 rounded-2xl hover:bg-gray-50 font-bold text-gray-600 transition-all">
            <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" class="w-5 h-5 mr-2" /> Google
          </button>
          <button @click="handleSocialAuth('Facebook')" class="flex items-center justify-center py-3 border-2 border-gray-100 rounded-2xl hover:bg-gray-50 font-bold text-gray-600 transition-all">
            <img src="https://upload.wikimedia.org/wikipedia/commons/b/b8/2021_Facebook_icon.svg" class="w-5 h-5 mr-2" /> FB
          </button>
        </div>
      </div>

      <div v-else class="space-y-4 animate-in">
        <div v-if="regStep === 'START'" class="space-y-4">
          <p v-if="isSocialMode" class="text-indigo-600 font-bold text-center">首次登入，請完成手機綁定</p>
          <input v-model="form.phoneNumber" type="tel" placeholder="輸入手機號碼 (+8869...)" class="w-full px-5 py-4 rounded-2xl border border-gray-100 bg-gray-50 outline-none focus:ring-4 focus:ring-indigo-100" />
          <button @click="sendOtp" :disabled="isLoading" class="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black shadow-xl hover:bg-indigo-700 transition-all">
            {{ isLoading ? '檢查中...' : '檢查並發送驗證碼' }}
          </button>
        </div>

        <div v-if="regStep === 'OTP'" class="space-y-4">
          <p class="text-sm text-gray-500">已發送驗證碼至 {{ form.phoneNumber }}</p>
          <input v-model="form.otpCode" type="text" maxlength="6" placeholder="000000" class="w-full py-5 rounded-2xl border-2 border-indigo-200 text-center text-4xl font-mono font-bold tracking-[0.3em] text-indigo-700 outline-none" />
          <button @click="verifyOtp" class="w-full bg-emerald-500 text-white py-4 rounded-2xl font-bold shadow-xl hover:bg-emerald-600">驗證手機</button>
          <button @click="regStep = 'START'" class="w-full text-xs text-gray-400">號碼填錯了？返回</button>
        </div>

        <div v-if="regStep === 'COMPLETE'" class="space-y-4">
          <div class="bg-green-50 p-4 rounded-2xl border border-green-100 text-green-700 text-sm font-bold flex items-center">
            ✅ 手機驗證成功：{{ form.phoneNumber }}
          </div>
          <input v-model="form.name" type="text" placeholder="您的真實姓名" class="w-full px-5 py-4 rounded-2xl border border-gray-100 bg-gray-50 outline-none focus:ring-4 focus:ring-indigo-100" />
          <input v-model="form.password" type="password" placeholder="設定登入密碼" class="w-full px-5 py-4 rounded-2xl border border-gray-100 bg-gray-50 outline-none focus:ring-4 focus:ring-indigo-100" />
          <button @click="finalRegister" class="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black shadow-xl hover:bg-indigo-700">完成註冊，領取 $10,466.89</button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue';
import axios from 'axios';
import { auth, googleProvider, facebookProvider } from '../firebaseConfig';
import { signInWithPopup, RecaptchaVerifier, signInWithPhoneNumber } from "firebase/auth";

const isLoginMode = ref(true);
const regStep = ref('START');
const isLoading = ref(false);
const isSocialMode = ref(false); // 標記是否為三方登入後的補填模式

const form = reactive({ phoneNumber: '', password: '', name: '', otpCode: '' });
let confirmationResult = null;
let currentIdToken = null; // 用於儲存 Firebase 的 Token

const resetToLogin = () => {
  isLoginMode.value = true;
  regStep.value = 'START';
  isSocialMode.value = false;
};

onUnmounted(() => {
  if (window.recaptchaVerifier) { window.recaptchaVerifier.clear(); window.recaptchaVerifier = null; }
});

// ==========================================
// 🔓 登入邏輯 (傳統)
// ==========================================
const handleLogin = async () => {
  isLoading.value = true;
  try {
    const res = await axios.post('http://localhost:8081/api/auth/login', {
      phoneNumber: form.phoneNumber,
      password: form.password
    });
    handleLoginSuccess(res.data);
  } catch (err) {
    window.alert("❌ " + (err.response?.data?.msg || "登入失敗"));
  } finally { isLoading.value = false; }
};

// ==========================================
// 📝 漸進式註冊 / 綁定邏輯
// ==========================================

// 1. 檢查號碼並發送 OTP
const sendOtp = async () => {
  isLoading.value = true;
  try {
    const checkRes = await axios.get(`http://localhost:8081/api/auth/check-phone`, {
      params: { phone: form.phoneNumber }
    });

    // 💡 注意：checkRes.data 是 Result 物件，checkRes.data.data 才是那個布林值
    if (checkRes.data.data === true) {
      window.alert("⚠️ 此號碼已註冊過，請直接登入");
      isLoginMode.value = true; // 切換回登入模式
      isLoading.value = false;
      return; // 停止往下執行
    }

    // --- 只有當 data === false 時，才會執行到這裡 ---
    console.log("號碼可用，準備發送簡訊...");

    if (window.recaptchaVerifier) { window.recaptchaVerifier.clear(); }
    window.recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', { 'size': 'invisible' });

    const result = await signInWithPhoneNumber(auth, form.phoneNumber, window.recaptchaVerifier);
    confirmationResult = result;
    regStep.value = 'OTP'; // 切換到輸入驗證碼畫面
    window.alert("✅ 驗證碼已發送");

  } catch (err) {
    console.error("檢查失敗:", err);
    window.alert("系統錯誤或網路異常");
  } finally {
    isLoading.value = false;
  }
};

// 2. 驗證 OTP
const verifyOtp = async () => {
  try {
    const result = await confirmationResult.confirm(form.otpCode);
    currentIdToken = await result.user.getIdToken();
    regStep.value = 'COMPLETE';
    window.alert("✅ 驗證成功");
  } catch (err) { window.alert("驗證碼錯誤"); }
};

// 3. 提交最終註冊 (兼容三方補填)
const finalRegister = async () => {
  try {
    // 💡 這裡要打的是 social-login 接口，不是一般的 register
    const res = await axios.post('http://localhost:8081/api/auth/social-login', {
      idToken: "MOCK_TOKEN", // 或者真正的 Firebase Token
      phoneNumber: form.phoneNumber, // 剛剛驗證的手機
      name: form.name
    });

    if (res.data.code === '200') {
      window.alert("🎊 綁定成功！一萬塊入帳");
      handleLoginSuccess(res.data);
    }
  } catch (err) {
    window.alert("註冊失敗：" + err.response?.data?.msg);
  }
};

// ==========================================
// 🎨 三方登入 (先辨認新老用戶)
// ==========================================
const handleSocialAuth = async (type) => {
  const provider = type === 'Google' ? googleProvider : facebookProvider;
  try {
    const result = await signInWithPopup(auth, provider);
    currentIdToken = await result.user.getIdToken();

    // 💡 嘗試登入 (此時不傳 phoneNumber)
    const res = await axios.post('http://localhost:8081/api/auth/social-login', {
      idToken: currentIdToken,
      name: result.user.displayName
    });

    if (res.data.code === '200') {
      handleLoginSuccess(res.data); // 老用戶直接進入
    } else if (res.data.code === '201') {
      // 💡 新用戶：進入補填手機模式
      isSocialMode.value = true;
      isLoginMode.value = false;
      regStep.value = 'START';
      window.alert("👋 歡迎！請完成手機綁定以領取 $10,466.89");
    }
  } catch (err) { window.alert(type + " 授權失敗"); }
};

const handleLoginSuccess = (result) => {
  const userData = result.data;
  if (userData && userData.token) {
    localStorage.setItem('token', userData.token);
    window.alert(`🎉 成功！\n名稱: ${userData.name}\n餘額: $${userData.balance}`);
    // router.push('/home');
  }
};
</script>