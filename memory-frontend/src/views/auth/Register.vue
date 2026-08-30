<!-- Register.vue -->
<template>
  <div class="auth-container">
    <div class="brand-section">
      <h1 class="logo">Memory Space</h1>
      <p class="slogan">开启您的视觉珍藏</p>
    </div>
    <RegisterForm :showfoot="true"/>
    
  </div>
</template>
<script setup lang="ts">
import RegisterForm from '@/components/auth/RegisterForm.vue';
</script>

<style scoped>
.auth-container {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-image: url('@/assets/image/backgroud/login-bg.png');
  /* 添加背景图片 */
  background-size: cover;
  background-position: center;
  position: relative;
}

.auth-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.1);
  /* 半透明遮罩 */
  backdrop-filter: blur(5px);
}

.brand-section {
  text-align: center;
  margin-bottom: 40px;
  z-index: 1;
}

.brand-section .logo {
  color: white;
  font-size: 3.5rem;
  font-weight: 300;
  letter-spacing: 2px;
  margin-bottom: 1rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.brand-section .slogan {
  color: rgba(255, 255, 255, 0.9);
  font-size: 1.2rem;
  margin: 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

</style>
