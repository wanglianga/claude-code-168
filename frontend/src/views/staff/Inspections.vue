<template>
  <div>
    <h2 class="page-title">市场巡查</h2>
    <p class="page-desc">对入场摊位记录卫生、明码标价、缺斤少两（计量）、占道、用电安全五项评分，发现问题责令当场整改。</p>

    <el-row :gutter="14">
      <el-col :span="9">
        <el-card>
          <template #header><b>新增巡查记录 — {{ store.currentFairDay?.name || '请选择集市' }}</b></template>
          <el-form label-width="92px">
            <el-form-item label="被巡查摊主" required>
              <el-select v-model="form.vendorId" filterable placeholder="选择摊主" style="width:100%">
                <el-option v-for="v in vendors" :key="v.id"
                           :label="`${v.name}（信用${v.creditScore}）`" :value="v.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="关联摊位">
              <el-select v-model="form.allocationId" clearable placeholder="可不选（如未到场摊位）" style="width:100%">
                <el-option v-for="a in allocations" :key="a.id"
                           :label="`${a.stall?.stallNo || '候补'} ${a.vendor.name}`" :value="a.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="卫生">
              <el-rate v-model="form.hygieneScore" show-score />
            </el-form-item>
            <el-form-item label="明码标价">
              <el-rate v-model="form.priceTagScore" show-score />
            </el-form-item>
            <el-form-item label="计量合规">
              <el-rate v-model="form.measureScore" show-score />
            </el-form-item>
            <el-form-item label="不占道">
              <el-rate v-model="form.aisleScore" show-score />
            </el-form-item>
            <el-form-item label="用电安全">
              <el-rate v-model="form.electricScore" show-score />
            </el-form-item>
            <el-form-item label="发现问题">
              <el-checkbox-group v-model="form.issueList">
                <el-checkbox v-for="i in issueOptions" :key="i" :label="i" border size="small" style="margin:3px 6px 3px 0" />
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="现场照片">
              <PhotoUpload v-model="form.photos" :limit="4" />
            </el-form-item>
            <el-form-item label="情况说明">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="form.requireRectify">责令当场整改并复查</el-checkbox>
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="submit">提交巡查记录</el-button>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="15">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>本场巡查记录</b>
              <el-button @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
            </div>
          </template>
          <el-table :data="list" stripe>
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="摊主" min-width="130">
              <template #default="{ row }">{{ row.vendor?.name }}</template>
            </el-table-column>
            <el-table-column label="摊位" width="80">
              <template #default="{ row }">{{ row.allocation?.stall?.stallNo || '—' }}</template>
            </el-table-column>
            <el-table-column label="五项评分" width="210">
              <template #default="{ row }">
                <el-rate :model-value="row.hygieneScore" disabled size="small" />
                <el-rate :model-value="row.priceTagScore" disabled size="small" />
                <el-rate :model-value="row.measureScore" disabled size="small" />
                <el-rate :model-value="row.aisleScore" disabled size="small" />
                <el-rate :model-value="row.electricScore" disabled size="small" />
              </template>
            </el-table-column>
            <el-table-column label="问题" min-width="150">
              <template #default="{ row }">
                <el-tag v-for="i in splitTags(row.issues)" :key="i" type="danger" size="small" style="margin:2px">{{ i }}</el-tag>
                <span v-if="!row.issues" class="card-msg">无</span>
              </template>
            </el-table-column>
            <el-table-column label="整改" width="70">
              <template #default="{ row }">
                <el-tag :type="row.requireRectify ? 'danger' : 'info'" size="small">{{ row.requireRectify ? '责令整改' : '—' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="照片/说明" min-width="180">
              <template #default="{ row }">
                <PhotoView v-if="row.photos" :urls="row.photos" />
                <div class="card-msg">{{ row.remark }}</div>
              </template>
            </el-table-column>
            <el-table-column label="巡查人" width="90">
              <template #default="{ row }">{{ row.staff?.name }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { store } from '../../store.js'
import PhotoUpload from '../../components/PhotoUpload.vue'
import PhotoView from '../../components/PhotoView.vue'

const issueOptions = ['卫生较差', '未明码标价', '缺斤少两', '占道经营', '私拉电线', '用电不规范', '未佩戴口罩手套', '活禽隔离不到位', '留样缺失']
const vendors = ref([])
const allocations = ref([])
const list = ref([])
const saving = ref(false)
const form = reactive({
  vendorId: null, allocationId: null,
  hygieneScore: 5, priceTagScore: 5, measureScore: 5, aisleScore: 5, electricScore: 5,
  issueList: [], photos: '', remark: '', requireRectify: false
})

function splitTags(s) { return s ? s.split(',').map((x) => x.trim()).filter(Boolean) : [] }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function loadBase() {
  vendors.value = await api.get('/users/vendors')
  if (!store.fairDayId) { allocations.value = []; return }
  allocations.value = await api.get(`/draw/${store.fairDayId}/allocations`)
}
async function load() {
  if (store.fairDayId) list.value = await api.get('/inspections', { params: { fairDayId: store.fairDayId } })
}
async function submit() {
  if (!form.vendorId) return ElMessage.warning('请选择被巡查摊主')
  const payload = {
    fairDayId: store.fairDayId, vendorId: form.vendorId, allocationId: form.allocationId,
    inspection: {
      hygieneScore: form.hygieneScore, priceTagScore: form.priceTagScore, measureScore: form.measureScore,
      aisleScore: form.aisleScore, electricScore: form.electricScore,
      issues: form.issueList.join(','), photos: form.photos, remark: form.remark, requireRectify: form.requireRectify
    }
  }
  saving.value = true
  try {
    await api.post('/inspections', payload)
    ElMessage.success('巡查记录已提交')
    Object.assign(form, {
      allocationId: null, issueList: [], photos: '', remark: '', requireRectify: false,
      hygieneScore: 5, priceTagScore: 5, measureScore: 5, aisleScore: 5, electricScore: 5
    })
    load()
  } finally {
    saving.value = false
  }
}
watch(() => store.fairDayId, () => { loadBase(); load() }, { immediate: true })
</script>
