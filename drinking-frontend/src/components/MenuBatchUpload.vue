<template>
  <div class="batch-upload-container">
    <h3>批次上傳飲品品項</h3>

    <div class="upload-controls">
      <input
          type="file"
          multiple
          accept="image/*"
          @change="handleFileChange"
          id="file-input"
          class="hidden-input"
      />
      <label for="file-input" class="select-btn">選擇多張圖片 (最多 10 張)</label>
      <button
          @click="uploadAll"
          :disabled="items.length === 0 || isUploading"
          class="upload-all-btn"
      >
        {{ isUploading ? `上傳中 (${uploadProgress}%)` : '開始批次上傳' }}
      </button>
    </div>

    <div class="preview-grid">
      <div v-for="(item, index) in items" :key="index" class="preview-card">
        <div class="image-wrapper">
          <img :src="item.previewUrl" alt="預覽圖" />
          <button @click="removeItem(index)" class="remove-btn">×</button>
        </div>
        <div class="input-wrapper">
          <input v-model="item.name" placeholder="輸入飲料名稱" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';

const items = ref([]); // 格式: { file: File, previewUrl: string, name: string }
const isUploading = ref(false);
const uploadProgress = ref(0);

// 1. 處理檔案選擇與預覽
const handleFileChange = (e) => {
  const files = Array.from(e.target.files);
  if (items.value.length + files.length > 10) {
    return alert("一次最多上傳 10 張圖片");
  }

  files.forEach(file => {
    items.value.push({
      file: file,
      previewUrl: URL.createObjectURL(file), // 產生暫時的預覽網址
      name: file.name.split('.')[0] // 預設名稱為檔名
    });
  });
  e.target.value = ''; // 清空 input 讓同檔案可重複選
};

// 2. 移除單一項目
const removeItem = (index) => {
  URL.revokeObjectURL(items.value[index].previewUrl); // 釋放記憶體
  items.value.splice(index, 1);
};

// 3. 批次上傳邏輯 (前端直傳 Cloudinary)
const uploadAll = async () => {
  isUploading.value = true;
  uploadProgress.value = 0;

  try {
    const uploadPromises = items.value.map(async (item, index) => {
      const formData = new FormData();
      formData.append('file', item.file);
      formData.append('upload_preset', 'store_images');

      // 上傳到 Cloudinary
      const res = await axios.post(
          "https://api.cloudinary.com/v1_1/dkgv2f6gx/image/upload",
          formData
      );

      // 更新總體進度 (簡單計算)
      uploadProgress.value = Math.round(((index + 1) / items.value.length) * 100);

      return {
        name: item.name,
        imageUrl: res.data.secure_url
      };
    });

    const uploadedResults = await Promise.all(uploadPromises);

    // 4. 最後將所有網址傳給 Java 後端存入資料庫
    await axios.post("http://localhost:8081/api/stores/batch-save", {
      items: uploadedResults
    }, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    });

    alert("所有品項已成功上傳至雲端並存入資料庫！");
    items.value = []; // 清空預覽
  } catch (err) {
    console.error("上傳失敗:", err);
    alert("上傳過程中出錯，請檢查網路或 Cloudinary 設定");
  } finally {
    isUploading.value = false;
  }
};
</script>

<style scoped>
.batch-upload-container { padding: 20px; max-width: 900px; margin: auto; }
.upload-controls { margin-bottom: 20px; display: flex; gap: 10px; }
.hidden-input { display: none; }
.select-btn { background: #646cff; color: white; padding: 10px 20px; border-radius: 8px; cursor: pointer; }
.upload-all-btn { background: #42b883; color: white; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; }
.upload-all-btn:disabled { background: #ccc; cursor: not-allowed; }

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 20px;
}
.preview-card { border: 1px solid #ddd; padding: 10px; border-radius: 8px; position: relative; }
.image-wrapper img { width: 100%; height: 120px; object-fit: cover; border-radius: 4px; }
.remove-btn {
  position: absolute; top: 0; right: 0; background: red; color: white;
  border: none; border-radius: 50%; cursor: pointer; width: 24px; height: 24px;
}
.input-wrapper input { width: 100%; margin-top: 8px; padding: 5px; box-sizing: border-box; }
</style>