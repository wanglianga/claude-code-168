<template>
  <el-config-provider>
    <router-view v-if="isLogin" />
    <el-container v-else class="layout">
      <el-aside width="232px" class="aside">
        <div class="brand">
          <el-icon class="brand-icon"><Shop /></el-icon>
          <div>
            <div class="brand-title">乡镇集市管理平台</div>
            <div class="brand-sub">抽签 · 收费 · 留样 · 投诉</div>
          </div>
        </div>
        <el-scrollbar>
          <el-menu :default-active="$route.path" router class="menu"
                   :collapse-transition="false">
            <el-menu-item index="/home"><el-icon><HomeFilled /></el-icon><span>工作台</span></el-menu-item>

            <el-sub-menu v-if="isVendor" index="vendor">
              <template #title><el-icon><Goods /></el-icon><span>摊主业务</span></template>
              <el-menu-item index="/vendor/apply">赶集报名</el-menu-item>
              <el-menu-item index="/vendor/mine">我的抽签与缴费</el-menu-item>
              <el-menu-item index="/vendor/samples">食品留样</el-menu-item>
              <el-menu-item index="/vendor/records">我的巡查/信用台账</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isStaff" index="market">
              <template #title><el-icon><OfficeBuilding /></el-icon><span>市场组织</span></template>
              <el-menu-item index="/staff/fairs">集市与摊位设置</el-menu-item>
              <el-menu-item index="/staff/review">报名审核</el-menu-item>
              <el-menu-item index="/staff/draw">组织抽签</el-menu-item>
              <el-menu-item index="/staff/allocations">抽签结果与收费</el-menu-item>
              <el-menu-item index="/staff/checkin">扫码入场核验</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isStaff" index="supervise">
              <template #title><el-icon><View /></el-icon><span>巡查与食安</span></template>
              <el-menu-item index="/staff/inspections">市场巡查</el-menu-item>
              <el-menu-item index="/staff/samples">留样台账</el-menu-item>
              <el-menu-item index="/staff/complaints">投诉处理</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="isStaff" index="trace">
              <template #title><el-icon><DocumentChecked /></el-icon><span>公平留痕</span></template>
              <el-menu-item index="/staff/ledger">调整公示台账</el-menu-item>
              <el-menu-item index="/staff/credit">摊主信用管理</el-menu-item>
              <el-menu-item index="/staff/audit">操作审计日志</el-menu-item>
            </el-sub-menu>

            <el-menu-item v-if="isStaff" index="/staff/dashboard">
              <el-icon><DataAnalysis /></el-icon><span>复盘看板</span>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <el-container>
        <el-header class="header">
          <div class="header-left">
            <el-tag v-if="store.currentFairDay" type="primary" effect="dark" size="large">
              当前集市：{{ store.currentFairDay.name }}
              （{{ dayStatusLabel(store.currentFairDay.status) }}）
            </el-tag>
            <el-tag v-else type="info" size="large">未选择集市</el-tag>
            <el-select v-model="fairChoice" size="large" style="width: 280px; margin-left: 12px"
                       placeholder="选择赶集日" @change="onPickFair">
              <el-option v-for="d in fairDays" :key="d.id"
                         :label="`${d.name}（${dayStatusLabel(d.status)}）`" :value="d.id" />
            </el-select>
          </div>
          <div class="header-right">
            <el-tag :type="roleTagType" effect="plain" size="large">{{ roleLabel }}</el-tag>
            <span class="username">{{ store.user?.name }}</span>
            <el-button text @click="logout"><el-icon><SwitchButton /></el-icon> 退出</el-button>
          </div>
        </el-header>
        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from './api.js'
import { store } from './store.js'

const router = useRouter()
const isLogin = computed(() => router.currentRoute.value.path === '/login')
const isVendor = computed(() => store.user?.role === 'VENDOR')
const isStaff = computed(() => store.user?.role === 'STAFF' || store.user?.role === 'ADMIN')
const roleLabel = computed(() => ({ VENDOR: '摊主', STAFF: '市场管理人员', ADMIN: '乡镇管理者' }[store.user?.role]))
const roleTagType = computed(() => ({ VENDOR: 'success', STAFF: 'warning', ADMIN: 'danger' }[store.user?.role]))

const fairDays = ref([])
const fairChoice = ref(store.fairDayId)

function dayStatusLabel(s) {
  return { PLANNED: '筹备中', OPEN: '报名中', DRAWN: '已抽签', ONGOING: '赶集进行中', CLOSED: '已结束' }[s] || s
}

async function loadFairs() {
  fairDays.value = await api.get('/fair-days')
  if (!store.currentFairDay && fairDays.value.length) {
    const first = fairDays.value[0]
    store.setFairDay(first)
    fairChoice.value = first.id
  }
}
function onPickFair(id) {
  const d = fairDays.value.find((x) => x.id === id)
  store.setFairDay(d || null)
}
function logout() {
  api.post('/auth/logout').catch(() => {})
  localStorage.removeItem('token')
  store.setUser(null)
  router.push('/login')
}
onMounted(() => { if (!isLogin.value) loadFairs() })
</script>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif; background: #f2f4f7; }
.layout { height: 100vh; }
.aside { background: linear-gradient(180deg, #1f3a2e 0%, #264d39 100%); color: #fff; display: flex; flex-direction: column; }
.brand { display: flex; align-items: center; gap: 10px; padding: 16px 14px; border-bottom: 1px solid rgba(255,255,255,.12); }
.brand-icon { font-size: 30px; color: #ffd98a; }
.brand-title { font-size: 15px; font-weight: 700; line-height: 1.3; }
.brand-sub { font-size: 11px; color: rgba(255,255,255,.65); }
.menu { background: transparent; border-right: none; flex: 1; }
:deep(.el-menu) { background: transparent; }
:deep(.el-menu-item), :deep(.el-sub-menu__title) { color: rgba(255,255,255,.82); }
:deep(.el-menu-item.is-active) { background: rgba(255,217,138,.18); color: #ffd98a; }
:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) { background: rgba(255,255,255,.08); color: #fff; }
:deep(.el-sub-menu .el-menu-item) { background: rgba(0,0,0,.15); min-width: 0; }
:deep(.el-sub-menu.is-active > .el-sub-menu__title) { color: #ffd98a; }
.header { background: #fff; border-bottom: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.header-left { display: flex; align-items: center; }
.header-right { display: flex; align-items: center; gap: 10px; }
.username { font-weight: 600; color: #333; }
.main { padding: 18px; background: #f2f4f7; }
.page-title { font-size: 19px; font-weight: 700; margin: 0 0 4px; color: #1f3a2e; }
.page-desc { color: #888; font-size: 13px; margin-bottom: 16px; }
.card-msg { color: #777; font-size: 13px; }
</style>
