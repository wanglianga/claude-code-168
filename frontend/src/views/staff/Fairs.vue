<template>
  <div>
    <h2 class="page-title">集市与摊位设置</h2>
    <p class="page-desc">维护赶集日、功能分区（普通/熟食冷链/活禽隔离）、摊位尺寸、电源与消防通道标记，并推进集市状态。</p>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>赶集日列表</b>
          <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon> 新建集市</el-button>
        </div>
      </template>
      <el-table :data="fairs" stripe>
        <el-table-column prop="name" label="集市" min-width="200" />
        <el-table-column prop="marketDate" label="赶集日期" width="120" />
        <el-table-column prop="signupDeadline" label="报名截止" width="170">
          <template #default="{ row }">{{ fmt(row.signupDeadline) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="dayTag(row.status)">{{ dayLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="drawSeed" label="抽签种子" width="150">
          <template #default="{ row }">{{ row.drawSeed || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">区域/摊位</el-button>
            <el-dropdown size="small" @command="(c) => changeStatus(row, c)" style="margin-left:6px">
              <el-button size="small">状态流转<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="s in statusFlow(row.status)" :key="s.v" :command="s.v">
                    转为「{{ dayLabel(s.v) }}」
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="新建集市" width="520px">
      <el-form :model="cf" label-width="100px">
        <el-form-item label="集市名称" required><el-input v-model="cf.name" placeholder="如：2026-10-08 重阳大集" /></el-form-item>
        <el-form-item label="赶集日期" required><el-date-picker v-model="cf.marketDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="报名截止"><el-date-picker v-model="cf.signupDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="计划抽签时间"><el-time-picker v-model="cf.drawTime" value-format="HH:mm:ss" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible=false">取消</el-button>
        <el-button type="primary" @click="doCreate">创建并自动开放报名</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="72%" :title="`区域与摊位 — ${detail?.fairDay?.name || ''}`">
      <template v-if="detail">
        <el-alert type="success" :closable="false" style="margin-bottom:12px"
                  title="分区决定抽签归属：熟食/凉菜/糕点→熟食冷链区，活禽→活禽隔离区；邻消防通道摊禁明火熟食与活禽。" />
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>功能分区</b>
              <el-button type="primary" size="small" @click="openZone"><el-icon><Plus /></el-icon> 新增区域</el-button>
            </div>
          </template>
          <el-table :data="detail.zones" stripe>
            <el-table-column prop="code" label="编码" width="70" />
            <el-table-column prop="name" label="区域" width="150" />
            <el-table-column label="类型" width="130">
              <template #default="{ row }"><el-tag :type="zoneTag(row.type)">{{ zoneLabel(row.type) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="摊位费" width="90"><template #default="{ row }">{{ row.stallFee }}元</template></el-table-column>
            <el-table-column label="用电费" width="90"><template #default="{ row }">{{ row.electricityFee }}元</template></el-table-column>
            <el-table-column label="保证金" width="90"><template #default="{ row }">{{ row.depositStandard }}元</template></el-table-column>
            <el-table-column prop="note" label="备注" min-width="160" />
            <el-table-column label="批量生成摊位" width="220">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openGen(row)">生成摊位</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card style="margin-top:14px">
          <template #header><b>摊位平面图（{{ detail.stalls.length }} 个）</b></template>
          <el-table :data="detail.stalls" stripe max-height="430">
            <el-table-column prop="stallNo" label="摊位号" width="90" />
            <el-table-column label="区域" width="150">
              <template #default="{ row }">{{ row.zone.name }}（{{ zoneLabel(row.zone.type) }}）</template>
            </el-table-column>
            <el-table-column label="尺寸" width="120"><template #default="{ row }">{{ row.length }}×{{ row.width }}m</template></el-table-column>
            <el-table-column label="属性" width="180">
              <template #default="{ row }">
                <el-tag v-if="row.hasElectricity" type="warning" size="small">带电源</el-tag>
                <el-tag v-if="row.nearFireLane" type="danger" size="small" style="margin-left:4px">邻消防通道</el-tag>
                <el-tag v-if="!row.enabled" type="info" size="small" style="margin-left:4px">停用</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="230">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" active-text="启用" inactive-text="停用"
                           @change="(v) => toggleStall(row, 'enabled', v)" />
                <el-switch v-model="row.hasElectricity" active-text="" @change="(v) => toggleStall(row, 'hasElectricity', v)" style="margin-left:12px" />
                <span class="card-msg" style="margin-left:4px">电源</span>
                <el-switch v-model="row.nearFireLane" @change="(v) => toggleStall(row, 'nearFireLane', v)" style="margin-left:12px" />
                <span class="card-msg">消防</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </el-drawer>

    <el-dialog v-model="zoneVisible" title="新增功能分区" width="480px">
      <el-form :model="cz" label-width="96px">
        <el-form-item label="区域编码" required><el-input v-model="cz.code" placeholder="A/B/C" style="width:100px" /></el-form-item>
        <el-form-item label="区域名称" required><el-input v-model="cz.name" /></el-form-item>
        <el-form-item label="分区类型" required>
          <el-select v-model="cz.type" style="width:200px">
            <el-option label="普通干货区" value="NORMAL" />
            <el-option label="熟食冷链区" value="COOKED" />
            <el-option label="活禽隔离区" value="POULTRY" />
          </el-select>
        </el-form-item>
        <el-form-item label="摊位费"><el-input-number v-model="cz.stallFee" :min="0" :precision="2" :step="10" /></el-form-item>
        <el-form-item label="用电附加费"><el-input-number v-model="cz.electricityFee" :min="0" :precision="2" :step="5" /></el-form-item>
        <el-form-item label="保证金标准"><el-input-number v-model="cz.depositStandard" :min="0" :precision="2" :step="50" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="cz.note" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="zoneVisible=false">取消</el-button>
        <el-button type="primary" @click="doAddZone">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="genVisible" title="批量生成摊位" width="460px">
      <el-form :model="g" label-width="130px">
        <el-form-item label="所属区域">{{ g.zoneName }}</el-form-item>
        <el-form-item label="摊位号前缀"><el-input v-model="g.prefix" style="width:100px" /></el-form-item>
        <el-form-item label="生成数量"><el-input-number v-model="g.count" :min="1" :max="40" /></el-form-item>
        <el-form-item label="摊位长度(米)"><el-input-number v-model="g.length" :min="1" :max="12" :precision="2" :step="0.5" /></el-form-item>
        <el-form-item label="摊位宽度(米)"><el-input-number v-model="g.width" :min="1" :max="10" :precision="2" :step="0.5" /></el-form-item>
        <el-form-item label="带电源摊位数"><el-input-number v-model="g.electricityCount" :min="0" :max="40" /></el-form-item>
        <el-form-item label="邻消防通道">
          <el-select v-model="g.fireLaneNos" multiple placeholder="选择序号（如第1个）" style="width:100%">
            <el-option v-for="n in g.count" :key="n" :label="`第 ${n} 个`" :value="n" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genVisible=false">取消</el-button>
        <el-button type="primary" @click="doGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api.js'
