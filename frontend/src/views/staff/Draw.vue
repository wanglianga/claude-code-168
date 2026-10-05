<template>
  <div>
    <h2 class="page-title">组织抽签</h2>
    <p class="page-desc">
      规则：分区（熟食冷链/活禽隔离/普通）、消防通道禁明火熟食与活禽、用电与尺寸匹配、老摊主（历史赶集次数）与信用分高者优先；
      使用当场公示随机种子，结果可复算，过程逐行可核。
    </p>

    <el-card v-if="store.currentFairDay">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>{{ store.currentFairDay.name }}</b>
          <el-tag :type="dayTag(store.currentFairDay.status)">{{ dayLabel(store.currentFairDay.status) }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="1" border style="max-width:760px">
        <el-descriptions-item label="本场公示规则">
          <div style="white-space:pre-wrap">{{ store.currentFairDay.rulesText || '默认规则见页面说明。' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="已公示种子" v-if="store.currentFairDay.drawSeed">
          <b style="color:#2f6b47">{{ store.currentFairDay.drawSeed }}</b>
          <span class="card-msg" style="margin-left:10px">抽签时间：{{ fmt(store.currentFairDay.drawnAt) }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="12" style="margin-top:14px">
        <el-col :span="10">
          <el-statistic title="审核通过报名数" :value="summary.approved" />
        </el-col>
        <el-col :span="7">
          <el-statistic title="可用摊位数" :value="summary.stalls" />
        </el-col>
        <el-col :span="7">
          <el-statistic title="已抽中（现存结果）" :value="summary.allocated" />
        </el-col>
      </el-row>

      <el-form inline style="margin-top:16px">
        <el-form-item label="随机种子（公示）">
          <el-input v-model="seed" placeholder="留空则现场随机生成；指定种子结果可复算" style="width:360px"
                    :disabled="busy" />
        </el-form-item>
        <el-button type="primary" :loading="busy" @click="doDraw">
          <el-icon><RefreshRight /></el-icon> {{ hasAllocation ? '重新抽签（将覆盖未入场结果）' : '开始抽签' }}
        </el-button>
        <el-button v-if="hasAllocation" type="success" @click="setOngoing">抽签完成，集市转入「进行中」</el-button>
      </el-form>
      <el-alert v-if="hasAllocation" type="warning" :closable="false" show-icon
                title="已有抽签结果；重新抽签会覆盖现有结果，但若已有摊主入场，系统将拒绝重抽（请改用临时调位）。" />
    </el-card>

    <el-card style="margin-top:14px" v-if="result">
      <template #header>
        <b>抽签过程（可复算）</b>
        <el-tag style="margin-left:8px">种子 {{ result.seed }}</el-tag>
        <el-tag type="success" style="margin-left:6px">中签 {{ result.allocatedCount }}</el-tag>
        <el-tag type="danger" style="margin-left:6px">候补 {{ result.waitlistCount }}</el-tag>
      </template>
      <el-table :data="result.traces" stripe max-height="520">
        <el-table-column label="抽签顺序" type="index" width="70" />
        <el-table-column prop="vendor" label="摊主" min-width="160" />
        <el-table-column prop="category" label="品类" width="90" />
        <el-table-column label="目标分区" width="120">
          <template #default="{ row }">
            <el-tag :type="zoneTag(row.targetZone)">{{ zoneLabel(row.targetZone) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先分" width="80" />
        <el-table-column prop="random" label="随机数（可复算）" width="200" />
        <el-table-column label="结果" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.result==='WAITLIST'" type="danger">候补</el-tag>
            <el-tag v-else type="success">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="entryTime" label="入场时间" width="90" />
        <el-table-column prop="reason" label="候补原因" min-width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api.js'
import { store } from '../../store.js'

const seed = ref('')
const result = ref(null)
const busy = ref(false)
const applications = ref([])
const allocations = ref([])
const stalls = ref([])

const hasAllocation = computed(() => allocations.value.some((a) => a.status !== 'WAITLIST'))
const summary = computed(() => ({
  approved: applications.value.filter((a) => a.status === 'APPROVED').length,
  stalls: stalls.value.filter((s) => s.enabled).length,
  allocated: allocations.value.filter((a) => a.status !== 'WAITLIST').length
}))

function dayLabel(s) { return { PLANNED: '筹备中', OPEN: '报名中', DRAWN: '已抽签', ONGOING: '进行中', CLOSED: '已结束' }[s] || s }
function dayTag(s) { return { PLANNED: 'info', OPEN: 'success', DRAWN: 'warning', ONGOING: 'danger', CLOSED: 'info' }[s] }
function zoneLabel(z) { return { NORMAL: '普通干货区', COOKED: '熟食冷链区', POULTRY: '活禽隔离区' }[z] || z }
function zoneTag(z) { return { NORMAL: '', COOKED: 'warning', POULTRY: 'danger' }[z] }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function load() {
  result.value = null
  applications.value = []
  allocations.value = []
  stalls.value = []
  if (!store.fairDayId) return
  applications.value = await api.get('/applications', { params: { fairDayId: store.fairDayId } })
  allocations.value = await api.get(`/draw/${store.fairDayId}/allocations`)
  const detail = await api.get(`/fair-days/${store.fairDayId}`)
  stalls.value = detail.stalls
}

async function doDraw() {
  await ElMessageBox.confirm(
    `确认在「${store.currentFairDay.name}」执行抽签？\n` +
    `种子：${seed.value || '（留空将随机生成）'}\n` +
    (hasAllocation.value ? '将覆盖现有未入场结果。' : ''),
    '抽签确认', { confirmButtonText: '执行抽签', cancelButtonText: '取消' })
  busy.value = true
  try {
    result.value = await api.post(`/draw/${store.fairDayId}`, { seed: seed.value })
    ElMessage.success('抽签完成')
    store.setFairDay({ ...store.currentFairDay, status: 'DRAWN' })
    load()
  } finally {
    busy.value = false
  }
}

async function setOngoing() {
  await ElMessageBox.confirm('确认开始赶集、允许扫码入场？（状态→进行中）', '状态确认', { type: 'success' })
  const day = await api.put(`/fair-days/${store.fairDayId}/status`, { status: 'ONGOING' })
  store.setFairDay(day)
  ElMessage.success('集市已进入进行中状态')
}
watch(() => store.fairDayId, load, { immediate: true })
</script>
