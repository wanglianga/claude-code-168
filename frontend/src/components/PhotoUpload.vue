<template>
  <div>
    <el-upload v-if="urls.length < limit" list-type="picture-card" :http-request="doUpload"
               :before-upload="beforeUpload" accept="image/*" :show-file-list="false">
      <el-icon><Plus /></el-icon>
    </el-upload>
    <div class="thumbs">
      <div v-for="(u, i) in urls" :key="i" class="thumb-wrap">
        <el-image :src="u" :preview-src-list="urls" :initial-index="i" fit="cover" class="thumb"
                  preview-teleported />
        <el-icon class="del" @click="removeAt(i)"><CircleCloseFilled /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFiles } from '../api.js'

const props = defineProps({
  modelValue: { type: String, default: '' },
  limit: { type: Number, default: 6 }
})
const emit = defineEmits(['update:modelValue'])
const urls = computed(() => (props.modelValue || '').split(',').map((s) => s.trim()).filter(Boolean))

function beforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过 10MB')
    return false
  }
  return true
}

async function doUpload({ file }) {
  const saved = await uploadFiles([file])
  emit('update:modelValue', urls.value.concat(saved).join(','))
}

function removeAt(i) {
  const next = urls.value.slice()
  next.splice(i, 1)
  emit('update:modelValue', next.join(','))
}
</script>

<style scoped>
.thumbs { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 8px; }
.thumb-wrap { position: relative; width: 96px; height: 72px; }
.thumb { width: 96px; height: 72px; border-radius: 6px; border: 1px solid #e0e0e0; }
.del { position: absolute; top: -8px; right: -8px; color: #f56c6c; background: #fff;
  border-radius: 50%; font-size: 16px; cursor: pointer; }
</style>
