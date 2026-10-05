<template>
  <div class="page">
    <el-card>
      <div class="card-title">投诉与申诉处理</div>
      <el-radio-group v-model="filter" class="mb" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="SUBMITTED">待处理</el-radio-button>
        <el-radio-button value="PROCESSING">处理中</el-radio-button>
        <el-radio-button value="RESOLVED">已办结</el-radio-button>
      </el-radio-group>
      <el-table :data="filtered" size="small">
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.reporterType === 'VENDOR' ? 'primary' : 'danger'">
              {{ row.reporterType === 'VENDOR' ? '摊主申诉' : '消费者投诉' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="事项" width="120">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="涉及摊位/摊主" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.result" size="small">{{ row.result.stallNo }}</el-tag>
            {{ row.vendor?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="投诉人" width="100" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="{ SUBMITTED: 'warning', PROCESSING: 'primary', RESOLVED: 'success', REJECTED: 'info' }[row.status]">
              {{ { SUBMITTED: '待处理', PROCESSING: '处理中', RESOLVED: '已办结', REJECTED: '已驳回' }[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="!['RESOLVED', 'REJECTED'].includes(row.status)" size="small" type="primary" @click="openHandle(row)">处理</el-button>
            <el-button v-else size="small" @click="openHandle(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="handleVisible" title="处理投诉/申诉" width="520px">
      <template v-if="current">
        <el-descriptions :column="1" border class="mb">
          <el-descriptions-item label="类型">{{ typeText(current.type) }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ current.description }}</el-descriptions-item>
          <el-descriptions-item label="投诉人">{{ current.reporterName }} {{ current.reporterPhone }}</el-descriptions-item>
        </el-descriptions>
        <el-form :model="handleForm" label-width="100px">
          <el-form-item label="处理状态">
            <el-radio-group v-model="handleForm.status">
              <el-radio value="PROCESSING">处理中</el-radio>
              <el-radio value="RESOLVED">办结</el-radio>
              <el-radio value="REJECTED">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理说明" required>
            <el-input v-model="handleForm.resolution" type="textarea" :rows="3"
              placeholder="处理结果将向投诉人与摊主公开" />
          </el-form-item>
          <el-form-item label="信用联动" v-if="current.vendor">
            <el-input-number v-model="handleForm.creditDelta" :min="-30" :max="10" />
            <span class="text-muted" style="margin-left:8px">负值扣分，0 不变</span>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handling" @click="submitHandle">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'
import { fmtTime } from '../../utils/dict'

const complaints = ref([])
const filter = ref('')
const handleVisible = ref(false)
const handling = ref(false)
const current = ref(null)
const handleForm = ref({ status: 'RESOLVED', resolution: '', creditDelta: 0 })

const filtered = computed(() => filter.value ? complaints.value.filter(c => c.status === filter.value) : complaints.value)

function typeText(t) {
  return { DIARRHEA: '腹泻/食品安全', PRICE: '价格问题', SHORT_WEIGHT: '缺斤少两', HYGIENE: '卫生问题', QUALITY: '质量问题', BOUNDARY: '边界纠纷', FEE_DISPUTE: '费用争议', DEPOSIT_DISPUTE: '扣罚争议', OTHER: '其他' }[t] || t
}

async function load() { complaints.value = await api.get('/admin/complaints') }

function openHandle(row) {
  current.value = row
  handleForm.value = { status: 'RESOLVED', resolution: row.resolution || '', creditDelta: 0 }
  handleVisible.value = true
}

async function submitHandle() {
  if (!handleForm.value.resolution) return ElMessage.warning('请填写处理说明')
  handling.value = true
  try {
    await api.post(`/admin/complaints/${current.value.id}/handle`, handleForm.value)
    ElMessage.success('已处理')
    handleVisible.value = false
    await load()
  } finally { handling.value = false }
}

onMounted(load)
</script>
