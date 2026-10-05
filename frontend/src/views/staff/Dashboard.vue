<template>
  <div>
    <h2 class="page-title">集市复盘看板</h2>
    <p class="page-desc">乡镇管理者复盘：高投诉品类、违规/拥堵高发区域、收费到账、食品安全风险。数据来源：{{ store.currentFairDay?.name || '请选择集市' }}</p>

    <el-skeleton v-if="!d" :rows="8" animated />
    <template v-else>
      <el-row :gutter="14">
        <el-col :span="6"><el-card class="stat"><div class="num">{{ d.signup.total }}</div><div class="lbl">报名（通过 {{ d.signup.approved }} / 驳回 {{ d.signup.rejected }} / 待审 {{ d.signup.pending }}）</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat"><div class="num">{{ d.draw.allocated }}</div><div class="lbl">中签摊位（候补 {{ d.draw.waitlist }}，未到场 {{ d.draw.noShow }}）</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat"><div class="num" style="color:#2f6b47">{{ d.fee.paidRate }}%</div><div class="lbl">收费到账率</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat"><div class="num" :class="riskClass">{{ d.foodRisk.riskLevel }}风险</div><div class="lbl">食品安全风险等级</div></el-card></el-col>
      </el-row>

      <el-row :gutter="14" style="margin-top:14px">
        <el-col :span="12">
          <el-card>
            <template #header><b>收费到账（元）</b></template>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="应收总额">{{ d.fee.receivable }}</el-descriptions-item>
              <el-descriptions-item label="已到账"><b style="color:#2f6b47">{{ d.fee.paid }}</b></el-descriptions-item>
              <el-descriptions-item label="费用减免">{{ d.fee.waived }}</el-descriptions-item>
              <el-descriptions-item label="待收"><b style="color:#c62828">{{ d.fee.outstanding }}</b></el-descriptions-item>
              <el-descriptions-item label="保证金扣罚"><span style="color:#c62828">{{ d.fee.depositDeducted }}</span></el-descriptions-item>
              <el-descriptions-item label="保证金已退">{{ d.fee.depositRefunded }}</el-descriptions-item>
            </el-descriptions>
            <div class="bar-row" style="margin-top:14px">
              <div class="bar green" :style="{ width: paidPct + '%' }">已收 {{ paidPct }}%</div>
              <div class="bar orange" :style="{ width: waivedPct + '%' }">减 {{ waivedPct }}%</div>
              <div class="bar red" :style="{ width: outPct + '%' }">欠 {{ outPct }}%</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header><b>食品安全风险</b></template>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="熟食/凉菜摊位数">{{ d.foodRisk.cookedStalls }}</el-descriptions-item>
              <el-descriptions-item label="已上传留样">{{ d.foodRisk.sampleCount }} 份</el-descriptions-item>
              <el-descriptions-item label="留样覆盖率">{{ d.foodRisk.sampleCoverage }}%</el-descriptions-item>
              <el-descriptions-item label="腹泻疑似食物中毒投诉">
                <b :style="{color: d.foodRisk.diarrheaComplaints ? '#c62828' : ''}">{{ d.foodRisk.diarrheaComplaints }} 起</b>
              </el-descriptions-item>
              <el-descriptions-item label="低卫生巡查">{{ d.foodRisk.lowHygieneInspections }} 次</el-descriptions-item>
            </el-descriptions>
            <el-alert v-if="d.foodRisk.diarrheaComplaints" type="error" :closable="false" show-icon style="margin-top:12px"
                      title="存在腹泻类投诉，应立即核查对应批次留样并溯源" />
            <el-alert v-else-if="d.foodRisk.sampleCoverage < 100" type="warning" :closable="false" show-icon style="margin-top:12px"
                      title="熟食摊位留样未全覆盖，散场前须补齐留样台账" />
            <el-alert v-else type="success" :closable="false" show-icon style="margin-top:12px" title="无腹泻投诉且留样全覆盖" />
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" style="margin-top:14px">
        <el-col :span="12">
          <el-card>
            <template #header>
              <b>高投诉品类</b>
              <el-tag class="hdr-tag">{{ d.complaintTotal }} 起投诉</el-tag>
            </template>
            <div v-for="c in d.complaintByCategory" :key="c.name" class="hbar">
              <span class="hlbl">{{ c.name }}</span>
              <span class="htrack"><i :style="{ width: barW(c.count, d.complaintTotal) + '%' }"></i></span>
              <b>{{ c.count }}</b>
            </div>
            <el-empty v-if="!d.complaintByCategory.length" description="本场暂无投诉" :image-size="60" />
            <el-divider content-position="left">按投诉类型</el-divider>
            <div v-for="c in d.complaintByType" :key="c.name" class="hbar">
              <span class="hlbl">{{ c.name }}</span>
              <span class="htrack"><i class="orange-bar" :style="{ width: barW(c.count, d.complaintTotal) + '%' }"></i></span>
              <b>{{ c.count }}</b>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header><b>违规/拥堵高发区域</b></template>
            <el-table :data="d.zones" size="small" stripe>
              <el-table-column prop="zone" label="区域" min-width="130" />
              <el-table-column prop="inspections" label="巡查次数" width="80" />
              <el-table-column prop="problemInspections" label="问题巡查" width="80">
                <template #default="{ row }">
                  <b :style="{ color: row.problemInspections ? '#c62828' : '' }">{{ row.problemInspections }}</b>
                </template>
              </el-table-column>
              <el-table-column label="平均评分(25)" width="110">
                <template #default="{ row }">
                  <span :style="{ color: row.avgScore >= 22 ? '#2f6b47' : row.avgScore >= 18 ? '#d97706' : '#c62828' }">
                    {{ row.avgScore }} / 25
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="involvedStalls" label="涉及摊位数" width="90" />
            </el-table>
            <el-empty v-if="!d.zones.length" description="本场暂无巡查数据" :image-size="60" />
            <div class="card-msg" style="margin-top:8px">提示：问题巡查集中的区域为下一场集市消防通道、摊位间距调整的重点。</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="14" style="margin-top:14px">
        <el-col :span="24">
          <el-card>
            <template #header><b>巡查发现问题分布</b></template>
            <el-row :gutter="10">
              <el-col :span="6" v-for="i in d.issues" :key="i.name">
                <div class="hbar">
                  <span class="hlbl wide">{{ i.name }}</span>
                  <span class="htrack"><i class="red-bar" :style="{ width: barW(i.count, issueMax) + '%' }"></i></span>
                  <b>{{ i.count }}</b>
                </div>
              </el-col>
            </el-row>
            <el-empty v-if="!d.issues.length" description="巡查未发现问题" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import api from '../../api.js'
