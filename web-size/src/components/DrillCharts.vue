<template>
  <div class="charts-row">
    <a-card size="small" class="panel">
      <template #title><span>{{ t('distTree') }}</span></template>
      <v-chart ref="treeRef" :option="treeOption" autoresize style="height:280px" />
    </a-card>
    <a-card size="small" class="panel">
      <template #title><span>{{ t('top10') }}</span></template>
      <v-chart ref="barRef" :option="barOption" autoresize style="height:280px" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { use } from 'echarts/core'
import { TreemapChart, BarChart } from 'echarts/charts'
import { TooltipComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { fmtBytes } from '../utils'
import { t, i18n } from '../i18n'

use([TreemapChart, BarChart, TooltipComponent, GridComponent, CanvasRenderer])

const VIVID_COLORS = ['#5470c6','#fc8452','#73c0de','#3ba272','#ee6666','#9a60b4','#ea7ccc','#fac858','#91cc75','#f6c659']

const props = defineProps<{ items: { name: string; size: number }[]; dark: boolean }>()

const treeRef = ref<any>(null)
const barRef = ref<any>(null)

function textColor() {
  return getComputedStyle(document.body).getPropertyValue('--color-text-1').trim() || '#333'
}

const treeOption = computed(() => {
  void props.dark; void i18n.lang
  const totalSize = props.items.reduce((s, x) => s + x.size, 0)
  const data = props.items.map((x, i) => ({
    name: x.name, value: x.size, itemStyle: { color: VIVID_COLORS[i % VIVID_COLORS.length] }
  }))
  return {
    tooltip: { formatter: (p: any) => {
      const pct = totalSize > 0 ? ((p.value ?? 0) / totalSize * 100).toFixed(1) + '%' : ''
      return `${p.name}<br/>${fmtBytes(p.value ?? 0)} (${pct})`
    }},
    series: [{
      type: 'treemap', data, width: '100%', height: '100%',
      roam: false, nodeClick: false, breadcrumb: { show: false },
      label: { show: true, color: '#fff', fontSize: 11,
        formatter: (p: any) => p.name + (p.value ? '\n' + fmtBytes(p.value) : '')
      },
      itemStyle: { borderWidth: 1, borderColor: '#222', gapWidth: 1 }
    }]
  }
})

const barOption = computed(() => {
  void props.dark; void i18n.lang
  const top10 = [...props.items].sort((a, b) => b.size - a.size).slice(0, 10)
  const tc = textColor()
  return {
    tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].name}: ${fmtBytes(p[0].value)}` },
    grid: { left: 140, right: 10, top: 6, bottom: 20 },
    xAxis: { type: 'value', axisLabel: { color: tc, formatter: (v: number) => fmtBytes(v) } },
    yAxis: { type: 'category', data: top10.map(x => x.name), axisLabel: { color: tc, fontSize: 10 }, inverse: true },
    series: [{
      type: 'bar', data: top10.map((x, i) => ({
        value: x.size, itemStyle: { color: VIVID_COLORS[i % VIVID_COLORS.length], borderRadius: [0, 3, 3, 0] }
      })), barMaxWidth: 18
    }]
  }
})

watch(() => [props.dark, i18n.lang], () => {
  setTimeout(() => {
    treeRef.value?.chart?.setOption(treeOption.value, true)
    barRef.value?.chart?.setOption(barOption.value, true)
  }, 50)
})
</script>

<style scoped>
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px; }
.panel { box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.panel :deep(.arco-card-body) { padding: 12px; }
@media (max-width: 768px) { .charts-row { grid-template-columns: 1fr; } }
</style>
