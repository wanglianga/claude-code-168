<template>
  <div class="page">
    <el-card>
      <div class="card-title">抽签记录（种子+快照留档，可复核）</div>
      <el-select v-model="eventId" placeholder="选择集市" class="mb" style="width: 280px" @change="loadRuns">
        <el-option v-for="e in events" :key="e.id" :label="`${e.name}（${e.marketDate}）`" :value="e.id" />
      </el-select>
      <el-table :data="runs" size="small">
        <el-table-column prop="id" label="批次" width="70" />
        <el-table-column prop="seed" label="随机种子" width="120" />
        <el-table-column prop="remark" label="说明" min-width="200" />
        <el-table-column label="操作人" width="110">
          <template #default="{ row }">{{ row.operator?.name }}</template>
        </el-table-column>
        <el-table-column label="执行时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.executedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="viewRun(row)">结果明细</el-button>
            <el-button size="small" type="success" :loading="verifying === row.id" @click="verify(row)">复核</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="eventId && !runs.length" description="该集市尚未抽签" :image-size="60" />
    </el-card>

    <el-drawer v-model="detailVisible" :title="`抽签批次 #${detail?.run?.id} 明细`" size="75%">
      <template v-if="detail">
        <el-descriptions :column="3" border class="mb">
          <el-descriptions-item label="随机种子">{{ detail.run.seed }}</el-descriptions-item>
          <el-descriptions-item label="执行人">{{ detail.run.operator?.name }}</el-descriptions-item>
          <el-descriptions-item label="执行时间">{{ fmtTime(detail.run.executedAt) }}</el-descriptions-item>
        </el-descriptions>
        <el-collapse class="mb">
          <el-collapse-item title="规则快照（抽签时固化）" name="1">
            <pre class="snapshot">{{ prettyJson(detail.run.ruleSnapshot) }}</pre>
          </el-collapse-item>
          <el-collapse-item title="输入快照（参与抽签的报名与摊位）" name="2">
            <pre class="snapshot">{{ prettyJson(detail.run.inputSnapshot) }}</pre>
          </el-collapse-item>
        </el-collapse>
        <el-table :data="detail.results" size="small">
          <el-table-column label="摊位号" width="90">
            <template #default="{ row }"><el-tag type="success">{{ row.stallNo }}</el-tag></template>
          </el-table-column>
          <el-table-column label="摊主" width="130">
            <template #default="{ row }">
              {{ row.application?.vendor?.name }}
              <el-tag v-if="row.application?.veteran" size="small" type="success">老</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="品类" width="100">
            <template #default="{ row }">{{ categories[row.application?.category] }}</template>
          </el-table-column>
          <el-table-column prop="entryTime" label="入场时间" width="90" />
          <el-table-column label="收费项目" min-width="220">
            <template #default="{ row }">
              <span v-for="(f, i) in parseJson(row.feeItems)" :key="i">{{ f.item }}¥{{ f.amount }}<span v-if="i < parseJson(row.feeItems).length - 1">；</span></span>
            </template>
          </el-table-column>
          <el-table-column label="保证金" width="90">
            <template #default="{ row }">¥{{ row.deposit }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'RELOCATED' ? 'warning' : 'success'">
                {{ row.status === 'RELOCATED' ? '已调位' : '已分配' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'
import { CATEGORIES, fmtTime } from '../../utils/dict'

const categories = CATEGORIES
const events = ref([])
const eventId = ref(null)
const runs = ref([])
const detail = ref(null)
const detailVisible = ref(false)
const verifying = ref(null)

function parseJson(s) { try { return JSON.parse(s) } catch { return [] } }
function prettyJson(s) { try { return JSON.stringify(JSON.parse(s), null, 2) } catch { return s } }

async function loadRuns() {
  if (!eventId.value) return
  runs.value = await api.get(`/admin/events/${eventId.value}/runs`)
}

async function viewRun(row) {
  detail.value = await api.get(`/admin/runs/${row.id}`)
  detailVisible.value = true
}

async function verify(row) {
  verifying.value = row.id
  try {
    const res = await api.get(`/admin/runs/${row.id}/verify`)
    if (res.match) {
      ElMessageBox.alert(res.message, '复核通过 ✓', { type: 'success' })
    } else {
      ElMessageBox.alert(res.message, '复核不一致 ✗', { type: 'error' })
    }
  } finally { verifying.value = null }
}

onMounted(async () => {
  events.value = await api.get('/admin/events')
  const withRun = events.value.find(e => ['LOTTERY_DONE', 'IN_PROGRESS', 'FINISHED'].includes(e.status))
  if (withRun) { eventId.value = withRun.id; await loadRuns() }
})
</script>

<style scoped>
.snapshot { background: #f5f7fa; padding: 10px; border-radius: 4px; font-size: 12px; max-height: 300px; overflow: auto; }
</style>
