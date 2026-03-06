<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import AppCard from '@/components/AppCard.vue'
import { getDeployUrl } from '@/config/env'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const userPrompt = ref('')
const creating = ref(false)

const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      await router.push(`/app/chat/${String(res.data.data)}`)
    } else {
      message.error('创建失败，' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败:', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    myApps.value = []
    myAppsPage.total = 0
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败:', error)
  }
}

const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败:', error)
  }
}

const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    window.open(getDeployUrl(app.deployKey), '_blank')
  }
}

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<template>
  <div id="homePage" class="page-shell">
    <section class="hero panel-card panel-card--soft">
      <div class="hero-copy">
        <span class="section-kicker">AI App Studio</span>
        <h1 class="hero-title">
          <span>把想法交给 AI</span>
          <span>把应用留给 babystep</span>
        </h1>
        <p>从创意输入到应用生成、预览、迭代修改，整个流程都可以在这里完成。</p>
        <div class="hero-metrics">
          <div class="metric">
            <strong>{{ myAppsPage.total }}</strong>
            <span>我的应用</span>
          </div>
          <div class="metric">
            <strong>{{ featuredAppsPage.total }}</strong>
            <span>精选案例</span>
          </div>
          <div class="metric">
            <strong>AI</strong>
            <span>即时生成</span>
          </div>
        </div>
      </div>
      <div class="hero-art">
        <div class="art-card">
          <img src="@/assets/babystep-logo.svg" alt="babystep logo" />
          <div class="art-copy">
            <span>Babystep Path</span>
            <p>从灵感出发，逐步生成、调整并完善你的应用体验。</p>
          </div>
        </div>
      </div>
    </section>

    <section class="composer panel-card">
      <div class="section-heading">
        <div>
          <span class="section-kicker">Prompt Composer</span>
          <h2>从这里开始创建你的下一个应用</h2>
        </div>
      </div>
      <div class="composer-box">
        <a-textarea
          v-model:value="userPrompt"
          placeholder="例如：帮我生成一个面向家长的成长记录网站，包含时间线、照片墙、成长瞬间和移动端适配。"
          :rows="5"
          :maxlength="1000"
          class="prompt-input"
        />
        <div class="composer-actions">
          <a-button type="primary" size="large" @click="createApp" :loading="creating">
            开始生成
          </a-button>
        </div>
      </div>
      <div class="prompt-presets">
        <a-button
          @click="
            setPrompt(
              '创建一个儿童成长记录平台，包含成长时间轴、相册、每月变化记录、家人寄语和可打印成长卡片。',
            )
          "
        >
          成长记录站
        </a-button>
        <a-button
          @click="
            setPrompt(
              '搭建一个清爽的作品集网站，包含项目展示、服务介绍、案例详情、预约咨询和响应式布局。',
            )
          "
        >
          作品集网站
        </a-button>
        <a-button
          @click="
            setPrompt(
              '做一个温和治愈风格的企业官网，包含品牌故事、服务矩阵、团队介绍、案例展示和联系入口。',
            )
          "
        >
          品牌官网
        </a-button>
        <a-button
          @click="
            setPrompt(
              '做一个带购物车、订单流程、优惠活动和会员体系的简洁电商首页，适配手机和桌面端。',
            )
          "
        >
          电商体验页
        </a-button>
      </div>
    </section>

    <section class="content-section">
      <div class="section-heading">
        <div>
          <span class="section-kicker">My Workspace</span>
          <h2>我的作品</h2>
        </div>
      </div>
      <div v-if="myApps.length" class="card-grid">
        <AppCard
          v-for="app in myApps"
          :key="app.id"
          :app="app"
          @view-chat="viewChat"
          @view-work="viewWork"
        />
      </div>
      <div v-else class="empty-state">
        <p>还没有应用。先输入一个需求，让 babystep 帮你起第一步。</p>
      </div>
      <div class="pagination-wrapper" v-if="myAppsPage.total > myAppsPage.pageSize">
        <a-pagination
          v-model:current="myAppsPage.current"
          v-model:page-size="myAppsPage.pageSize"
          :total="myAppsPage.total"
          :show-size-changer="false"
          :show-total="(total: number) => `共 ${total} 个应用`"
          @change="loadMyApps"
        />
      </div>
    </section>

    <section class="content-section">
      <div class="section-heading">
        <div>
          <span class="section-kicker">Featured</span>
          <h2>精选案例</h2>
        </div>
      </div>
      <div v-if="featuredApps.length" class="card-grid">
        <AppCard
          v-for="app in featuredApps"
          :key="app.id"
          :app="app"
          :featured="true"
          @view-chat="viewChat"
          @view-work="viewWork"
        />
      </div>
      <div v-else class="empty-state">
        <p>暂时还没有精选案例。</p>
      </div>
      <div class="pagination-wrapper" v-if="featuredAppsPage.total > featuredAppsPage.pageSize">
        <a-pagination
          v-model:current="featuredAppsPage.current"
          v-model:page-size="featuredAppsPage.pageSize"
          :total="featuredAppsPage.total"
          :show-size-changer="false"
          :show-total="(total: number) => `共 ${total} 个案例`"
          @change="loadFeaturedApps"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
#homePage {
  display: grid;
  gap: 24px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.9fr);
  gap: 24px;
  padding: 34px;
  overflow: hidden;
  position: relative;
}

.hero::after {
  content: '';
  position: absolute;
  right: -60px;
  bottom: -140px;
  width: 420px;
  height: 220px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(109, 180, 133, 0.24), rgba(142, 208, 224, 0.12));
  transform: rotate(-10deg);
}

.hero-copy {
  position: relative;
  z-index: 1;
}

.hero-title {
  margin: 16px 0 14px;
  display: grid;
  gap: 6px;
  max-width: 11ch;
  font-size: clamp(30px, 4.6vw, 52px);
  line-height: 1.08;
  letter-spacing: -0.04em;
  font-weight: 700;
}

.hero-title span:last-child {
  color: var(--bs-primary-deep);
}

.hero-copy p {
  margin: 0;
  max-width: 650px;
  color: var(--bs-text-soft);
  font-size: 17px;
  line-height: 1.8;
}

.hero-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 28px;
}

.metric {
  min-width: 120px;
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--bs-border);
}

.metric strong {
  display: block;
  font-size: 28px;
  color: var(--bs-primary-deep);
}

.metric span {
  color: var(--bs-text-soft);
}

.hero-art {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.art-card {
  width: min(100%, 360px);
  padding: 24px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--bs-border);
  box-shadow: 0 22px 44px rgba(73, 102, 84, 0.12);
}

.art-card img {
  width: 100%;
  display: block;
}

.art-copy {
  padding-top: 12px;
}

.art-copy span {
  color: var(--bs-primary-deep);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
}

.art-copy p {
  margin: 8px 0 0;
  color: var(--bs-text-soft);
  line-height: 1.7;
}

.composer,
.content-section {
  padding: 28px;
  border-radius: var(--bs-radius-xl);
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid var(--bs-border);
  box-shadow: var(--bs-shadow);
}

.composer-box {
  position: relative;
}

.prompt-input :deep(textarea) {
  min-height: 170px;
  padding: 22px;
  font-size: 16px;
  line-height: 1.8;
  background: rgba(255, 255, 255, 0.85);
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.prompt-presets {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.prompt-presets .ant-btn {
  height: auto;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(236, 246, 239, 0.9);
  color: var(--bs-primary-deep);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

@media (max-width: 960px) {
  .hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero,
  .composer,
  .content-section {
    padding: 20px;
  }

  .hero-metrics {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
