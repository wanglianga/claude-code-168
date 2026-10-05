<template>
  <div class="page">
    <el-card>
      <div class="card-title">审计日志（最近 200 条敏感操作）</div>
      <el-table :data="audits" size="small" max-height="680">
        <el-table-column prop="action" label="操作类型" width="170">
          <template #default="{ row }"><el-tag size="small">{{ row.action }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="actorName" label="操作人" width="140" />
        <el-table-column prop="detail" label="详情" min-width="400" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'
import { fmtTime } from '../../utils/dict'

const audits = ref([])
onMounted(async () => { audits.value = await api.get('/admin/audits') })
</script>
