<template>
  <div class="charts-row">
    <a-card size="small" class="panel">
      <template #title><span>{{ t('sectionTree') }}</span></template>
      <v-chart ref="treeRef" :option="treeOption" autoresize style="height:320px" />
    </a-card>
    <a-card size="small" class="panel">
      <template #title><span>{{ t('memUsage') }}</span></template>
      <v-chart ref="barRef" :option="barOption" autoresize style="height:320px" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { use } from 'echarts/core'
import { TreemapChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { fmtBytes } from '../utils'
import { t, i18n } from '../i18n'

use([TreemapChart, BarChart, TitleComponent, TooltipComponent, GridComponent, CanvasRenderer])

const VIVID_COLORS = ['#5470c6','#fc8452','#73c0de','#3ba272','#ee6666','#9a60b4','#ea7ccc','#fac858','#91cc75','#f6c659']

const props = defineProps<{
  sections: { memoryType: string; abbrev: string; size: number }[]
  memTypes: Record<string, { used: number; size: number }>
  dark: boolean
}>()

const treeRef = ref<any>(null)
const barRef = ref<any>(null)

function textColor() {
  return getComputedStyle(document.body).getPropertyValue('--color-text-1').trim() || '#333'
}

const treeOption = computed(() => {
  void props.dark; void i18n.lang
  const tc = textColor()
  // Group sections by memory type for 2-level treemap
  const groups: Record<string, { name: string; abbr: string; size: number }[]> = {}
  for (const s of props.sections) {
    if (!groups[s.memoryType]) groups[s.memoryType] = []
    groups[s.memoryType].push({ name: s.abbrev + '\n' + fmtBytes(s.size), abbr: s.abbrev, size: s.size })
  }
  const memKeys = Object.keys(groups)
  const totalSize = props.sections.reduce((s, sec) => s + sec.size, 0)
  const rootLabel = 'Total App'
  const root = {
    name: rootLabel + '  ' + fmtBytes(totalSize), value: totalSize,
    children: memKeys.map((mt, i) => {
      const color = VIVID_COLORS[i % VIVID_COLORS.length]
      return {
        name: mt, value: groups[mt].reduce((s, c) => s + c.size, 0),
        itemStyle: { color },
        children: groups[mt].map(child => ({
          name: mt + ' ' + child.abbr, value: child.size, itemStyle: { color }
        }))
      }
    })
  }
  return {
    tooltip: { formatter: (p: any) => {
      const path = p.treePathInfo?.map((n: any) => n.name.split('  ')[0]).join(' > ') ?? p.name
      const pct = totalSize > 0 ? ((p.value ?? 0) / totalSize * 100).toFixed(1) + '%' : '0%'
      return `${path}<br/>${fmtBytes(p.value ?? 0)} (${pct})`
    }},
    series: [{
      type: 'treemap', data: [root], width: '100%', height: '100%',
      roam: false, nodeClick: false,
      breadcrumb: { show: false },
      levels: [
        {
          itemStyle: { borderWidth: 3, borderColor: '#333', gapWidth: 3 },
          upperLabel: { show: true, height: 28, fontSize: 13, fontWeight: 'bold', color: '#fff',
            formatter: (p: any) => p.name + '  ' + fmtBytes(p.value ?? 0)
          }
        },
        {
          colorSaturation: [0.35, 0.5],
          itemStyle: { borderWidth: 1, borderColor: '#1c1c1c', gapWidth: 1 },
          label: { fontSize: 12, color: '#fff',
            formatter: (p: any) => '{name|' + p.name + '}\n{val|' + fmtBytes(p.value ?? 0) + '}',
            rich: { name: { fontSize: 12, lineHeight: 16 }, val: { fontSize: 10, lineHeight: 14 } }
          }
        }
      ]
    }]
  }
})

const barOption = computed(() => {
  void props.dark; void i18n.lang
  const keys = Object.keys(props.memTypes)
  const tc = textColor()
  return {
    tooltip: { trigger: 'axis', formatter: (p: any) => {
      const d = p[0]; const info = props.memTypes[d.name]
      if (info && info.size > 0) {
        const pct = (info.used / info.size * 100).toFixed(1)
        return `${d.name}<br/>${t('used')}: ${fmtBytes(d.value)} / ${fmtBytes(info.size)} (${pct}%)`
      }
      return `${d.name}<br/>${t('used')}: ${fmtBytes(d.value)}`
    }},
    grid: { left: 80, right: 50, top: 10, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { color: tc, formatter: (v: number) => fmtBytes(v) } },
    yAxis: { type: 'category', data: keys, axisLabel: { color: tc } },
    series: [
      {
        name: t('total'), type: 'bar', barGap: '-100%', barMaxWidth: 24,
        data: keys.map((k, i) => ({
          value: props.memTypes[k]?.size || 0,
          itemStyle: { color: '#909399', borderRadius: [0, 4, 4, 0], opacity: 0.2 }
        })),
        tooltip: { show: false }
      },
      {
        name: t('used'), type: 'bar', barMaxWidth: 24,
        data: keys.map((k, i) => ({
          value: props.memTypes[k]?.used || 0,
          itemStyle: { color: VIVID_COLORS[i % VIVID_COLORS.length], borderRadius: [0, 4, 4, 0] }
        })),
        label: { show: true, position: 'right', color: tc, formatter: (p: any) => {
          const info = props.memTypes[p.name]
          const sz = info?.size || 0
          if (sz > 0) return (info!.used / sz * 100).toFixed(1) + '%'
          return fmtBytes(p.value)
        }}
      }
    ]
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
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.panel { box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.panel :deep(.arco-card-body) { padding: 12px; }
@media (max-width: 768px) { .charts-row { grid-template-columns: 1fr; } }
</style>
