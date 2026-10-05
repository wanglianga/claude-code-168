<template>
  <div>
    <h2 class="page-title">赶集报名</h2>
    <p class="page-desc">在赶集日前提交经营品类、车辆尺寸、用电需求、熟食情况、证照照片与历史经营记录，供管理人员审核并组织抽签。</p>

    <el-alert v-if="!store.fairDayId" type="warning" :closable="false" title="请先在顶部选择赶集日" style="margin-bottom:14px" />

    <el-row :gutter="14" v-if="store.fairDayId">
      <el-col :span="15">
        <el-card>
          <template #header>
            <b>报名信息 — {{ store.currentFairDay?.name }}</b>
            <el-tag style="margin-left:10px" :type="dayTag">{{ dayLabel }}</el-tag>
          </template>
          <el-form label-width="120px">
            <el-form-item label="经营品类" required>
              <el-select v-model="form.category" placeholder="选择主营品类" style="width: 240px">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
              <el-checkbox v-model="form.cookedFood" style="margin-left:16px">售卖熟食/凉菜/糕点（需留样）</el-checkbox>
              <el-checkbox v-model="form.livePoultry">售卖活禽</el-checkbox>
            </el-form-item>
            <el-form-item label="主要商品描述" required>
              <el-input v-model="form.goodsDesc" maxlength="180" show-word-limit style="max-width:520px"
                        placeholder="如：卤鸡爪、卤豆干；自产应季蔬菜" />
            </el-form-item>
            <el-form-item label="车辆/摊棚尺寸">
              <el-input-number v-model="form.vehicleLength" :min="0" :max="12" :step="0.1" :precision="1" /> 米（长）
              <el-input-number v-model="form.vehicleWidth" :min="0" :max="8" :step="0.1" :precision="1" style="margin-left:10px" /> 米（宽）
              <span class="card-msg" style="margin-left:8px">抽签按尺寸匹配摊位，超尺寸将进候补</span>
            </el-form-item>
            <el-form-item label="用电需求">
              <el-switch v-model="form.needElectricity" active-text="需要接电" />
              <span v-if="form.needElectricity" style="margin-left:14px">
                功率 <el-input-number v-model="form.powerWatt" :min="100" :max="20000" :step="100" /> 瓦
              </span>
            </el-form-item>
            <el-form-item label="证照照片">
              <PhotoUpload v-model="form.licensePhotos" :limit="6" />
              <div class="card-msg">营业执照 / 健康证 / 小餐饮备案卡；熟食类必须上传，否则审核不通过</div>
            </el-form-item>
            <el-form-item label="历史经营记录">
              <el-input v-model="form.historyRecord" type="textarea" :rows="2" maxlength="300"
                        show-word-limit style="max-width:520px"
                        placeholder="历史赶集次数、文明经营/奖惩情况；老摊主身份由工作人员核实后计入优先权重" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="saving" @click="save">提交报名</el-button>
              <el-button size="large" @click="loadMine">刷新我的报名</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card>
          <template #header><b>我的报名状态</b></template>
          <el-empty v-if="!myApp" description="尚未报名" />
          <el-descriptions v-else :column="1" border>
            <el-descriptions-item label="状态">
              <el-tag :type="applyTag(myApp.status)">{{ applyLabel(myApp.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="品类">{{ myApp.category }}（{{ myApp.cookedFood ? '熟食' : '非熟食' }}{{ myApp.livePoultry ? '、活禽' : '' }}）</el-descriptions-item>
            <el-descriptions-item label="核实赶集次数">{{ myApp.historyCount }} 次（老摊主优先依据）</el-descriptions-item>
            <el-descriptions-item label="审核说明">{{ myApp.reviewNote || '—' }}</el-descriptions-item>
            <el-descriptions-item v-if="myApp.rejectReason" label="驳回原因">
              <span style="color:#c62828">{{ myApp.rejectReason }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
        <el-card style="margin-top:14px">
          <template #header><b>本场抽签规则（公示）</b></template>
          <div class="rules">{{ store.currentFairDay?.rulesText || '按当场公示规则执行：分区、消防通道、冷链、活禽隔离、老摊主与信用优先。' }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { store } from '../../store.js'
import PhotoUpload from '../../components/PhotoUpload.vue'

const categories = ['蔬菜', '水果', '卤味', '糕点', '凉菜', '烤鸭/熟食', '活禽', '水产', '服装', '百货杂货', '餐饮小吃', '其他']
const form = reactive({
  category: '', goodsDesc: '', cookedFood: false, livePoultry: false,
  vehicleLength: 3, vehicleWidth: 2, needElectricity: false, powerWatt: 1000,
  licensePhotos: '', historyRecord: '', historyCount: 0
})
const myApp = ref(null)
const saving = ref(false)
const dayLabel = computed(() => ({ PLANNED: '筹备中', OPEN: '报名中', DRAWN: '已抽签', ONGOING: '进行中', CLOSED: '已结束' }[store.currentFairDay?.status]))
const dayTag = computed(() => ({ PLANNED: 'info', OPEN: 'success', DRAWN: 'warning', ONGOING: 'danger', CLOSED: 'info' }[store.currentFairDay?.status]))

function applyLabel(s) { return { SUBMITTED: '待审核', APPROVED: '审核通过', REJECTED: '审核不通过' }[s] || s }
function applyTag(s) { return { SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] }

function fillFrom(app) {
  Object.assign(form, {
    category: app.category, goodsDesc: app.goodsDesc, cookedFood: app.cookedFood, livePoultry: app.livePoultry,
    vehicleLength: Number(app.vehicleLength), vehicleWidth: Number(app.vehicleWidth),
    needElectricity: app.needElectricity, powerWatt: app.powerWatt,
    licensePhotos: app.licensePhotos || '', historyRecord: app.historyRecord || '', historyCount: app.historyCount
  })
}

async function loadMine() {
  const list = await api.get('/applications/mine')
  myApp.value = list.find((a) => a.fairDay.id === store.fairDayId) || null
  if (myApp.value) fillFrom(myApp.value)
}

async function save() {
  if (!form.category || !form.goodsDesc) return ElMessage.warning('请填写品类和商品描述')
  if (form.cookedFood && !form.licensePhotos) return ElMessage.warning('售卖熟食必须上传健康证/备案照片')
  saving.value = true
  try {
    const saved = await api.post('/applications', { ...form, fairDay: { id: store.fairDayId } })
    ElMessage.success('报名已提交，等待工作人员审核')
    myApp.value = saved
  } finally {
    saving.value = false
  }
}

watch(() => store.fairDayId, () => { if (store.fairDayId) loadMine() }, { immediate: true })
</script>

<style scoped>
.rules { font-size: 13px; color: #555; line-height: 1.9; white-space: pre-wrap; }
</style>
