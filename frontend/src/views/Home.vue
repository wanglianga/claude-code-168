<template>
  <div>
    <h2 class="page-title">工作台</h2>
    <p class="page-desc">您好，{{ store.user?.name }}（{{ roleLabel }}）。请在顶部选择赶集日开展业务。</p>

    <el-alert v-if="!store.fairDayId" type="warning" show-icon :closable="false"
              title="尚未选择赶集日" description="请在页面顶部下拉选择一个赶集日。" style="margin-bottom:16px" />

    <!-- 摊主工作台 -->
    <template v-if="isVendor">
      <el-row :gutter="14">
        <el-col :span="6" v-for="c in vendorCards" :key="c.path">
          <el-card shadow="hover" class="entry" @click="$router.push(c.path)">
            <el-icon class="entry-icon" :style="{ background: c.color }"><component :is="c.icon" /></el-icon>
            <div class="entry-title">{{ c.title }}</div>
            <div class="entry-desc">{{ c.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
      <el-card style="margin-top: 16px" v-if="mine">
        <template #header><b>我在「{{ store.currentFairDay?.name }}」的抽签结果</b></template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="状态">{{ allocStatus(mine.status) }}</el-descriptions-item>
          <el-descriptions-item label="摊位">{{ mine.stall ? mine.stall.stallNo : '—' }}</el-descriptions-item>
          <el-descriptions-item label="入场时间">{{ mine.entryTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="摊位费">{{ mine.stallFee }} 元</el-descriptions-item>
          <el-descriptions-item label="用电费">{{ mine.electricityFee }} 元</el-descriptions-item>
          <el-descriptions-item label="保证金">{{ mine.deposit }} 元</el-descriptions-item>
          <el-descriptions-item label="缴费状态">{{ feeStatus(mine.feeStatus) }}</el-descriptions-item>
          <el-descriptions-item label="信用分">
            <el-tag :type="creditType">{{ store.user.creditScore }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="核销码">{{ mine.checkCode }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="mine.status === 'WAITLIST'" class="card-msg" style="margin-top:8px;color:#b88230">
          {{ mine.prohibitions }}
        </div>
      </el-card>
    </template>

    <!-- 管理工作台 -->
    <template v-else>
      <el-row :gutter="14">
        <el-col :span="6" v-for="c in staffCards" :key="c.path">
          <el-card shadow="hover" class="entry" @click="$router.push(c.path)">
            <el-icon class="entry-icon" :style="{ background: c.color }"><component :is="c.icon" /></el-icon>
            <div class="entry-title">{{ c.title }}</div>
            <div class="entry-desc">{{ c.desc }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" style="margin-top:16px" v-if="overview">
        <el-col :span="6"><el-card class="stat"><div class="stat-num">{{ overview.signup.total }}</div><div class="stat-label">报名总数（{{ overview.signup.approved }} 通过）</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat"><div class="stat-num">{{ overview.draw.allocated }}</div><div class="stat-label">已分配摊位（候补 {{ overview.draw.waitlist }}）</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat"><div class="stat-num">{{ overview.draw.checkInCount }}</div><div class="stat-label">已扫码入场（未到场 {{ overview.draw.noShow }}）</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat"><div class="stat-num">{{ overview.fee.paidRate }}%</div><div class="stat-label">费用到账率（应收 {{ overview.fee.receivable }} 元）</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat"><div class="stat-num">{{ overview.complaintTotal }}</div><div class="stat-label">投诉登记数</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat"><div class="stat-num">{{ overview.foodRisk.sampleCount }}</div><div class="stat-label">食品留样份数（覆盖率 {{ overview.foodRisk.sampleCoverage }}%）</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat">
          <div class="stat-num" :class="riskClass">{{ overview.foodRisk.riskLevel }}风险</div>
          <div class="stat-label">腹泻投诉 {{ overview.foodRisk.diarrheaComplaints }}，低卫生巡查 {{ overview.foodRisk.lowHygieneInspections }}</div>
        </el-card></el-col>
        <el-col :span="6"><el-card class="stat"><div class="stat-num">{{ overview.fee.depositDeducted }}</div><div class="stat-label">保证金扣罚（元）</div></el-card></el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { store } from '../store.js'
import api from '../api.js'
import { ref } from 'vue'

const isVendor = computed(() => store.user?.role === 'VENDOR')
const roleLabel = computed(() => ({ VENDOR: '摊主', STAFF: '市场管理人员', ADMIN: '乡镇管理者' }[store.user?.role]))

const vendorCards = [
  { path: '/vendor/apply', title: '赶集报名', desc: '品类/车辆/用电/证照', icon: 'EditPen', color: '#3b82f6' },
  { path: '/vendor/mine', title: '抽签结果与缴费', desc: '摊位号/二维码/收费', icon: 'Ticket', color: '#2f6b47' },
  { path: '/vendor/samples', title: '食品留样上传', desc: '批次/照片/保存位置', icon: 'Box', color: '#d97706' },
  { path: '/vendor/records', title: '我的巡查与台账', desc: '巡查记录/扣罚/信用', icon: 'Notebook', color: '#7c3aed' }
]
const staffCards = [
  { path: '/staff/review', title: '报名审核', desc: '证照核验/老摊主核实', icon: 'Checked', color: '#3b82f6' },
  { path: '/staff/draw', title: '组织抽签', desc: '规则公示/种子复算', icon: 'RefreshRight', color: '#2f6b47' },
  { path: '/staff/allocations', title: '收费与调位', desc: '缴费/减免/扣罚/调位', icon: 'Wallet', color: '#d97706' },
  { path: '/staff/checkin', title: '扫码入场核验', desc: '核销二维码/拍布置', icon: 'Aim', color: '#0e7490' },
  { path: '/staff/inspections', title: '市场巡查', desc: '卫生/标价/计量/占道/用电', icon: 'View', color: '#be185d' },
  { path: '/staff/complaints', title: '投诉处理', desc: '腹泻/缺斤少两联动扣罚', icon: 'ChatDotRound', color: '#b91c1c' },
  { path: '/staff/ledger', title: '调整公示台账', desc: '调位/减免/扣罚留痕', icon: 'Document', color: '#7c3aed' },
  { path: '/staff/dashboard', title: '复盘看板', desc: '投诉/拥堵/到账/风险', icon: 'DataAnalysis', color: '#334155' }
]

const mine = ref(null)
const overview = ref(null)
const creditType = computed(() => (store.user?.creditScore >= 100 ? 'success' : store.user?.creditScore >= 80 ? 'warning' : 'danger'))
const riskClass = computed(() => ({ 高: 'risk-high', 中: 'risk-mid', 低: 'risk-low' }[overview.value?.foodRisk?.riskLevel]))

function allocStatus(s) { return { ALLOCATED: '已中签待入场', CHECKED_IN: '已入场', ADJUSTED: '已临时调位', NO_SHOW: '未到场', CANCELLED: '已取消', WAITLIST: '候补未中签' }[s] || s }
function feeStatus(s) { return { UNPAID: '未缴', PAID: '已缴', WAIVED: '已减免' }[s] || s }

async function load() {
  mine.value = null
  overview.value = null
  if (!store.fairDayId) return
  if (isVendor.value) {
    mine.value = await api.get(`/draw/mine/${store.fairDayId}`)
  } else {
    overview.value = await api.get(`/stats/overview/${store.fairDayId}`)
  }
}
watch(() => store.fairDayId, load, { immediate: true })
</script>

<style scoped>
.entry { cursor: pointer; text-align: center; padding: 8px 0; }
.entry-icon { font-size: 26px; color: #fff; width: 54px; height: 54px; border-radius: 12px; padding: 14px; margin-bottom: 8px; }
.entry-title { font-weight: 700; font-size: 15px; }
.entry-desc { color: #999; font-size: 12px; margin-top: 2px; }
.stat { text-align: center; margin-bottom: 14px; }
.stat-num { font-size: 28px; font-weight: 800; color: #1f3a2e; }
.stat-label { color: #888; font-size: 12px; margin-top: 4px; }
.risk-high { color: #c62828; }
.risk-mid { color: #d97706; }
.risk-low { color: #2f6b47; }
</style>
