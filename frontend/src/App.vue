<template>
  <router-view />
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif; background: #f5f7fa; }
#app { min-height: 100vh; }
.page { padding: 20px; }
.card-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.mt { margin-top: 16px; }
.mb { margin-bottom: 16px; }
.text-muted { color: #909399; font-size: 13px; }
.img-thumb { width: 80px; height: 80px; object-fit: cover; border-radius: 4px; margin-right: 6px; cursor: pointer; }
</style>
