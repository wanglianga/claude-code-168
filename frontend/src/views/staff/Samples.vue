<template>
  <div>
    <h2 class="page-title">留样台账</h2>
    <p class="page-desc">核对卤味、糕点、凉菜等熟食摊主逐批次留样（照片、批次号、保存位置、冷藏温度），支撑腹泻类投诉追溯。</p>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>{{ store.currentFairDay?.name || '全部集市' }} · 食品留样</b>
          <el-button @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
        </div>
      </template>
      <el-table :data="list" stripe>
        <el-table-column label="留样时间" width="160">
          <template #default="{ row }">{{ fmt(row.sampledAt) }}</template>
        </el-table-column>
        <el-table-column label="摊主" min-width="140">
          <template #default="{ row }">{{ row.vendor?.name }}</template>
        </el-table-column>
        <el-table-column prop="foodName" label="食品" width="140" />
        <el-table-column prop="batchNo" label="批次号" width="130" />
        <el-table-column prop="storageLocation" label="保存位置" width="150" />
        <el-table-column label="温度" width="80">
          <template #default="{ row }">{{ row.storageTemp ?? '—' }}℃</template>
        </el-table-column>
        <el-table-column label="照片" min-width="110">
          <template #default="{ row }"><PhotoView v-if="row.photos" :urls="row.photos" /></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '../../api.js'
import { store } from '../../store.js'
import PhotoView from '../../components/PhotoView.vue'

const list = ref([])
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }
async function load() {
  list.value = store.fairDayId
    ? await api.get('/food-samples', { params: { fairDayId: store.fairDayId } })
    : await api.get('/food-samples')
}
watch(() => store.fairDayId, load, { immediate: true })
</script>
