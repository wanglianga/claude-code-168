<template>
  <el-image v-for="(u, i) in urls" :key="i" :src="u" :preview-src-list="urls"
            :initial-index="i" fit="cover" class="photo" preview-teleported />
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ urls: { type: String, default: '' } })
const urls = computed(() => (props.urls || '').split(',').map((s) => s.trim()).filter(Boolean))
</script>

<style scoped>
.photo { width: 88px; height: 66px; border-radius: 6px; border: 1px solid #e2e2e2; margin: 0 8px 8px 0; cursor: zoom-in; }
</style>
