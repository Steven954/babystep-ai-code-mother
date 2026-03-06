<template>
  <div class="auth-shell">
    <div class="auth-grid">
      <section class="auth-brand panel-card panel-card--soft">
        <span class="section-kicker">Create Account</span>
        <h1>注册 babystep，把你的想法保存成可以继续迭代的应用工作流。</h1>
        <p>注册成功后功能不变，依然可以创建应用、进入对话和后续部署。</p>
        <div class="brand-visual">
          <img src="@/assets/babystep-logo.svg" alt="babystep logo" />
        </div>
      </section>

      <section id="userRegisterPage" class="auth-card panel-card">
        <span class="section-kicker">Sign Up</span>
        <h2 class="title">babystep应用生成</h2>
        <p class="desc">创建账号后，你的应用生成记录和调试对话都会保存在个人空间里。</p>
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
          <a-form-item
            name="checkPassword"
            label="确认密码"
            :rules="[
              { required: true, message: '请确认密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
              { validator: validateCheckPassword },
            ]"
          >
            <a-input-password v-model:value="formState.checkPassword" placeholder="请再次输入密码" />
          </a-form-item>
          <div class="tips">
            已有账号？
            <RouterLink to="/user/login">去登录</RouterLink>
          </div>
          <a-form-item>
            <a-button type="primary" html-type="submit" block>注册</a-button>
          </a-form-item>
        </a-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (
  rule: unknown,
  value: string,
  callback: (error?: Error) => void,
) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
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
