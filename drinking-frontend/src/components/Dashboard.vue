<template>
  <div class="dashboard-wrapper">
    <div class="sidebar">
      <div class="sidebar-header">
        <h2>👋 {{ name }}</h2>
        <p class="role-badge">{{ role === 'STORES' ? '🏪 店家' : '👤 一般用戶' }}</p>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/store-edit" class="nav-item">
          🏪 編輯店家資料
        </router-link>
        <router-link to="/MenuBatchUpload" class="nav-item">
          🍹 上傳飲品菜單
        </router-link>
      </nav>

      <button @click="handleLogout" class="btn-logout">
        🚪 登出
      </button>
    </div>

    <div class="main-content">
      <h3>歡迎回來，{{ name }}！</h3>
      <p class="hint">請從左側選單選擇功能</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// 從 localStorage 拿登入資訊
const name = ref(localStorage.getItem('name') || '用戶');
const role = ref(localStorage.getItem('role') || 'USERS');

const handleLogout = () => {
  // 清除所有登入相關的緩存
  localStorage.removeItem('token');
  localStorage.removeItem('name');
  localStorage.removeItem('role');
  localStorage.removeItem('userId');
  localStorage.removeItem('storeStatus');

  alert('已成功登出！');
  router.push('/'); // 跳回登入頁
};
</script>

<style scoped>
.dashboard-wrapper {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 240px;
  background: #1e293b;
  color: white;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  gap: 12px;
}

.sidebar-header {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #334155;
}

.sidebar-header h2 {
  margin: 0 0 6px 0;
  font-size: 1.1rem;
}

.role-badge {
  margin: 0;
  font-size: 0.8rem;
  color: #94a3b8;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.nav-item {
  display: block;
  padding: 12px 16px;
  border-radius: 8px;
  color: #cbd5e1;
  text-decoration: none;
  transition: background 0.2s;
}

.nav-item:hover,
.nav-item.router-link-active {
  background: #3b82f6;
  color: white;
}

.btn-logout {
  width: 100%;
  padding: 12px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s;
}

.btn-logout:hover {
  background: #dc2626;
}

.main-content {
  flex: 1;
  padding: 40px;
  background: #f8fafc;
}

.hint {
  color: #94a3b8;
}
</style>