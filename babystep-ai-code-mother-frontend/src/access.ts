import { message } from 'ant-design-vue'
import router from '@/router'
import { useLoginUserStore } from '@/stores/loginUser'

let firstFetchLoginUser = true

/**
 * Global permission guard.
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser

  if (firstFetchLoginUser) {
    try {
      await loginUserStore.fetchLoginUser()
      loginUser = loginUserStore.loginUser
    } catch (error) {
      console.error('Failed to initialize login user:', error)
    } finally {
      firstFetchLoginUser = false
    }
  }

  if (to.fullPath.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }

  next()
})
