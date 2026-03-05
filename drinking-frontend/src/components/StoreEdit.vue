<template>
  <div class="edit-container">
    <div class="card">
      <h3>修改店家圖片</h3>

      <div class="image-preview">
        <img v-if="imageUrl" :src="imageUrl" class="preview-img" />
        <div v-else class="no-image">目前無圖片</div>
      </div>

      <div class="upload-area">
        <input type="file" @change="onFileChange" accept="image/*" id="store-image" hidden />
        <label for="store-image" class="btn-select">
          {{ isUploading ? '上傳中...' : '更換店家圖片' }}
        </label>
      </div>

      <hr />

      <div class="input-group">
        <label>店家名稱</label>
        <input v-model="storeName" type="text" placeholder="請輸入新名稱" />
      </div>

      <button @click="submitUpdate" :disabled="isUploading || !imageUrl" class="btn-save">
        確認儲存修改
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const storeId = ref(null);
const storeName = ref('');
const imageUrl = ref('');
const isUploading = ref(false);

// 💡 頁面載入時，直接從 localStorage 拿 storeId（登入時已存好）
onMounted(async () => {
  const token = localStorage.getItem('token');
  const sid = localStorage.getItem('storeId');

  if (!token) return alert('請先登入');
  if (!sid) return alert('您尚未申請開店');

  storeId.value = sid;

  // 用 storeId 拉現有店家資料填入表單
  try {
    const res = await axios.get(`http://localhost:8081/api/stores/${sid}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    const store = res.data.data;
    storeName.value = store.name || '';
    imageUrl.value = store.imageUrl || '';
  } catch (err) {
    alert('載入店家資料失敗：' + (err.response?.data?.msg || err.message));
  }
});

// 1. 處理圖片上傳到 Cloudinary
const onFileChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;

  isUploading.value = true;

  const formData = new FormData();
  formData.append('file', file);
  formData.append('upload_preset', 'store_images');

  // 💡 讓圖片在雲端的檔名變成 "店家名稱_ID"
  // 注意：public_id 不支援中文，建議使用 ID 或 英文名稱
  const customFileName = `store_${storeId.value}`;
  formData.append('public_id', customFileName);

  try {
    const res = await axios.post(
        'https://api.cloudinary.com/v1_1/dkgv2f6gx/image/upload',
        formData
    );

    // 拿到 secure_url
    imageUrl.value = res.data.secure_url;
    alert(`圖片已上傳！雲端檔名已設定為: ${customFileName}`);
  } catch (err) {
    console.error("Cloudinary 失敗:", err.response?.data?.error?.message);
    alert("上傳失敗");
  } finally {
    isUploading.value = false;
  }
};
// 2. 將 ID 和圖片網址傳給 Java 後端
const submitUpdate = async () => {
  try {
    // 💡 只傳後端需要的：ID、名稱、圖片網址 (address 不傳)
    const payload = {
      id: storeId.value,
      name: storeName.value,
      imageUrl: imageUrl.value
    };

    const token = localStorage.getItem('token');
    const res = await axios.put('http://localhost:8081/api/stores/update', payload, {
      headers: { Authorization: `Bearer ${token}` }
    });

    if (res.data.code === "200") {
      alert("資料庫更新成功！");
    } else {
      alert("後端報錯: " + res.data.msg);
    }
  } catch (err) {
    // 💡 這裡會接到你寫的 GlobalExceptionHandler 拋出的錯誤 (如 404 或 500)
    const errorMsg = err.response?.data?.msg || "連線後端失敗";
    alert(errorMsg);
  }
};
</script>

<style scoped>
.preview-img { width: 200px; height: 150px; object-fit: cover; border-radius: 8px; }
.btn-select { background: #6c757d; color: white; padding: 8px 12px; cursor: pointer; border-radius: 4px; }
.btn-save { background: #28a745; color: white; width: 100%; padding: 12px; border: none; margin-top: 10px; cursor: pointer; }
.btn-save:disabled { background: #ccc; cursor: not-allowed; }
</style>