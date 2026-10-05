<template>
  <div>
    <h2 class="page-title">消费者投诉处理</h2>
    <p class="page-desc">登记腹泻疑似食物中毒、缺斤少两、价格纠纷等投诉；查实时可联动保证金扣罚与信用扣分，全部进入调整公示台账。</p>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>投诉登记</b>
          <div>
            <el-select v-model="filterStatus" size="small" style="width:140px" clearable placeholder="全部状态">
              <el-option label="已受理" value="RECEIVED" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已办结" value="RESOLVED" />
              <el-option label="驳回" value="DISMISSED" />
            </el-select>
            <el-button type="primary" size="small" @click="openCreate" style="margin-left:10px"><el-icon><Plus /></el-icon> 登记投诉</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filtered" stripe>
        <el-table-column label="登记时间" width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="150">
          <template #default="{ row }">
            <el-tag :type="row.type.includes('腹泻') ? 'danger' : 'warning'" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投诉人" width="130">
          <template #default="{ row }">{{ row.complainant }} <span class="card-msg">{{ row.contact }}</span></template>
        </el-table-column>
        <el-table-column label="被投诉摊主" min-width="140">
          <template #default="{ row }">{{ row.vendor?.name || '—' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="投诉内容" min-width="240" />
        <el-table-column label="证据" width="100">
          <template #default="{ row }"><PhotoView v-if="row.evidencePhotos" :urls="row.evidencePhotos" /></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="处置结果" min-width="240">
          <template #default="{ row }">
            <div>{{ row.handleResult || '—' }}</div>
            <div class="card-msg" v-if="row.handler">处理人：{{ row.handler.name }} {{ fmt(row.handledAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="warning" @click="openHandle(row)" :disabled="row.status==='RESOLVED' || row.status==='DISMISSED'">处置</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="登记消费者投诉" width="560px">
      <el-form :model="cf" label-width="100px">
        <el-form-item label="所属集市" required>
          <el-select v-model="cf.fairDayId" placeholder="选择集市" style="width:100%">
            <el-option v-for="d in fairs" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="投诉类型" required>
          <el-select v-model="cf.type" style="width:220px">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="投诉人" required><el-input v-model="cf.complainant" /></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="cf.contact" placeholder="可选" /></el-form-item>
        <el-form-item label="被投诉摊主">
          <el-select v-model="cf.vendorId" filterable clearable placeholder="可关联摊主" style="width:100%">
            <el-option v-for="v in vendors" :key="v.id" :label="`${v.name}（信用${v.creditScore}）`" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="投诉内容" required>
          <el-input v-model="cf.content" type="textarea" :rows="3" placeholder="腹泻时间、购买食品、症状等" />
        </el-form-item>
        <el-form-item label="证据照片"><PhotoUpload v-model="cf.evidencePhotos" :limit="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible=false">取消</el-button>
        <el-button type="primary" @click="doCreate">登记受理</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleVisible" title="投诉处置" width="640px">
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="投诉">{{ current.type }} — {{ current.complainant }}（{{ current.contact }}）</el-descriptions-item>
          <el-descriptions-item label="内容">{{ current.content }}</el-descriptions-item>
          <el-descriptions-item label="被投诉摊主">{{ current.vendor?.name || '未关联摊主' }}</el-descriptions-item>
        </el-descriptions>
        <el-form label-width="120px" style="margin-top:16px">
          <el-form-item label="处置结果" required>
            <el-radio-group v-model="hf.status">
              <el-radio value="RESOLVED">查办结案</el-radio>
              <el-radio value="PROCESSING">继续处理</el-radio>
              <el-radio value="DISMISSED">驳回投诉</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处置说明" required>
            <el-input v-model="hf.handleResult" type="textarea" :rows="3"
                      placeholder="核查事实、调解/赔偿、整改要求等" />
          </el-form-item>
          <el-form-item label="关联抽签记录">
            <el-select v-model="hf.allocationId" clearable filterable placeholder="选择当场抽签记录（扣罚保证金必填）" style="width:100%">
              <el-option v-for="a in allocations" :key="a.id"
                         :label="`${a.stall?.stallNo || '候补'} ${a.vendor.name}`" :value="a.id" />
            </el-select>
          </el-form-item>
          <el-divider content-position="left">联动保证金扣罚 / 信用扣分（查实后填写）</el-divider>
          <el-form-item>
            <el-checkbox v-model="hf.deduct">查实为违规，扣罚保证金</el-checkbox>
          </el-form-item>
          <el-form-item v-if="hf.deduct" label="扣罚金额">
            <el-input-number v-model="hf.deductAmount" :min="1" :step="10" /> 元
          </el-form-item>
          <el-form-item v-if="hf.deduct" label="信用扣分">
            <el-input-number v-model="hf.creditDelta" :max="-1" :step="-5" /> 分（如 -10）
          </el-form-item>
          <el-form-item v-if="hf.deduct" label="事实依据">
            <el-input v-model="hf.reason" type="textarea" :rows="2"
                      placeholder="如：留样温度超标 + 操作不规范（此理由将写入公示台账）" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="handleVisible=false">取消</el-button>
        <el-button type="primary" @click="doHandle">提交处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { store } from '../../store.js'
import PhotoUpload from '../../components/PhotoUpload.vue'
import PhotoView from '../../components/PhotoView.vue'

const types = ['腹泻疑似食物中毒', '缺斤少两', '价格纠纷', '卫生问题', '占道经营', '用电安全', '服务态度', '其他']
const list = ref([])
const fairs = ref([])
const vendors = ref([])
const allocations = ref([])
const filterStatus = ref('')
const createVisible = ref(false)
const handleVisible = ref(false)
const current = ref(null)
const cf = reactive({ fairDayId: null, type: '', complainant: '', contact: '', vendorId: null, content: '', evidencePhotos: '' })
const hf = reactive({ status: 'RESOLVED', handleResult: '', deduct: false, deductAmount: 50, creditDelta: -10, allocationId: null, reason: '' })

const filtered = computed(() => filterStatus.value ? list.value.filter((c) => c.status === filterStatus.value) : list.value)
function statusLabel(s) { return { RECEIVED: '已受理', PROCESSING: '处理中', RESOLVED: '已办结', DISMISSED: '驳回' }[s] || s }
function statusTag(s) { return { RECEIVED: 'warning', PROCESSING: 'primary', RESOLVED: 'success', DISMISSED: 'info' }[s] }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function load() { list.value = await api.get('/complaints') }
async function loadMeta() {
  fairs.value = await api.get('/fair-days')
  vendors.value = await api.get('/users/vendors')
}
function openCreate() {
  Object.assign(cf, { fairDayId: store.fairDayId, type: '', complainant: '', contact: '', vendorId: null, content: '', evidencePhotos: '' })
  createVisible.value = true
}
async function doCreate() {
  if (!cf.fairDayId || !cf.type || !cf.complainant || !cf.content) return ElMessage.warning('请填写完整投诉信息')
  await api.post('/complaints', {
    ...cf, fairDay: { id: cf.fairDayId },
    vendor: cf.vendorId ? { id: cf.vendorId } : null
  })
  ElMessage.success('投诉已登记受理')
  createVisible.value = false
  load()
}
async function openHandle(row) {
  current.value = row
  allocations.value = row.fairDayId ? await api.get(`/draw/${row.fairDayId}/allocations`) : []
  Object.assign(hf, { status: 'RESOLVED', handleResult: '', deduct: false, deductAmount: 50, creditDelta: -10, allocationId: row.allocationId || null, reason: '' })
  handleVisible.value = true
}
async function doHandle() {
  if (!hf.handleResult) return ElMessage.warning('请填写处置说明')
  if (hf.deduct && !hf.allocationId) return ElMessage.warning('扣罚保证金必须关联当场抽签记录')
  if (hf.deduct && !hf.reason) return ElMessage.warning('扣罚必须填写事实依据（写入公示台账）')
  await api.put(`/complaints/${current.value.id}/handle`, {
    status: hf.status, handleResult: hf.handleResult,
    deduct: hf.deduct, deductAmount: hf.deductAmount, creditDelta: hf.creditDelta,
    allocationId: hf.allocationId, reason: hf.reason
  })
  ElMessage.success('处置完成')
  handleVisible.value = false
  load()
}
loadMeta()
load()
</script>
