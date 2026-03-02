<template>
  <div class="reset-container">
    <h2>設定新密碼</h2>
    <input v-model="newPassword" type="password" placeholder="請輸入新密碼" />
    <button @click="handleReset">確認修改</button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';

const route = useRoute();
const newPassword = ref('');
const token = ref('');

onMounted(() => {
  // 💡 直接從 route.query 拿，不要加 .value
  if (route.query && route.query.token) {
    token.value = route.query.token;
    console.log("抓到的 Token:", token.value);
  } else {
    alert("無效的連結，找不到 Token！");
  }
});

const handleReset = async () => {
  try {
    // 💡 呼叫你寫好的 POST /api/auth/reset-password-complete
    await axios.post('http://localhost:8081/api/auth/reset-password-complete', {
      token: token.value,
      newPassword: newPassword.value
    });
    alert("密碼重設成功，請重新登入！");
  } catch (error) {
    alert("重設失敗：" + error.response.data.message);
  }
};
</script>