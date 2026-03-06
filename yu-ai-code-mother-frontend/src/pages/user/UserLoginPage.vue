<template>
  <div class="auth-shell">
    <div class="auth-grid">
      <section class="auth-brand panel-card panel-card--soft">
        <span class="section-kicker">Welcome Back</span>
        <h1>登录 babystep，继续沿着你的应用路径往前走。</h1>
        <p>保留原来的登录功能，只把界面改成和新 logo 更统一的清透绿蓝配色。</p>
        <div class="brand-visual">
          <img src="@/assets/babystep-logo.svg" alt="babystep logo" />
        </div>
      </section>

      <section id="userLoginPage" class="auth-card panel-card">
        <span class="section-kicker">Sign In</span>
        <h2 class="title">babystep应用生成</h2>
        <p class="desc">不用从零搭框架，登录后直接开始生成、修改和部署应用。</p>
        <a-form :model="formState" name="basic" autocomplete="off" layout="vertical" @finish="handleSubmit">
          <a-form-item name="userAccount" label="账号" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
          </a-form-item>
          <a-form-item
            name="userPassword"
            label="密码"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
          </a-form-item>
          <div class="tips">
            没有账号？
            <RouterLink to="/user/register">去注册</RouterLink>
          </div>
          <a-form-item>
            <a-button type="primary" html-type="submit" block>登录</a-button>
          </a-form-item>
        </a-form>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: API.UserLoginRequest) => {
  const res = await userLogin(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error('登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.auth-brand,
.auth-card {
  padding: 28px;
}

.auth-brand h1 {
  margin: 16px 0 12px;
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.08;
}

.auth-brand p {
  margin: 0;
  color: var(--bs-text-soft);
  line-height: 1.8;
}

.brand-visual {
  margin-top: 28px;
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.76);
}

.brand-visual img {
  width: 100%;
  display: block;
}

.title {
  margin: 16px 0 8px;
  font-size: 32px;
}

.desc {
  margin: 0 0 24px;
  color: var(--bs-text-soft);
  line-height: 1.7;
}

.tips {
  margin-bottom: 18px;
  color: var(--bs-text-soft);
  text-align: right;
}
</style>
