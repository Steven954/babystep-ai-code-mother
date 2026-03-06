<template>
  <a-layout class="basic-layout">
    <div class="backdrop backdrop-left"></div>
    <div class="backdrop backdrop-right"></div>
    <GlobalHeader />
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: transparent;
  position: relative;
  overflow: hidden;
}

.main-content {
  position: relative;
  z-index: 2;
  width: 100%;
  background: transparent;
}

.backdrop {
  position: fixed;
  border-radius: 999px;
  filter: blur(40px);
  pointer-events: none;
  opacity: 0.7;
  z-index: 0;
}

.backdrop-left {
  top: 90px;
  left: -120px;
  width: 320px;
  height: 320px;
  background: rgba(109, 180, 133, 0.18);
}

.backdrop-right {
  top: 220px;
  right: -110px;
  width: 280px;
  height: 280px;
  background: rgba(142, 208, 224, 0.2);
}
</style>
