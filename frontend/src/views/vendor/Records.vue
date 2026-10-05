<template>
  <div>
    <h2 class="page-title">我的巡查记录与信用台账</h2>
    <p class="page-desc">巡查问题、责令整改、保证金扣罚/退还、费用减免、临时调位与信用调整全部可查，每条处置均注明来源。</p>

    <el-row :gutter="14">
      <el-col :span="6">
        <el-card class="credit-card">
          <div class="credit-num" :class="creditClass">{{ store.user?.creditScore }}</div>
          <div class="stat-label">我的信用分（初始 100）</div>
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card>
          <template #header><b>调整公示台账</b></template>
          <el-table :data="ledger" stripe>
            <el-table-column label="类型" width="110">
              <template #default="{ row }"><el-tag :type="ledgerTag(row.type)">{{ ledgerLabel(row.type) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="集市" width="170">
              <template #default="{ row }">{{ row.fairDay?.name || '—' }}</template>
            </el-table-column>
            <el-table-column label="变化" width="160">
              <template #default="{ row }">
                {{ row.beforeValue }} → <b>{{ row.afterValue }}</b>
                <span v-if="row.amount" class="card-msg">（{{ row.amount }}元）</span>
              </template>
            </el-table-column>
            <el-table-column label="来源" width="130">
              <template #default="{ row }">{{ row.sourceType }}<span v-if="row.sourceId"> #{{ row.sourceId }}</span></template>
            </el-table-column>
            <el-table-column prop="reason" label="理由（公示）" min-width="220" />
            <el-table-column label="时间" width="140">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:14px">
      <template #header><b>巡查记录</b></template>
      <el-table :data="inspections" stripe>
        <el-table-column label="集市" width="180"><template #default="{ row }">{{ row.fairDay?.name }}</template></el-table-column>
        <el-table-column label="卫生" width="70">
          <template #default="{ row }"><span :style="scoreStyle(row.hygieneScore)">{{ row.hygieneScore }}</span></template>
        </el-table-column>
        <el-table-column label="标价" width="70">
          <template #default="{ row }"><span :style="scoreStyle(row.priceTagScore)">{{ row.priceTagScore }}</span></template>
        </el-table-column>
        <el-table-column label="计量" width="70">
          <template #default="{ row }"><span :style="scoreStyle(row.measureScore)">{{ row.measureScore }}</span></template>
        </el-table-column>
        <el-table-column label="占道" width="70">
          <template #default="{ row }"><span :style="scoreStyle(row.aisleScore)">{{ row.aisleScore }}</span></template>
        </el-table-column>
        <el-table-column label="用电" width="70">
          <template #default="{ row }"><span :style="scoreStyle(row.electricScore)">{{ row.electricScore }}</span></template>
        </el-table-column>
        <el-table-column label="发现问题" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="i in splitTags(row.issues)" :key="i" size="small" type="danger" style="margin:2px">{{ i }}</el-tag>
            <span v-if="!row.issues" class="card-msg">无</span>
          </template>
        </el-table-column>
        <el-table-column label="责令整改" width="90">
          <template #default="{ row }">
            <el-tag :type="row.requireRectify ? 'danger' : 'info'">{{ row.requireRectify ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="情况说明" min-width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, ref, watch } from 'vue'
import api from '../../api.js'
import { store } from '../../store.js'

const ledger = ref([])
const inspections = ref([])
const creditClass = computed(() => store.user?.creditScore >= 100 ? 'good' : store.user?.creditScore >= 80 ? 'mid' : 'bad')

const score = defineComponent({
  props: { v: Number },
  setup(props) { return () => h('span', { style: `color:${props.v >= 4 ? '#2f6b47' : props.v >= 3 ? '#d97706' : '#c62828'};font-weight:700` }, props.v) }
})

function splitTags(s) { return s ? s.split(',').map((x) => x.trim()).filter(Boolean) : [] }
function scoreStyle(v) {
  const color = v >= 4 ? '#2f6b47' : v >= 3 ? '#d97706' : '#c62828'
  return { color: color, fontWeight: 700 }
}
function fmt(t) { return t?.replace('T', ' ').slice(0, 16) }
function ledgerLabel(t) { return { STALL_MOVE: '临时调位', FEE_WAIVER: '费用减免', DEPOSIT_DEDUCT: '保证金扣罚', DEPOSIT_REFUND: '保证金退还', CREDIT_ADJUST: '信用调整' }[t] || t }
function ledgerTag(t) { return { STALL_MOVE: 'primary', FEE_WAIVER: 'warning', DEPOSIT_DEDUCT: 'danger', DEPOSIT_REFUND: 'success', CREDIT_ADJUST: 'info' }[t] }

async function load() {
  ledger.value = await api.get('/ledger/mine')
  inspections.value = await api.get('/inspections', { params: { vendorId: store.user.id } })
}
watch(() => store.user, load, { immediate: true })
</script>

<style scoped>
.credit-card { text-align: center; }
.credit-num { font-size: 46px; font-weight: 800; }
.credit-num.good { color: #2f6b47; }
.credit-num.mid { color: #d97706; }
.credit-num.bad { color: #c62828; }
.stat-label { color: #888; font-size: 13px; }
</style>
