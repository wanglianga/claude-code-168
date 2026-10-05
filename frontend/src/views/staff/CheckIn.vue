<template>
  <div>
    <h2 class="page-title">扫码入场核验</h2>
    <p class="page-desc">工作人员扫描摊主出示的核销码（或手工输入），核对身份、摊位与缴费状态，并拍摄摊位布置照片完成入场。</p>

    <el-row :gutter="14">
      <el-col :span="10">
        <el-card>
          <template #header><b>核销码核验</b></template>
          <el-input v-model="code" size="large" placeholder="扫描/粘贴核销码，如 CHK2-3-xxxx" clearable
                    @keyup.enter="lookup">
            <template #prepend><el-icon><Aim /></el-icon></template>
          </el-input>
          <el-button type="primary" size="large" style="width:100%;margin-top:10px" @click="lookup">查 询</el-button>
          <el-divider>本场入场记录</el-divider>
          <el-table :data="checkIns" stripe max-height="420" size="small">
            <el-table-column label="时间" width="140">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="摊主" min-width="130">
              <template #default="{ row }">{{ row.allocation?.vendor?.name }}</template>
            </el-table-column>
            <el-table-column label="摊位" width="90">
              <template #default="{ row }">{{ row.allocation?.stall?.stallNo }}</template>
            </el-table-column>
            <el-table-column label="布置照片" width="90">
              <template #default="{ row }">
                <PhotoView v-if="row.setupPhotos" :urls="row.setupPhotos" />
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card v-if="data" :class="{'ok-card': canCheckIn}">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>核验信息</b>
              <el-tag :type="data.allocation.feeStatus==='PAID' ? 'success' : 'danger'">
                {{ feeLabel(data.allocation.feeStatus) }}（待收 {{ data.outstanding }} 元）
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="摊主"><b>{{ data.allocation.vendor.name }}</b>（{{ data.allocation.vendor.phone }}）</el-descriptions-item>
            <el-descriptions-item label="信用分">{{ data.allocation.vendor.creditScore }}</el-descriptions-item>
            <el-descriptions-item label="摊位号"><b style="font-size:22px;color:#2f6b47">{{ data.allocation.stall?.stallNo }}</b>（{{ data.allocation.stall?.zone?.name }}）</el-descriptions-item>
            <el-descriptions-item label="状态">{{ allocLabel(data.allocation.status) }}</el-descriptions-item>
            <el-descriptions-item label="应入场时间">{{ data.allocation.entryTime }}</el-descriptions-item>
            <el-descriptions-item label="核销码">{{ data.allocation.checkCode }}</el-descriptions-item>
          </el-descriptions>
          <el-alert v-if="data.allocation.prohibitions" type="warning" :closable="false" show-icon
                    :title="`禁止/注意事项：${data.allocation.prohibitions}`" style="margin-top:12px" />

          <el-divider>拍摄摊位布置照片（入场必拍）</el-divider>
          <PhotoUpload v-model="setupPhotos" :limit="4" />
          <el-input v-model="remark" type="textarea" :rows="2" style="margin-top:12px"
                    placeholder="现场备注，如：黄线内经营、电源接入正常" />
          <div style="margin-top:14px">
            <el-button type="success" size="large" :loading="busy" :disabled="!canCheckIn" @click="confirm">
              <el-icon><CircleCheck /></el-icon> 确认入场核验
            </el-button>
            <el-tag v-if="data.allocation.feeStatus==='UNPAID'" type="danger" style="margin-left:12px">费用未缴清，无法入场</el-tag>
            <el-tag v-if="data.allocation.status==='WAITLIST'" type="danger" style="margin-left:12px">候补状态，无法入场</el-tag>
          </div>
        </el-card>
        <el-empty v-else description="扫码或输入核销码后显示核验信息" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { store } from '../../store.js'
import PhotoUpload from '../../components/PhotoUpload.vue'
import PhotoView from '../../components/PhotoView.vue'

const code = ref('')
const data = ref(null)
const checkIns = ref([])
const setupPhotos = ref('')
const remark = ref('')
const busy = ref(false)

const canCheckIn = computed(() => data.value
  && data.value.allocation.feeStatus !== 'UNPAID'
  && ['ALLOCATED', 'ADJUSTED'].includes(data.value.allocation.status))

function feeLabel(s) { return { UNPAID: '未缴', PAID: '已缴', WAIVED: '已减免' }[s] || s }
function allocLabel(s) { return { ALLOCATED: '已中签待入场', CHECKED_IN: '已入场', ADJUSTED: '已临时调位', NO_SHOW: '未到场', CANCELLED: '已取消', WAITLIST: '候补未中签' }[s] || s }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function lookup() {
  if (!code.value.trim()) return
  data.value = await api.get(`/allocations/by-code/${encodeURIComponent(code.value.trim())}`)
}
async function loadList() {
  if (!store.fairDayId) return
  checkIns.value = await api.get('/check-ins', { params: { fairDayId: store.fairDayId } })
}
async function confirm() {
  if (!setupPhotos.value) return ElMessage.warning('请拍摄摊位布置照片')
  busy.value = true
  try {
    await api.post('/check-ins', { code: code.value.trim(), setupPhotos: setupPhotos.value, remark: remark.value })
    ElMessage.success('入场核验成功')
    setupPhotos.value = ''
    remark.value = ''
    await Promise.all([lookup(), loadList()])
  } finally {
    busy.value = false
  }
}
watch(() => store.fairDayId, () => { data.value = null; loadList() }, { immediate: true })
</script>

<style scoped>
.ok-card { border-top: 3px solid #2f6b47; }
</style>
