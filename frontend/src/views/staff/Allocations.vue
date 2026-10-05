<template>
  <div>
    <h2 class="page-title">抽签结果、收费与现场处置</h2>
    <p class="page-desc">
      收费确认、费用减免、保证金扣罚/退还、临时调位、未到场登记。所有改变抽签结果与费用的动作都会写入「调整公示台账」并注明来源与理由。
    </p>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>{{ store.currentFairDay?.name || '请选择集市' }} · 抽签结果</b>
          <el-button @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
        </div>
      </template>
      <el-table :data="list" stripe max-height="640" :default-sort="{ prop: 'stall.stallNo' }">
        <el-table-column prop="stall.stallNo" label="摊位" width="90">
          <template #default="{ row }">{{ row.stall?.stallNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="摊主" min-width="150">
          <template #default="{ row }">
            <b>{{ row.vendor.name }}</b>
            <div class="card-msg">{{ row.application?.category }} · 赶集{{ row.application?.historyCount }}次 · 信用{{ row.vendor.creditScore }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="entryTime" label="入场时间" width="90" />
        <el-table-column label="费用（摊/电/保）" width="200">
          <template #default="{ row }">
            {{ row.stallFee }} + {{ row.electricityFee }} + {{ row.deposit }} = <b>{{ row.totalAmount }}</b>
          </template>
        </el-table-column>
        <el-table-column label="收缴情况" width="150">
          <template #default="{ row }">
            <el-tag :type="feeTag(row.feeStatus)" size="small">{{ feeLabel(row.feeStatus) }}</el-tag>
            <div class="card-msg">已缴{{ row.paidAmount }} / 减{{ row.waivedAmount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="保证金" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.deductedAmount>0" type="danger" size="small">扣{{ row.deductedAmount }}</el-tag>
            <el-tag v-if="row.refundedAmount>0" type="success" size="small" style="margin-left:4px">退{{ row.refundedAmount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="allocTag(row.status)" size="small">{{ allocLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :disabled="row.status==='WAITLIST'" @click="openPay(row)">收费</el-button>
            <el-dropdown size="small" @command="(c) => onCommand(c, row)">
              <el-button size="small">处置<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="waive" :disabled="row.status==='WAITLIST'">费用减免</el-dropdown-item>
                  <el-dropdown-item command="deduct" :disabled="row.status==='WAITLIST'">保证金扣罚</el-dropdown-item>
                  <el-dropdown-item command="refund" :disabled="row.status==='WAITLIST'">保证金退还</el-dropdown-item>
                  <el-dropdown-item command="move" :disabled="row.status==='WAITLIST'">临时调位</el-dropdown-item>
                  <el-dropdown-item command="noshow" :disabled="row.status==='CHECKED_IN'">标记未到场</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 收费 -->
    <el-dialog v-model="payVisible" title="确认收费到账" width="420px">
      <el-descriptions :column="1" border v-if="target">
        <el-descriptions-item label="摊主">{{ target.vendor.name }}</el-descriptions-item>
        <el-descriptions-item label="摊位">{{ target.stall?.stallNo }}</el-descriptions-item>
        <el-descriptions-item label="合计应收">{{ target.totalAmount }} 元（已减{{ target.waivedAmount }}，已收{{ target.paidAmount }}）</el-descriptions-item>
        <el-descriptions-item label="本次待收"><b style="color:#c62828">{{ outstanding }} 元</b></el-descriptions-item>
      </el-descriptions>
      <el-input-number v-model="payForm.amount" :min="0.01" :max="outstanding" :precision="2" :step="10" style="margin-top:12px" /> 元
      <template #footer>
        <el-button @click="payVisible=false">取消</el-button>
        <el-button type="primary" @click="doPay">确认收款</el-button>
      </template>
    </el-dialog>

    <!-- 减免 / 扣罚 -->
    <el-dialog v-model="moneyVisible" :title="moneyTitle" width="500px">
      <el-form label-width="110px">
        <el-form-item label="金额">
          <el-input-number v-model="moneyForm.amount" :min="0.01" :precision="2" :step="10" /> 元
        </el-form-item>
        <el-form-item v-if="moneyKind==='deduct'" label="联动信用扣分">
          <el-input-number v-model="moneyForm.creditDelta" :max="-1" :step="-5" /> 分（如 -10）
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="moneyForm.sourceType" style="width:220px">
            <el-option label="现场处置" value="现场处置" />
            <el-option label="投诉查实" value="投诉" />
            <el-option label="巡查违规" value="巡查" />
            <el-option label="天气调整" value="天气调整" />
            <el-option label="摊主申请" value="摊主申请" />
            <el-option label="困难减免" value="困难减免" />
          </el-select>
        </el-form-item>
        <el-form-item label="理由（必填）">
          <el-input v-model="moneyForm.reason" type="textarea" :rows="3"
                    placeholder="说明处置的事实依据、证明材料（如村委证明、现场照片、投诉编号等）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moneyVisible=false">取消</el-button>
        <el-button type="primary" @click="doMoney">确认并记入公示台账</el-button>
      </template>
    </el-dialog>

    <!-- 退还 -->
    <el-dialog v-model="refundVisible" title="保证金退还" width="420px">
      <el-descriptions :column="1" border v-if="target">
        <el-descriptions-item label="保证金">{{ target.deposit }} 元</el-descriptions-item>
        <el-descriptions-item label="已扣罚">{{ target.deductedAmount }} 元</el-descriptions-item>
        <el-descriptions-item label="已退">{{ target.refundedAmount }} 元</el-descriptions-item>
        <el-descriptions-item label="可退"><b style="color:#2f6b47">{{ refundable }} 元</b>（默认全退剩余）</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px" style="margin-top:10px">
        <el-form-item label="退还金额"><el-input-number v-model="refundAmount" :min="0" :max="refundable" :precision="2" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="refundReason" type="textarea" :rows="2" placeholder="散场退还剩余保证金" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible=false">取消</el-button>
        <el-button type="primary" @click="doRefund">确认退还</el-button>
      </template>
    </el-dialog>

    <!-- 调位 -->
    <el-dialog v-model="moveVisible" title="临时调位" width="760px">
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
                title="调位目标摊位必须空闲，且仍满足分区（熟食/活禽隔离）、用电、消防通道与尺寸要求；费用差额由减免/补收另行处理。" />
      <el-form label-width="100px">
        <el-form-item label="原摊位" v-if="target">{{ target.stall?.stallNo }}（{{ target.stall?.zone?.name }}）</el-form-item>
        <el-form-item label="目标摊位" required>
          <el-select v-model="moveTargetId" placeholder="选择空闲且符合规则的摊位" style="width:360px">
            <el-option v-for="s in freeStalls" :key="s.id"
                       :label="`${s.stallNo}（${s.zone.name} ${s.length}×${s.width}m${s.hasElectricity?' 带电':''}${s.nearFireLane?' 邻消防':''}）`"
                       :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="moveSourceType" style="width:220px">
            <el-option label="天气调整（雨天/积水）" value="天气调整" />
            <el-option label="邻里边界纠纷" value="边界纠纷" />
            <el-option label="消防/安全需要" value="消防处置" />
            <el-option label="摊主申请" value="摊主申请" />
            <el-option label="现场处置" value="现场处置" />
          </el-select>
        </el-form-item>
        <el-form-item label="调位原因" required>
          <el-input v-model="moveReason" type="textarea" :rows="3"
                    placeholder="说明事实与依据，公示给摊主，避免“抽签规则被人为改变”的质疑" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveVisible=false">取消</el-button>
        <el-button type="primary" @click="doMove">确认调位并记台账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api.js'
import { store } from '../../store.js'

const list = ref([])
const detail = ref({ stalls: [] })
const target = ref(null)
const payVisible = ref(false)
const moneyVisible = ref(false)
const refundVisible = ref(false)
const moveVisible = ref(false)
const moneyKind = ref('waive')
const payForm = reactive({ amount: 0 })
const moneyForm = reactive({ amount: 0, creditDelta: -10, sourceType: '现场处置', reason: '' })
const refundAmount = ref(0)
const refundReason = ref('散场退还剩余保证金')
const moveTargetId = ref(null)
const moveSourceType = ref('天气调整')
const moveReason = ref('')

const outstanding = computed(() => target.value ? Number((target.value.totalAmount - target.value.waivedAmount - target.value.paidAmount).toFixed(2)) : 0)
const refundable = computed(() => target.value ? Number((target.value.deposit - target.value.deductedAmount - target.value.refundedAmount).toFixed(2)) : 0)
const occupied = computed(() => new Set(list.value.filter((a) => a.stall && !['CANCELLED', 'NO_SHOW', 'WAITLIST'].includes(a.status)).map((a) => a.stall.id)))
const freeStalls = computed(() => detail.value.stalls.filter((s) => s.enabled && !occupied.value.has(s.id)))

const moneyTitle = computed(() => moneyKind.value === 'waive' ? '费用减免（填写理由）' : '保证金扣罚（事实依据）')

function feeLabel(s) { return { UNPAID: '未缴', PAID: '已缴', WAIVED: '已减免' }[s] }
function feeTag(s) { return { UNPAID: 'danger', PAID: 'success', WAIVED: 'warning' }[s] }
function allocLabel(s) { return { ALLOCATED: '已中签待入场', CHECKED_IN: '已入场', ADJUSTED: '已临时调位', NO_SHOW: '未到场', CANCELLED: '已取消', WAITLIST: '候补未中签' }[s] || s }
function allocTag(s) { return { ALLOCATED: 'warning', CHECKED_IN: 'success', ADJUSTED: 'primary', NO_SHOW: 'info', CANCELLED: 'info', WAITLIST: 'danger' }[s] }

async function load() {
  list.value = []
  detail.value = { stalls: [] }
  if (!store.fairDayId) return
  list.value = await api.get(`/draw/${store.fairDayId}/allocations`)
  detail.value = await api.get(`/fair-days/${store.fairDayId}`)
}

function openPay(row) {
  target.value = row
  payForm.amount = outstanding.value
  payVisible.value = true
}
async function doPay() {
  await api.post(`/allocations/${target.value.id}/pay`, { amount: payForm.amount })
  ElMessage.success('到账已确认')
  payVisible.value = false
  load()
}

function onCommand(cmd, row) {
  target.value = row
  if (cmd === 'waive') {
    moneyKind.value = 'waive'
    Object.assign(moneyForm, { amount: Math.min(outstanding.value, 30), creditDelta: -10, sourceType: '困难减免', reason: '' })
    moneyVisible.value = true
  } else if (cmd === 'deduct') {
    moneyKind.value = 'deduct'
    Object.assign(moneyForm, { amount: refundable.value || 50, creditDelta: -10, sourceType: '现场处置', reason: '' })
    moneyVisible.value = true
  } else if (cmd === 'refund') {
    refundAmount.value = refundable.value
    refundReason.value = '散场退还剩余保证金'
    refundVisible.value = true
  } else if (cmd === 'move') {
    moveTargetId.value = null
    moveReason.value = ''
    moveVisible.value = true
  } else if (cmd === 'noshow') {
    doNoShow(row)
  }
}

async function doMoney() {
  if (!moneyForm.reason) return ElMessage.warning('理由（事实依据）必填')
  const path = moneyKind.value === 'waive' ? 'waive' : 'deduct'
  const body = moneyKind.value === 'waive'
    ? { amount: moneyForm.amount, reason: moneyForm.reason, sourceType: moneyForm.sourceType }
    : { amount: moneyForm.amount, creditDelta: moneyForm.creditDelta, reason: moneyForm.reason, sourceType: moneyForm.sourceType }
  await api.post(`/allocations/${target.value.id}/${path}`, body)
  ElMessage.success('已记入调整公示台账')
  moneyVisible.value = false
  load()
}

async function doRefund() {
  await api.post(`/allocations/${target.value.id}/refund`, { amount: refundAmount.value, reason: refundReason.value })
  ElMessage.success('保证金已退还')
  refundVisible.value = false
  load()
}

async function doMove() {
  if (!moveTargetId.value) return ElMessage.warning('请选择目标摊位')
  if (!moveReason.value) return ElMessage.warning('请填写调位原因')
  await api.post(`/allocations/${target.value.id}/move`, {
    targetStallId: moveTargetId.value, reason: moveReason.value, sourceType: moveSourceType.value
  })
  ElMessage.success('临时调位完成并已留痕')
  moveVisible.value = false
  load()
}

async function doNoShow(row) {
  await ElMessageBox.confirm(`确认将 ${row.vendor.name} 标记为「未到场」？`, '未到场登记', { type: 'warning' })
  await api.post(`/allocations/${row.id}/no-show`)
  ElMessage.success('已登记')
  load()
}

watch(() => store.fairDayId, load, { immediate: true })
</script>
