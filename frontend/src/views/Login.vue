<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-head">
        <el-icon class="logo"><Shop /></el-icon>
        <h1>乡镇集市摊位抽签收费<br/>食品留样与投诉平台</h1>
        <p>公开抽签 · 分区管理 · 留痕可溯 · 食安守护</p>
      </div>
      <el-form @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" size="large" placeholder="密码"
                    :prefix-icon="Lock" show-password @keyup.enter="onLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider>演示账号</el-divider>
      <div class="accounts">
        <el-tag v-for="a in accounts" :key="a.u" class="acct" :type="a.type"
                @click="fill(a.u, a.p)">
          {{ a.label }}：{{ a.u }} / {{ a.p }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import api from '../api.js'
import { store } from '../store.js'

const router = useRouter()
const form = reactive({ username: 'staff', password: 'staff' })
const loading = ref(false)

const accounts = [
  { label: '乡镇管理者', u: 'admin', p: 'admin', type: 'danger' },
  { label: '市管员', u: 'staff', p: 'staff', type: 'warning' },
  { label: '卤味摊主', u: 'wanglu', p: '123456', type: 'success' },
  { label: '蔬菜老摊主', u: 'chenfang', p: '123456', type: 'success' },
  { label: '活禽摊主', u: 'sunqiang', p: '123456', type: 'success' },
  { label: '新摊主', u: 'zhengli', p: '123456', type: 'info' }
]
function fill(u, p) { form.username = u; form.password = p }

async function onLogin() {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const data = await api.post('/auth/login', { username: form.username, password: form.password })
    localStorage.setItem('token', data.token)
    store.setUser(data.user)
    ElMessage.success(`欢迎，${data.user.name}`)
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f3a2e 0%, #35694b 60%, #4a8c63 100%); }
.login-card { width: 430px; background: #fff; border-radius: 14px; padding: 34px 38px 28px;
  box-shadow: 0 18px 60px rgba(0,0,0,.25); }
.login-head { text-align: center; margin-bottom: 20px; }
.logo { font-size: 44px; color: #2f6b47; }
h1 { font-size: 20px; color: #1f3a2e; margin: 8px 0 6px; line-height: 1.4; }
.login-head p { color: #999; font-size: 13px; margin: 0; }
.accounts { display: flex; flex-wrap: wrap; gap: 8px; }
.acct { cursor: pointer; }
</style>