import { store } from '../../store.js'

const d = ref(null)
const paidPct = computed(() => d.value && d.value.fee.receivable ? pct(d.value.fee.paid, d.value.fee.receivable) : 0)
const waivedPct = computed(() => d.value && d.value.fee.receivable ? pct(d.value.fee.waived, d.value.fee.receivable) : 0)
const outPct = computed(() => d.value && d.value.fee.receivable ? Math.max(0, 100 - paidPct.value - waivedPct.value) : 0)
const issueMax = computed(() => d.value ? Math.max(1, ...d.value.issues.map((i) => i.count)) : 1)
const riskClass = computed(() => ({ 高: 'risk-high', 中: 'risk-mid', 低: 'risk-low' }[d.value?.foodRisk?.riskLevel]))

function pct(a, b) { return Math.round((Number(a) / Number(b)) * 1000) / 10 }
function barW(count, max) { return Math.max(4, Math.round((count / Math.max(1, max)) * 100)) }

async function load() {
  d.value = null
  if (store.fairDayId) d.value = await api.get(`/stats/overview/${store.fairDayId}`)
}
watch(() => store.fairDayId, load, { immediate: true })
</script>

<style scoped>
.stat { text-align: center; }
.num { font-size: 30px; font-weight: 800; color: #1f3a2e; }
.lbl { color: #888; font-size: 12.5px; margin-top: 4px; }
.risk-high { color: #c62828; }
.risk-mid { color: #d97706; }
.risk-low { color: #2f6b47; }
.hdr-tag { margin-left: 10px; }
.bar-row { display: flex; width: 100%; height: 22px; border-radius: 4px; overflow: hidden; background: #f2f2f2; font-size: 12px; color: #fff; font-weight: 600; }
.bar { display: flex; align-items: center; justify-content: center; }
.bar.green { background: #2f6b47; }
.bar.orange { background: #d97706; }
.bar.red { background: #c62828; }
.hbar { display: flex; align-items: center; gap: 10px; margin: 10px 0; }
.hlbl { width: 150px; font-size: 13px; color: #444; text-align: right; flex-shrink: 0; }
.hlbl.wide { width: 180px; }
.htrack { flex: 1; background: #f0f1f3; border-radius: 4px; height: 14px; display: flex; }
.htrack i { display: block; height: 100%; border-radius: 4px; background: #2f6b47; }
.htrack i.orange-bar { background: #d97706; }
.htrack i.red-bar { background: #c62828; }
.hbar b { width: 28px; text-align: left; color: #333; }
</style>
