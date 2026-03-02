<script setup>
import { RouterView } from 'vue-router'
// 💡 不再需要在這裡 import Auth，因為路由會幫你處理
</script>

<template>
  <router-view />
</template>

<style scoped>
/* 保持原樣或清空 */.auth-container { display: flex; justify-content: center; align-items: center; min-height: 90vh; font-family: sans-serif; }
.auth-card { width: 100%; max-width: 400px; padding: 30px; background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }
h2 { text-align: center; color: #333; margin-bottom: 8px; }
.subtitle { text-align: center; color: #888; font-size: 14px; margin-bottom: 24px; }
.form-group { margin-bottom: 16px; text-align: left; }
.form-group label { display: block; font-size: 14px; font-weight: bold; margin-bottom: 6px; color: #555; }
input { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 6px; box-sizing: border-box; }
.input-row { display: flex; gap: 8px; }
.input-row input { flex: 1; }
button { cursor: pointer; border-radius: 6px; transition: 0.3s; font-weight: bold; }
.primary-btn { width: 100%; padding: 14px; background: #4CAF50; color: white; border: none; margin-top: 10px; }
.secondary-btn { padding: 0 12px; background: #f0f0f0; border: 1px solid #ccc; font-size: 13px; }
.footer { margin-top: 20px; text-align: center; font-size: 14px; }
.footer a { color: #2196F3; text-decoration: none; }
.slide-in { animation: slide 0.4s ease-out; }
@keyframes slide { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>