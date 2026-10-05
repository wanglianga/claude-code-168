<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2>农村集市摊位管理平台</h2>
      <p class="sub">摊位抽签 · 收费 · 食品留样 · 投诉追溯</p>
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" @keyup.enter="doLogin">
            <el-form-item><el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" /></el-form-item>
            <el-form-item><el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password /></el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loading" @click="doLogin">登 录</el-button>
          </el-form>
          <div class="demo-accounts">
            <div class="demo-title">演示账号（点击填充）</div>
            <el-tag v-for="a in demoAccounts" :key="a.u" class="demo-tag" @click="fill(a)">{{ a.label }}</el-tag>
          </div>
        </el-tab-pane>
        <el-tab-pane label="摊主注册" name="register">
          <el-form :model="regForm">
            <el-form-item><el-input v-model="regForm.username" placeholder="用户名" prefix-icon="User" /></el-form-item>
            <el-form-item><el-input v-model="regForm.name" placeholder="姓名/摊位名称" prefix-icon="Avatar" /></el-form-item>
            <el-form-item><el-input v-model="regForm.phone" placeholder="手机号" prefix-icon="Iphone" /></el-form-item>
            <el-form-item><el-input v-model="regForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password /></el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loading" @click="doRegister">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div class="complaint-link">
        消费者无需注册，<el-link type="primary" @click="$router.push('/complaint')">点我提交投诉</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const tab = ref('login')
const loading = ref(false)
const loginForm = ref({ username: '', password: '' })
const regForm = ref({ username: '', name: '', phone: '', password: '' })

const demoAccounts = [
  { label: '乡镇管理员', u: 'admin', p: 'admin123' },
  { label: '工作人员', u: 'staff1', p: 'staff123' },
  { label: '摊主-王老二(熟食)', u: 'vendor1', p: 'vendor123' },
  { label: '摊主-赵四(果蔬)', u: 'vendor4', p: 'vendor123' }
]

function fill(a) {
  loginForm.value = { username: a.u, password: a.p }
}

async function doLogin() {
  if (!loginForm.value.username || !loginForm.value.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const res = await api.post('/auth/login', loginForm.value)
    auth.setAuth(res.token, res.user)
    router.push('/home')
  } finally { loading.value = false }
}

async function doRegister() {
  if (!regForm.value.username || !regForm.value.password || !regForm.value.name) {
    return ElMessage.warning('请填写完整信息')
  }
  loading.value = true
  try {
    const res = await api.post('/auth/register', regForm.value)
    auth.setAuth(res.token, res.user)
    ElMessage.success('注册成功')
    router.push('/home')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1f3a5f 0%, #2e5c3f 100%); }
.login-card { width: 420px; }
h2 { text-align: center; color: #303133; }
.sub { text-align: center; color: #909399; font-size: 13px; margin: 8px 0 16px; }
.demo-accounts { margin-top: 16px; }
.demo-title { font-size: 12px; color: #909399; margin-bottom: 6px; }
.demo-tag { margin: 0 6px 6px 0; cursor: pointer; }
.complaint-link { margin-top: 16px; text-align: center; font-size: 13px; color: #606266; }
</style>
