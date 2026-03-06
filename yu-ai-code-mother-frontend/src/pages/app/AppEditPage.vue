<template>
  <div id="appEditPage" class="page-shell">
    <section class="page-header panel-card panel-card--soft">
      <div>
        <span class="section-kicker">Edit Workspace</span>
        <h1>编辑应用信息</h1>
        <p>保留原来的编辑能力，只把信息结构整理成更清晰的双区块工作台。</p>
      </div>
    </section>

    <div class="page-grid page-grid--2">
      <section class="panel-card form-panel">
        <div class="section-heading">
          <div>
            <h2>基本信息</h2>
          </div>
        </div>
        <a-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item label="应用名称" name="appName">
            <a-input v-model:value="formData.appName" placeholder="请输入应用名称" :maxlength="50" show-count />
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="应用封面"
            name="cover"
            extra="支持图片链接，建议尺寸 400x300"
          >
            <a-input v-model:value="formData.cover" placeholder="请输入封面图片链接" />
            <div v-if="formData.cover" class="cover-preview">
              <a-image
                :src="formData.cover"
                :width="220"
                :height="150"
                fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
              />
            </div>
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="优先级"
            name="priority"
            extra="设置为 99 表示精选应用"
          >
            <a-input-number v-model:value="formData.priority" :min="0" :max="99" style="width: 220px" />
          </a-form-item>

          <a-form-item label="初始提示词" name="initPrompt">
            <a-textarea
              v-model:value="formData.initPrompt"
              placeholder="请输入初始提示词"
              :rows="4"
              :maxlength="1000"
              show-count
              disabled
            />
            <div class="form-tip">初始提示词不可修改</div>
          </a-form-item>

          <a-form-item label="生成类型" name="codeGenType">
            <a-input :value="formatCodeGenType(formData.codeGenType)" placeholder="生成类型" disabled />
            <div class="form-tip">生成类型不可修改</div>
          </a-form-item>

          <a-form-item v-if="formData.deployKey" label="部署密钥" name="deployKey">
            <a-input v-model:value="formData.deployKey" placeholder="部署密钥" disabled />
            <div class="form-tip">部署密钥不可修改</div>
          </a-form-item>

          <a-form-item>
            <a-space wrap>
              <a-button type="primary" html-type="submit" :loading="submitting">保存修改</a-button>
              <a-button @click="resetForm">重置</a-button>
              <a-button type="link" @click="goToChat">进入对话</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </section>

      <section class="panel-card info-panel">
        <div class="section-heading">
          <div>
            <h2>应用信息</h2>
          </div>
        </div>
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="应用 ID">
            {{ appInfo?.id }}
          </a-descriptions-item>
          <a-descriptions-item label="创建者">
            <UserInfo :user="appInfo?.user" size="small" />
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatTime(appInfo?.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ formatTime(appInfo?.updateTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="部署时间">
            {{ appInfo?.deployedTime ? formatTime(appInfo.deployedTime) : '未部署' }}
          </a-descriptions-item>
          <a-descriptions-item label="访问链接">
            <a-button v-if="appInfo?.deployKey" type="link" @click="openPreview" size="small">
              查看预览
            </a-button>
            <span v-else>未部署</span>
          </a-descriptions-item>
        </a-descriptions>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import UserInfo from '@/components/UserInfo.vue'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController'
import { getStaticPreviewUrl } from '@/config/env'
import { useLoginUserStore } from '@/stores/loginUser'
import { formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appInfo = ref<API.AppVO>()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
})

const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

const rules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { min: 1, max: 50, message: '应用名称长度应在 1 到 50 个字符之间', trigger: 'blur' },
  ],
  cover: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  priority: [{ type: 'number', min: 0, max: 99, message: '优先级范围是 0 到 99', trigger: 'blur' }],
}

const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用 ID 不存在')
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      if (!isAdmin.value && appInfo.value.userId !== loginUserStore.loginUser.id) {
        message.error('您没有权限编辑此应用')
        router.push('/')
        return
      }

      formData.appName = appInfo.value.appName || ''
      formData.cover = appInfo.value.cover || ''
      formData.priority = appInfo.value.priority || 0
      formData.initPrompt = appInfo.value.initPrompt || ''
      formData.codeGenType = appInfo.value.codeGenType || ''
      formData.deployKey = appInfo.value.deployKey || ''
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败:', error)
    message.error('获取应用信息失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!appInfo.value?.id) return

  submitting.value = true
  try {
    let res
    if (isAdmin.value) {
      res = await updateAppByAdmin({
        id: appInfo.value.id,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority,
      })
    } else {
      res = await updateApp({
        id: appInfo.value.id,
        appName: formData.appName,
      })
    }

    if (res.data.code === 0) {
      message.success('修改成功')
      await fetchAppInfo()
    } else {
      message.error('修改失败，' + res.data.message)
    }
  } catch (error) {
    console.error('修改失败:', error)
    message.error('修改失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  if (appInfo.value) {
    formData.appName = appInfo.value.appName || ''
    formData.cover = appInfo.value.cover || ''
    formData.priority = appInfo.value.priority || 0
  }
  formRef.value?.clearValidate()
}

const goToChat = () => {
  if (appInfo.value?.id) {
    router.push(`/app/chat/${appInfo.value.id}`)
  }
}

const openPreview = () => {
  if (appInfo.value?.codeGenType && appInfo.value?.id) {
    const url = getStaticPreviewUrl(appInfo.value.codeGenType, String(appInfo.value.id))
    window.open(url, '_blank')
  }
}

onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
#appEditPage {
  display: grid;
  gap: 20px;
}

.page-header,
.form-panel,
.info-panel {
  padding: 26px;
}

.page-header h1,
.form-panel h2,
.info-panel h2 {
  margin: 0;
}

.page-header p {
  margin: 10px 0 0;
  color: var(--bs-text-soft);
}

.cover-preview {
  margin-top: 14px;
  padding: 14px;
  border-radius: 22px;
  background: rgba(236, 246, 239, 0.72);
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--bs-text-soft);
}
</style>
