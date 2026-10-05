<template>
  <div>
    <h2 class="page-title">摊主信用管理</h2>
    <p class="page-desc">信用分初始 100 分：投诉查实、巡查违规等扣分，文明经营、主动配合处置可加分。任何加减分均须填写来源与理由，进入公示台账。</p>
    <el-card>
      <template #header><b>摊主信用分名录</b></template>
      <el-table :data="vendors" stripe>
        <el-table-column prop="name" label="摊主" min-width="160" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="address" label="住址" min-width="120" />
        <el-table-column label="信用分" width="130">
          <template #default="{ row }">
            <el-progress :percentage="creditPct(row.creditScore)" :color="creditColor(row.creditScore)" :stroke-width="14" :show-text="false" />
            <b :style="{ color: creditColor(row.creditScore), marginLeft: '8px' }">{{ row.creditScore }}</b>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="110">
          <template #default="{ row }"><el-tag :type="creditTag(row.creditScore)">{{ creditLevel(row.creditScore) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="openAdjust(row, 5)">加分</el-button>
            <el-button size="small" type="danger" @click="openAdjust(row, -5)">扣分</el-button>
          </template>
        </el-table-column>
        <el-table-column label="近期调整" min-width="260">
          <template #default="{ row }">
            <div v-for="l in (row._ledger || []).slice(0, 3)" :key="l.id" class="card-msg">
              {{ fmt(l.createdAt) }} {{ l.creditDelta > 0 ? '+' : '' }}{{ l.creditDelta }}：{{ l.reason }}
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="信用分调整" width="520px">
      <el-descriptions :column="1" border v-if="target">
        <el-descriptions-item label="摊主">{{ target.name }}（当前 {{ target.creditScore }} 分）</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="100px" style="margin-top:12px">
        <el-form-item label="调整分值">
          <el-input-number v-model="form.delta" :step="5" /> 分
        </el-form-item>
        <el-form-item label="关联集市">
          <el-select v-model="form.fairDayId" clearable placeholder="不关联具体集市" style="width:100%">
            <el-option v-for="d in fairs" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="form.sourceType" style="width:220px">
            <el-option label="人工核查" value="人工核查" />
            <el-option label="巡查记录" value="巡查" />
            <el-option label="投诉处理" value="投诉" />
            <el-option label="表彰奖励" value="表彰奖励" />
          </el-select>
        </el-form-item>
        <el-form-item label="理由（必填）">
          <el-input v-model="form.reason" type="textarea" :rows="3"
                    placeholder="说明事实依据与来源，将向摊主公示" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submit">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'

const vendors = ref([])
const fairs = ref([])
const dialogVisible = ref(false)
const target = ref(null)
const form = reactive({ delta: 0, fairDayId: null, sourceType: '人工核查', reason: '' })

function creditPct(v) { return Math.max(0, Math.min(100, (v / 120) * 100)) }
function creditColor(v) { return v >= 100 ? '#2f6b47' : v >= 80 ? '#d97706' : '#c62828' }
function creditLevel(v) { return v >= 100 ? '信用良好' : v >= 80 ? '一般关注' : '重点监管' }
function creditTag(v) { return v >= 100 ? 'success' : v >= 80 ? 'warning' : 'danger' }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function load() {
  const [vs, fs] = await Promise.all([api.get('/users/vendors'), api.get('/fair-days')])
  fairs.value = fs
  const allLedger = await api.get('/ledger')
  for (const v of vs) v._ledger = allLedger.filter((l) => l.vendor?.id === v.id)
  vendors.value = vs
}
function openAdjust(row, delta) {
  target.value = row
  Object.assign(form, { delta, fairDayId: null, sourceType: delta > 0 ? '表彰奖励' : '人工核查', reason: '' })
  dialogVisible.value = true
}
async function submit() {
  if (!form.reason) return ElMessage.warning('理由（事实依据）必填')
  await api.post('/users/credit-adjust', { ...form, vendorId: target.value.id })
  ElMessage.success('信用分已调整并记入公示台账')
  dialogVisible.value = false
  load()
}
onMounted(load)
</script>
