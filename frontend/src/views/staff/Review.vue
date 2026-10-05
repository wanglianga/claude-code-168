<template>
  <div>
    <h2 class="page-title">报名审核</h2>
    <p class="page-desc">核验经营资质（熟食必须有健康证/备案照片）、确认历史赶集次数（老摊主优先依据），通过后方可进入抽签。</p>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>{{ store.currentFairDay?.name || '请选择集市' }} · 报名审核</b>
          <el-radio-group v-model="filter" size="small">
            <el-radio-button label="ALL">全部</el-radio-button>
            <el-radio-button label="SUBMITTED">待审核</el-radio-button>
            <el-radio-button label="APPROVED">已通过</el-radio-button>
            <el-radio-button label="REJECTED">已驳回</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="filtered" stripe>
        <el-table-column label="摊主" min-width="150">
          <template #default="{ row }">
            <b>{{ row.vendor.name }}</b>
            <div class="card-msg">{{ row.vendor.phone }} · {{ row.vendor.address }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="品类" width="110">
          <template #default="{ row }">
            {{ row.category }}
            <el-tag v-if="row.cookedFood" type="warning" size="small" style="margin-left:4px">熟食</el-tag>
            <el-tag v-if="row.livePoultry" type="danger" size="small" style="margin-left:4px">活禽</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="车辆/用电" width="150">
          <template #default="{ row }">
            {{ row.vehicleLength }}×{{ row.vehicleWidth }}m
            <el-tag v-if="row.needElectricity" type="warning" size="small" style="margin-left:4px">{{ row.powerWatt }}W</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="证照" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.licensePhotos" type="success" size="small">已上传</el-tag>
            <el-tag v-else :type="row.cookedFood ? 'danger' : 'info'" size="small">{{ row.cookedFood ? '缺证照!' : '无' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="historyCount" label="赶集次数" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="tag(row.status)">{{ label(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="view(row)">详情</el-button>
            <el-button v-if="row.status==='SUBMITTED'" size="small" type="success" @click="openApprove(row)">通过</el-button>
            <el-button v-if="row.status==='SUBMITTED'" size="small" type="danger" @click="openReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" size="56%" :title="`报名详情 — ${current?.vendor?.name || ''}`">
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品描述">{{ current.goodsDesc }}</el-descriptions-item>
          <el-descriptions-item label="历史经营记录">{{ current.historyRecord }}</el-descriptions-item>
          <el-descriptions-item label="自报次数 / 信用分">{{ current.historyCount }} 次 / {{ current.vendor.creditScore }} 分</el-descriptions-item>
          <el-descriptions-item label="证照照片"><PhotoView :urls="current.licensePhotos" /></el-descriptions-item>
          <el-descriptions-item label="审核备注">{{ current.reviewNote || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="current.rejectReason" label="驳回原因">
            <span style="color:#c62828">{{ current.rejectReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog v-model="approveVisible" title="审核通过" width="460px">
      <el-alert v-if="current?.cookedFood && !current.licensePhotos" type="error" :closable="false" show-icon
                title="该摊主申报熟食但未上传证照照片，不能审核通过" style="margin-bottom:12px" />
      <el-form label-width="150px">
        <el-form-item label="核实赶集次数">
          <el-input-number v-model="approveForm.historyCount" :min="0" :max="200" />
          <span class="card-msg" style="margin-left:8px">老摊主优先权重 = 次数×3 + (信用分−100)</span>
        </el-form-item>
        <el-form-item label="审核备注"><el-input v-model="approveForm.note" type="textarea" :rows="2" placeholder="证照齐全，资格核实无误" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible=false">取消</el-button>
        <el-button type="primary" :disabled="current?.cookedFood && !current.licensePhotos" @click="doApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="审核驳回" width="460px">
      <el-form label-width="100px">
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectNote" type="textarea" :rows="3" placeholder="如：熟食未上传健康证；资质不符" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible=false">取消</el-button>
        <el-button type="danger" @click="doReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { store } from '../../store.js'
import PhotoView from '../../components/PhotoView.vue'

const list = ref([])
const filter = ref('SUBMITTED')
const detailVisible = ref(false)
const approveVisible = ref(false)
const rejectVisible = ref(false)
const current = ref(null)
const approveForm = reactive({ historyCount: 0, note: '' })
const rejectNote = ref('')

const filtered = computed(() => filter.value === 'ALL' ? list.value : list.value.filter((a) => a.status === filter.value))
function label(s) { return { SUBMITTED: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[s] }
function tag(s) { return { SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] }

async function load() {
  if (!store.fairDayId) { list.value = []; return }
  list.value = await api.get('/applications', { params: { fairDayId: store.fairDayId } })
}
function view(row) { current.value = row; detailVisible.value = true }
function openApprove(row) {
  current.value = row
  Object.assign(approveForm, { historyCount: row.historyCount, note: '' })
  approveVisible.value = true
}
function openReject(row) { current.value = row; rejectNote.value = ''; rejectVisible.value = true }
async function doApprove() {
  await api.put(`/applications/${current.value.id}/review`, { approved: true, note: approveForm.note, historyCount: approveForm.historyCount })
  ElMessage.success('已审核通过')
  approveVisible.value = false
  load()
}
async function doReject() {
  if (!rejectNote.value) return ElMessage.warning('请填写驳回原因')
  await api.put(`/applications/${current.value.id}/review`, { approved: false, note: rejectNote.value })
  ElMessage.success('已驳回')
  rejectVisible.value = false
  load()
}
watch(() => store.fairDayId, load, { immediate: true })
</script>
