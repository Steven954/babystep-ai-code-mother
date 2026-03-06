<template>
  <div class="header-shell">
    <a-layout-header class="header panel-card">
      <a-row :wrap="false" align="middle" class="header-row">
        <a-col flex="320px">
          <RouterLink to="/" class="brand-link">
            <div class="header-left">
              <img class="logo" src="@/assets/babystep-logo.svg" alt="babystep logo" />
              <div class="brand-copy">
                <span class="brand-mark">Baby Step</span>
                <h1 class="site-title">babystep应用生成</h1>
              </div>
            </div>
          </RouterLink>
        </a-col>
        <a-col flex="auto">
          <a-menu
            v-model:selectedKeys="selectedKeys"
            mode="horizontal"
            :items="menuItems"
            @click="handleMenuClick"
          />
        </a-col>
        <a-col flex="144px">
          <div class="user-login-status">
            <div v-if="loginUserStore.loginUser.id">
              <a-dropdown>
                <a-space class="user-chip">
                  <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                  {{ loginUserStore.loginUser.userName ?? '无名' }}
                </a-space>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="doLogout">
                      <LogoutOutlined />
                      退出登录
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
            <div v-else>
              <a-button type="primary" href="/user/login">登录</a-button>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-layout-header>
  </div>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { HomeOutlined, LogoutOutlined, MessageOutlined } from '@ant-design/icons-vue'
import { userLogout } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const selectedKeys = ref<string[]>(['/'])

router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    icon: () => h(MessageOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/admin/chatManage',
    label: '对话管理',
    title: '对话管理',
  },
]

const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  if (key.startsWith('/')) {
    router.push(key)
  }
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header-shell {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 18px 20px 0;
}

.header {
  width: min(1280px, calc(100vw - 40px));
  margin: 0 auto;
  height: 88px;
  line-height: 1;
  padding: 0 22px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(236, 246, 239, 0.88)) !important;
}

.header-row {
  height: 100%;
}

.brand-link {
  display: block;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.logo {
  width: 58px;
  height: 58px;
  object-fit: contain;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.8);
  padding: 4px;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.brand-mark {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--bs-text-soft);
}

.site-title {
  margin: 0;
  color: var(--bs-primary-deep);
  font-size: 22px;
  font-weight: 800;
  white-space: nowrap;
}

.user-login-status {
  display: flex;
  justify-content: flex-end;
}

.user-chip {
  min-height: 46px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--bs-border);
  color: var(--bs-primary-deep);
}

:deep(.ant-menu) {
  background: transparent !important;
  border-bottom: none !important;
  color: var(--bs-text-soft);
}

:deep(.ant-menu-horizontal > .ant-menu-item) {
  top: 0;
  height: 48px;
  line-height: 48px;
  border-radius: 999px;
  margin-inline: 6px;
  border-bottom: none !important;
}

:deep(.ant-menu-horizontal > .ant-menu-item-selected) {
  background: rgba(109, 180, 133, 0.14);
  color: var(--bs-primary-deep) !important;
}

@media (max-width: 900px) {
  .header-shell {
    padding: 12px 12px 0;
  }

  .header {
    width: calc(100vw - 24px);
    padding: 0 14px;
    height: auto;
  }

  .header-row {
    flex-wrap: wrap !important;
    gap: 12px;
    padding: 14px 0;
  }
}
</style>
