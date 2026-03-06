<template>
  <div id="chatManagePage" class="page-shell">
    <section class="toolbar-card panel-card">
      <div class="section-heading">
        <div>
          <span class="section-kicker">Admin Console</span>
          <h1>对话管理</h1>
        </div>
      </div>
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="消息内容">
          <a-input v-model:value="searchParams.message" placeholder="输入消息内容" />
        </a-form-item>
        <a-form-item label="消息类型">
          <a-select v-model:value="searchParams.messageType" placeholder="选择消息类型" style="width: 140px">
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="user">用户消息</a-select-option>
            <a-select-option value="assistant">AI 消息</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="应用 ID">
          <a-input v-model:value="searchParams.appId" placeholder="输入应用 ID" />
        </a-form-item>
        <a-form-item label="用户 ID">
          <a-input v-model:value="searchParams.userId" placeholder="输入用户 ID" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>
    </section>

    <section class="data-card panel-card">
      <a-table
        :columns="columns"
        :data-source="data"
        :pagination="pagination"
        @change="doTableChange"
        :scroll="{ x: 1320 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'message'">
            <a-tooltip :title="record.message">
              <div class="message-text">{{ record.message }}</div>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'messageType'">
            <a-tag :color="record.messageType === 'user' ? 'blue' : 'green'">
              {{ record.messageType === 'user' ? '用户消息' : 'AI 消息' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="primary" size="small" @click="viewAppChat(record.appId)">查看对话</a-button>
              <a-popconfirm title="确定要删除这条消息吗？" @confirm="deleteMessage(record.id)">
                <a-button danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import { formatTime } from '@/utils/time'

const router = useRouter()

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80, fixed: 'left' },
  { title: '消息内容', dataIndex: 'message', width: 320 },
  { title: '消息类型', dataIndex: 'messageType', width: 110 },
  { title: '应用 ID', dataIndex: 'appId', width: 90 },
  { title: '用户 ID', dataIndex: 'userId', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' },
]

const data = ref<API.ChatHistory[]>([])
const total = ref(0)

const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const fetchData = async () => {
  try {
    const res = await listAllChatHistoryByPageForAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    message.error('获取数据失败')
  }
}

onMounted(() => {
  fetchData()
})

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`,
}))

const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const viewAppChat = (appId: number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}`)
  }
}

const deleteMessage = async (id: number | undefined) => {
  if (!id) return
  try {
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除失败:', error)
    message.error('删除失败')
  }
}
</script>

<style scoped>
#chatManagePage {
  display: grid;
  gap: 20px;
}

.message-text {
  max-width: 320px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