import { store } from '../../store.js'

const fairs = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const zoneVisible = ref(false)
const genVisible = ref(false)
const detail = ref(null)
const currentFair = ref(null)
const cf = reactive({ name: '', marketDate: '', signupDeadline: '', drawTime: '04:30' })
const cz = reactive({ code: '', name: '', type: 'NORMAL', stallFee: 30, electricityFee: 10, depositStandard: 100, note: '' })
const g = reactive({ zoneId: null, zoneName: '', prefix: '', count: 10, length: 4, width: 3, electricityCount: 0, fireLaneNos: [] })

function dayLabel(s) { return { PLANNED: '筹备中', OPEN: '报名中', DRAWN: '已抽签', ONGOING: '赶集进行中', CLOSED: '已结束' }[s] || s }
function dayTag(s) { return { PLANNED: 'info', OPEN: 'success', DRAWN: 'warning', ONGOING: 'danger', CLOSED: 'info' }[s] }
function zoneLabel(t) { return { NORMAL: '普通干货区', COOKED: '熟食冷链区', POULTRY: '活禽隔离区' }[t] || t }
function zoneTag(t) { return { NORMAL: '', COOKED: 'warning', POULTRY: 'danger' }[t] }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }
function statusFlow(s) {
  return {
    PLANNED: [{ v: 'OPEN' }],
    OPEN: [{ v: 'DRAWN', label: '需先在抽签页完成抽签' }],
    DRAWN: [{ v: 'ONGOING' }],
    ONGOING: [{ v: 'CLOSED' }],
    CLOSED: []
  }[s] || []
}

async function load() { fairs.value = await api.get('/fair-days') }
async function viewDetail(row) {
  currentFair.value = row
  detail.value = await api.get(`/fair-days/${row.id}`)
  detailVisible.value = true
}
function openCreate() { Object.assign(cf, { name: '', marketDate: '', signupDeadline: '', drawTime: '04:30' }); createVisible.value = true }
async function doCreate() {
  if (!cf.name || !cf.marketDate) return ElMessage.warning('名称和日期必填')
  await api.post('/fair-days', cf)
  ElMessage.success('集市已创建（默认报名中）')
  createVisible.value = false
  load()
}
async function changeStatus(row, target) {
  await ElMessageBox.confirm(`确认将「${row.name}」状态转为【${dayLabel(target)}】？`, '状态流转确认', { type: 'warning' })
  await api.put(`/fair-days/${row.id}/status`, { status: target })
  ElMessage.success('状态已更新')
  store.setFairDay({ ...row, status: target })
  load()
}
function openZone() {
  if (!detail.value.zones.length) Object.assign(cz, { code: 'A', name: '', type: 'NORMAL', stallFee: 30, electricityFee: 10, depositStandard: 100, note: '' })
  zoneVisible.value = true
}
async function doAddZone() {
  await api.post(`/fair-days/${currentFair.value.id}/zones`, cz)
  ElMessage.success('区域已新增')
  zoneVisible.value = false
  viewDetail(currentFair.value)
}
function openGen(zone) {
  Object.assign(g, { zoneId: zone.id, zoneName: zone.name, prefix: zone.code, count: 10, length: 4, width: 3, electricityCount: 0, fireLaneNos: [] })
  genVisible.value = true
}
async function doGenerate() {
  const n = await api.post(`/fair-days/${currentFair.value.id}/stalls/generate`, {
    zoneId: g.zoneId, prefix: g.prefix, count: g.count, length: g.length, width: g.width,
    electricityCount: g.electricityCount, fireLaneNos: g.fireLaneNos
  })
  ElMessage.success(`已生成 ${n} 个摊位`)
  genVisible.value = false
  viewDetail(currentFair.value)
}
async function toggleStall(row, field, value) {
  await api.put(`/fair-days/stalls/${row.id}`, { [field]: value })
  ElMessage.success('已更新')
}
load()
</script>
