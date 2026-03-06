<template>
  <div class="app-card" :class="{ 'app-card--featured': featured }">
    <div class="app-preview">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="app-placeholder">
        <img src="@/assets/babystep-logo.svg" alt="placeholder" />
      </div>
      <div class="app-badge">{{ featured ? '精选案例' : '我的应用' }}</div>
      <div class="app-overlay">
        <a-space>
          <a-button type="primary" @click="handleViewChat">查看对话</a-button>
          <a-button v-if="app.deployKey" @click="handleViewWork">查看作品</a-button>
        </a-space>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-left">
        <a-avatar :src="app.user?.userAvatar" :size="42">
          {{ app.user?.userName?.charAt(0) || 'U' }}
        </a-avatar>
      </div>
      <div class="app-info-right">
        <h3 class="app-title">{{ app.appName || '未命名应用' }}</h3>
        <p class="app-author">
          {{ app.user?.userName || (featured ? '官方推荐' : '未知用户') }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()

const handleViewChat = () => {
  emit('view-chat', props.app.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}
</script>

<style scoped>
.app-card {
  overflow: hidden;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--bs-border);
  box-shadow: 0 18px 36px rgba(73, 102, 84, 0.1);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 26px 46px rgba(73, 102, 84, 0.16);
  border-color: rgba(87, 123, 102, 0.3);
}

.app-card--featured {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(236, 246, 239, 0.84));
}

.app-preview {
  position: relative;
  height: 214px;
  overflow: hidden;
  background:
    radial-gradient(circle at 20% 22%, rgba(142, 208, 224, 0.45), transparent 25%),
    linear-gradient(180deg, #eff7f0 0%, #dcebdc 100%);
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.app-placeholder img {
  width: 120px;
  height: 120px;
  object-fit: contain;
  opacity: 0.94;
}

.app-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--bs-primary-deep);
  font-size: 12px;
  font-weight: 700;
}

.app-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 18px;
  background: linear-gradient(180deg, rgba(36, 52, 43, 0.04), rgba(36, 52, 43, 0.52));
  opacity: 0;
  transition: opacity 0.22s ease;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.app-info {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 18px 20px;
}

.app-info-right {
  min-width: 0;
  flex: 1;
}

.app-title {
  margin: 0 0 6px;
  font-size: 18px;
  color: var(--bs-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-author {
  margin: 0;
  color: var(--bs-text-soft);
}
</style>
