<template>
  <div>
    <h2 class="page-title">调整公示台账</h2>
    <p class="page-desc">
      临时调位、费用减免、保证金扣罚/退还、信用调整逐条公示：调整前后、金额/分值、<b>来源单据</b>与<b>理由</b>、经办人全留痕，确保公开公平与现场灵活并行不悖。
    </p>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>公示记录</b>
          <div>
            <el-select v-model="filterType" size="small" clearable placeholder="全部类型" style="width:150px">
              <el-option label="临时调位" value="STALL_MOVE" />
              <el-option label="费用减免" value="FEE_WAIVER" />
              <el-option label="保证金扣罚" value="DEPOSIT_DEDUCT" />
              <el-option label="保证金退还" value="DEPOSIT_REFUND" />
              <el-option label="信用分调整" value="CREDIT_ADJUST" />
            </el-select>
            <el-button size="small" @click="load" style="margin-left:10px"><el-icon><Refresh /></el-icon> 刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filtered" stripe>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }"><el-tag :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="摊主" min-width="150">
          <template #default="{ row }">
            <b>{{ row.vendor?.name }}</b>
            <div class="card-msg">{{ row.fairDay?.name || '—' }}{{ row.allocation?.stall ? ' · 摊位' + row.allocation.stall.stallNo : '' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="调整内容" width="260">
          <template #default="{ row }">
            {{ row.beforeValue }} <el-icon><Right /></el-icon> <b>{{ row.afterValue }}</b>
            <el-tag v-if="row.amount" type="warning" size="small" style="margin-left:8px">{{ row.amount }} 元</el-tag>
            <el-tag v-if="row.creditDelta" :type="row.creditDelta>0?'success':'danger'" size="small" style="margin-left:4px">
              信用{{ row.creditDelta > 0 ? '+' : '' }}{{ row.creditDelta }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="170">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.sourceType }}<span v-if="row.sourceId"> #{{ row.sourceId }}</span></el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="理由说明（公示口径）" min-width="260" />
        <el-table-column label="经办人" width="120">
          <template #default="{ row }">{{ row.operator?.name }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import api from '../../api.js'
import { store } from '../../store.js'

const list = ref([])
const filterType = ref('')
const filtered = computed(() => filterType.value ? list.value.filter((l) => l.type === filterType.value) : list.value)

function typeLabel(t) { return { STALL_MOVE: '临时调位', FEE_WAIVER: '费用减免', DEPOSIT_DEDUCT: '保证金扣罚', DEPOSIT_REFUND: '保证金退还', CREDIT_ADJUST: '信用分调整' }[t] || t }
function typeTag(t) { return { STALL_MOVE: 'primary', FEE_WAIVER: 'warning', DEPOSIT_DEDUCT: 'danger', DEPOSIT_REFUND: 'success', CREDIT_ADJUST: 'info' }[t] }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function load() {
  list.value = store.fairDayId
    ? await api.get('/ledger', { params: { fairDayId: store.fairDayId } })
    : await api.get('/ledger')
}
watch(() => store.fairDayId, load, { immediate: true })
</script>
