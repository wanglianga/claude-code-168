<template>
  <div>
    <h2 class="page-title">食品留样</h2>
    <p class="page-desc">售卖卤味、糕点、凉菜等食品的摊主须按批次上传留样照片、批次号和保存位置，留样不少于 125 克、冷藏 48 小时。</p>

    <el-card>
      <template #header><b>新增留样</b><span class="card-msg" style="margin-left:10px">集市：{{ store.currentFairDay?.name || '请先选择' }}</span></template>
      <el-form label-width="110px" style="max-width:760px">
        <el-form-item label="留样食品" required>
          <el-input v-model="form.foodName" placeholder="如：卤豆干（拼盘留样）" />
        </el-form-item>
        <el-form-item label="批次号" required>
          <el-input v-model="form.batchNo" placeholder="如：WL-1008-01" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="保存位置" required>
              <el-input v-model="form.storageLocation" placeholder="如：市场留样柜 B-2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="冷藏温度">
              <el-input-number v-model="form.storageTemp" :min="-5" :max="20" :step="0.5" :precision="1" /> ℃
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="留样时间">
          <el-date-picker v-model="form.sampledAt" type="datetime" placeholder="留空默认当前时间"
                          value-format="YYYY-MM-DDTHH:mm:ss" style="width:260px" />
        </el-form-item>
        <el-form-item label="留样照片">
          <PhotoUpload v-model="form.photos" :limit="4" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="密封留样盒、不少于125克等" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">提交留样</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header><b>我的留样记录</b></template>
      <el-table :data="list" stripe>
        <el-table-column prop="fairDay.name" label="集市" width="190" />
        <el-table-column prop="foodName" label="食品" width="150" />
        <el-table-column prop="batchNo" label="批次号" width="140" />
        <el-table-column prop="storageLocation" label="保存位置" width="150" />
        <el-table-column label="温度" width="80">
          <template #default="{ row }">{{ row.storageTemp ?? '—' }}℃</template>
        </el-table-column>
        <el-table-column label="留样时间" width="150">
          <template #default="{ row }">{{ fmt(row.sampledAt) }}</template>
        </el-table-column>
        <el-table-column label="照片" min-width="120">
          <template #default="{ row }"><PhotoView :urls="row.photos" /></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { store } from '../../store.js'
import PhotoUpload from '../../components/PhotoUpload.vue'
import PhotoView from '../../components/PhotoView.vue'

const form = reactive({ foodName: '', batchNo: '', storageLocation: '', storageTemp: 4, sampledAt: '', photos: '', remark: '' })
const list = ref([])
const saving = ref(false)

function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

async function load() { list.value = await api.get('/food-samples/mine') }

async function submit() {
  if (!store.fairDayId) return ElMessage.warning('请先选择赶集日')
  saving.value = true
  try {
    await api.post('/food-samples', { ...form, fairDay: { id: store.fairDayId } })
    ElMessage.success('留样已上传')
    Object.assign(form, { foodName: '', batchNo: '', storageLocation: '', storageTemp: 4, sampledAt: '', photos: '', remark: '' })
    load()
  } finally {
    saving.value = false
  }
}
watch(() => store.user, load, { immediate: true })
</script>
