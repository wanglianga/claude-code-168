<template>
  <div>
    <h2 class="page-title">我的抽签结果与缴费</h2>
    <p class="page-desc">展示摊位号、错峰入场时间、收费项目、保证金、禁止事项与入场核销二维码。</p>
    <el-alert v-if="!store.fairDayId" type="warning" :closable="false" title="请先选择赶集日" style="margin-bottom:14px" />

    <div v-if="store.fairDayId">
      <el-empty v-if="!data" description="当场集市暂无抽签结果（可能尚未抽签或您未通过审核）" />
      <el-row v-else :gutter="14">
        <el-col :span="14">
          <el-card>
            <template #header>
              <b>{{ store.currentFairDay?.name }}</b>
              <el-tag :type="allocTag(data.allocation.status)" style="margin-left:10px">{{ allocLabel(data.allocation.status) }}</el-tag>
            </template>
            <el-alert v-if="data.allocation.status==='WAITLIST'" type="warning" :closable="false"
                      :title="data.allocation.prohibitions" style="margin-bottom:12px" />
            <template v-else>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="摊位号">
                  <span style="font-size:20px;font-weight:800;color:#2f6b47">{{ data.allocation.stall?.stallNo }}</span>
                  （{{ data.allocation.stall?.zone?.name }}）
                </el-descriptions-item>
                <el-descriptions-item label="错峰入场时间">
                  <b style="color:#b45309">{{ data.allocation.entryTime }}</b>
                </el-descriptions-item>
                <el-descriptions-item label="摊位尺寸">{{ data.allocation.stall?.length }}m × {{ data.allocation.stall?.width }}m
                  <el-tag v-if="data.allocation.stall?.hasElectricity" size="small" type="warning" style="margin-left:6px">可接电</el-tag>
                  <el-tag v-if="data.allocation.stall?.nearFireLane" size="small" type="danger" style="margin-left:6px">邻消防通道</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="核销码">{{ data.allocation.checkCode }}</el-descriptions-item>
                <el-descriptions-item label="摊位费">{{ data.allocation.stallFee }} 元</el-descriptions-item>
                <el-descriptions-item label="用电附加费">{{ data.allocation.electricityFee }} 元</el-descriptions-item>
                <el-descriptions-item label="保证金">{{ data.allocation.deposit }} 元</el-descriptions-item>
                <el-descriptions-item label="合计应收">{{ data.allocation.totalAmount }} 元</el-descriptions-item>
                <el-descriptions-item label="已缴">{{ data.allocation.paidAmount }} 元</el-descriptions-item>
                <el-descriptions-item label="已减免">{{ data.allocation.waivedAmount }} 元</el-descriptions-item>
                <el-descriptions-item label="待缴">
                  <el-tag :type="Number(data.outstanding) > 0 ? 'danger' : 'success'">{{ data.outstanding }} 元</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="缴费状态">
                  <el-tag :type="feeTag(data.allocation.feeStatus)">{{ feeLabel(data.allocation.feeStatus) }}</el-tag>
                </el-descriptions-item>
              </el-descriptions>
              <el-alert type="info" :closable="false" style="margin-top:12px"
                        title="入场请向工作人员出示本页二维码；费用未缴清将无法扫码入场。" />
            </template>
          </el-card>

          <el-card v-if="data.allocation.status !== 'WAITLIST'" style="margin-top:14px">
            <template #header><b>本摊位禁止/注意事项</b></template>
            <div class="prohibitions">{{ data.allocation.prohibitions }}</div>
          </el-card>
        </el-col>

        <el-col :span="10">
          <el-card class="qr-card">
            <template #header><b>入场核销二维码</b></template>
            <img v-if="data.qr" :src="data.qr" alt="核销码" class="qr" />
            <div class="card-msg">工作人员扫码核验后，将拍摄摊位布置照片完成入场。</div>
          </el-card>
          <el-card style="margin-top:14px">
            <template #header>
              <b>我的调整台账（公示可查）</b>
              <el-button link type="primary" @click="loadLedger">刷新</el-button>
            </template>
            <el-timeline>
              <el-timeline-item v-for="l in ledger" :key="l.id" :timestamp="fmt(l.createdAt)" placement="top">
                <el-tag size="small" :type="ledgerTag(l.type)" style="margin-right:6px">{{ ledgerLabel(l.type) }}</el-tag>
                <b>{{ l.beforeValue }} → {{ l.afterValue }}</b>
                <div class="card-msg">来源：{{ l.sourceType }}<span v-if="l.sourceId"> #{{ l.sourceId }}</span>；{{ l.reason }}</div>
                <div class="card-msg">经办人：{{ l.operator?.name }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="!ledger.length" description="暂无调整记录" :image-size="70" />
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '../../api.js'
import { store } from '../../store.js'

const data = ref(null)
const ledger = ref([])

function allocLabel(s) { return { ALLOCATED: '已中签待入场', CHECKED_IN: '已入场', ADJUSTED: '已临时调位', NO_SHOW: '未到场', CANCELLED: '已取消', WAITLIST: '候补未中签' }[s] || s }
function allocTag(s) { return { ALLOCATED: 'warning', CHECKED_IN: 'success', ADJUSTED: 'primary', NO_SHOW: 'info', CANCELLED: 'info', WAITLIST: 'danger' }[s] }
function feeLabel(s) { return { UNPAID: '未缴', PAID: '已缴', WAIVED: '部分减免后已缴' }[s] || s }
function feeTag(s) { return { UNPAID: 'danger', PAID: 'success', WAIVED: 'warning' }[s] }
function ledgerLabel(t) { return { STALL_MOVE: '临时调位', FEE_WAIVER: '费用减免', DEPOSIT_DEDUCT: '保证金扣罚', DEPOSIT_REFUND: '保证金退还', CREDIT_ADJUST: '信用调整' }[t] || t }
function ledgerTag(t) { return { STALL_MOVE: 'primary', FEE_WAIVER: 'warning', DEPOSIT_DEDUCT: 'danger', DEPOSIT_REFUND: 'success', CREDIT_ADJUST: 'info' }[t] }
function fmt(t) { return t?.replace('T', ' ').slice(0, 16) }

async function load() {
  data.value = null
  ledger.value = []
  if (!store.fairDayId) return
  const mine = await api.get(`/draw/mine/${store.fairDayId}`)
  if (mine) data.value = await api.get(`/allocations/${mine.id}`)
  ledger.value = await api.get('/ledger/mine')
}
watch(() => store.fairDayId, load, { immediate: true })
</script>

<style scoped>
.qr-card { text-align: center; }
.qr { width: 230px; height: 230px; }
.prohibitions { font-size: 13.5px; line-height: 2; color: #444; }
</style>
