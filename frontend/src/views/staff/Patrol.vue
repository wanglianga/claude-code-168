<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :span="9">
        <el-card>
          <div class="card-title">巡查登记</div>
          <el-form :model="form" label-width="90px">
            <el-form-item label="集市">
              <el-select v-model="form.eventId" style="width: 100%" @change="loadAssignments">
                <el-option v-for="e in events" :key="e.id" :label="`${e.name}（${e.marketDate}）`" :value="e.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="摊位" required>
              <el-select v-model="form.resultId" style="width: 100%" filterable>
                <el-option v-for="a in assignments" :key="a.result.id"
                  :label="`${a.result.stallNo} - ${a.result.application?.vendor?.name}`" :value="a.result.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="卫生评分" required>
              <el-rate v-model="form.hygieneScore" :max="5" />
            </el-form-item>
            <el-form-item label="明码标价">
              <el-switch v-model="form.priceMarked" active-text="规范" inactive-text="未标价" />
            </el-form-item>
            <el-form-item label="缺斤少两">
              <el-switch v-model="form.shortWeight" active-text="发现" inactive-text="无"
                active-color="#f56c6c" />
            </el-form-item>
            <el-form-item label="占道经营">
              <el-switch v-model="form.occupyingLane" active-text="占道" inactive-text="无"
                active-color="#f56c6c" />
            </el-form-item>
            <el-form-item label="用电安全">
              <el-switch v-model="form.electricSafe" active-text="安全" inactive-text="隐患" />
            </el-form-item>
            <el-form-item label="现场照片">
              <ImageUpload v-model="form.photos" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.notes" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="submit">提交巡查记录</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="15">
        <el-card>
          <div class="card-title">巡查记录</div>
          <el-table :data="patrols" size="small" max-height="640">
            <el-table-column label="摊位" width="80">
              <template #default="{ row }"><el-tag size="small">{{ row.result?.stallNo }}</el-tag></template>
            </el-table-column>
            <el-table-column label="摊主" width="120">
              <template #default="{ row }">{{ row.result?.application?.vendor?.name }}</template>
            </el-table-column>
            <el-table-column label="卫生" width="70">
              <template #default="{ row }">{{ row.hygieneScore }}分</template>
            </el-table-column>
            <el-table-column label="问题" min-width="180">
              <template #default="{ row }">
                <el-tag v-if="!row.priceMarked" type="warning" size="small" class="mr">未明码标价</el-tag>
                <el-tag v-if="row.shortWeight" type="danger" size="small" class="mr">缺斤少两</el-tag>
                <el-tag v-if="row.occupyingLane" type="danger" size="small" class="mr">占道</el-tag>
                <el-tag v-if="!row.electricSafe" type="warning" size="small" class="mr">用电隐患</el-tag>
                <span v-if="row.priceMarked && !row.shortWeight && !row.occupyingLane && row.electricSafe"
                  class="text-muted">无异常</span>
              </template>
            </el-table-column>
            <el-table-column label="巡查人" width="130">
              <template #default="{ row }">{{ row.inspector?.name }}</template>
            </el-table-column>
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'
import { fmtTime } from '../../utils/dict'
import ImageUpload from '../../components/ImageUpload.vue'

const events = ref([])
const assignments = ref([])
const patrols = ref([])
const saving = ref(false)
const form = ref({
  eventId: null, resultId: null, hygieneScore: 5,
  priceMarked: true, shortWeight: false, occupyingLane: false, electricSafe: true,
  photos: '', notes: ''
})

async function loadAssignments() {
  if (!form.value.eventId) return
  const [asgs, ps] = await Promise.all([
    api.get(`/staff/events/${form.value.eventId}/assignments`),
    api.get(`/staff/events/${form.value.eventId}/patrols`)
  ])
  assignments.value = asgs
  patrols.value = ps
}

async function submit() {
  if (!form.value.resultId) return ElMessage.warning('请选择摊位')
  saving.value = true
  try {
    await api.post('/staff/patrols', form.value)
    ElMessage.success('巡查记录已提交')
    form.value.notes = ''
    form.value.photos = ''
    await loadAssignments()
  } finally { saving.value = false }
}

onMounted(async () => {
  events.value = await api.get('/meta/events')
  const active = events.value.find(e => ['LOTTERY_DONE', 'IN_PROGRESS'].includes(e.status))
  if (active) { form.value.eventId = active.id; await loadAssignments() }
})
</script>

<style scoped>
.mr { margin-right: 4px; }
</style>
