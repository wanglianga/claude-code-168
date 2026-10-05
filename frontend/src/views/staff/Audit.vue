<template>
  <div>
    <h2 class="page-title">操作审计日志</h2>
    <p class="page-desc">登录、审核、抽签、收费、投诉处置等关键动作自动记录操作人、时间与 IP，便于事后追溯核查。</p>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>最近 100 条审计日志</b>
          <el-button size="small" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
        </div>
      </template>
      <el-table :data="logs" stripe>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="140" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }"><el-tag size="small">{{ roleLabel(row.role) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="action" label="动作" width="170" />
        <el-table-column prop="detail" label="详情" min-width="300" />
        <el-table-column prop="ip" label="IP" width="130" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../../api.js'

const logs = ref([])
function roleLabel(r) { return { VENDOR: '摊主', STAFF: '市场管理人员', ADMIN: '乡镇管理者' }[r] || r }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 19) : '' }
async function load() { logs.value = await api.get('/stats/audit-logs') }
onMounted(load)
</script>
