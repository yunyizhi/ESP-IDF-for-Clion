<template>
  <a-card size="small" class="panel">
    <template #title>
      <a-breadcrumb>
        <a-breadcrumb-item><a-link icon @click="$emit('back')">{{ archive.abbrev }}</a-link></a-breadcrumb-item>
        <a-breadcrumb-item>{{ objFile.abbrev }}</a-breadcrumb-item>
      </a-breadcrumb>
    </template>
    <template #extra>
      <span class="total-text">{{ t('total') }}: <b>{{ fmtBytes(objFile.totalSize) }}</b></span>
    </template>

    <DrillCharts :items="chartItems" :dark="dark" />
    <a-input-search v-model="search" :placeholder="t('searchSymbols')" allow-clear class="search-input" />

    <a-table
      :data="filtered" :pagination="false" :bordered="{ cell: true }" size="small" :stripe="true"
      row-key="key" :scroll="{ y: Math.min(filtered.length * 35, 400) }"
    >
      <template #columns>
        <a-table-column :title="t('symbol')" data-index="abbrev" :sortable="{ sortDirections: ['ascend','descend'] }" :width="180" />

        <a-table-column :title="t('size')" :width="90" :sortable="{ sortDirections: ['ascend','descend'] }">
          <template #cell="{ record }">{{ fmtBytes(record.totalSize) }}</template>
        </a-table-column>

        <a-table-column :title="t('pct')" :width="75">
          <template #cell="{ record }">{{ fmtPct(record.totalSize, objFile.totalSize) }}</template>
        </a-table-column>

        <!-- 区域/段分布单列，跨区域在同一个单元格内展示 -->
        <a-table-column :title="t('distCol')" :width="280">
          <template #cell="{ record }">
            <div class="dist-cell">
              <div v-for="de in distEntries(record)" :key="de.mt + '/' + de.sn" class="dist-line">
                <a-tag size="small" :color="de.color">{{ de.mt }}</a-tag>
                <span class="sec-name">{{ de.sn }}</span>
                <span class="sec-val">{{ fmtBytes(de.size) }}</span>
              </div>
            </div>
          </template>
        </a-table-column>
      </template>
    </a-table>

    <a-button size="small" type="outline" @click="$emit('back')" style="margin-top:12px">{{ t('back') }}</a-button>
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { fmtBytes, fmtPct, CHART_COLORS } from '../utils'
import { t } from '../i18n'
import type { ArchiveAggInfo, ObjFileAggInfo, SymbolAggInfo } from '../useData'
import DrillCharts from './DrillCharts.vue'

const props = defineProps<{
  archive: ArchiveAggInfo
  objFile: ObjFileAggInfo
  memTypes: string[]
  symbols: SymbolAggInfo[]
  dark: boolean
}>()

defineEmits<{ back: [] }>()

const search = ref('')

const filtered = computed(() => {
  const q = search.value.toLowerCase()
  const list = q
    ? props.symbols.filter(s => s.abbrev.toLowerCase().includes(q) || s.key.toLowerCase().includes(q))
    : [...props.symbols]
  return list.sort((a, b) => b.totalSize - a.totalSize)
})

const chartItems = computed(() => props.symbols.map(s => ({ name: s.abbrev, size: s.totalSize })))

/** 展开某符号在所有 (mt, section) 下的分布，按大小降序 */
function distEntries(record: SymbolAggInfo) {
  const mtSec = record.mtSections || {}
  const entries: { mt: string; sn: string; size: number; color: string }[] = []
  for (const [mt, secMap] of Object.entries(mtSec)) {
    const idx = props.memTypes.indexOf(mt)
    const color = CHART_COLORS[idx % CHART_COLORS.length]
    for (const [sn, size] of Object.entries(secMap)) {
      entries.push({ mt, sn, size, color })
    }
  }
  return entries.sort((a, b) => b.size - a.size)
}
</script>

<style scoped>
.panel { margin-bottom: 16px; box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.panel :deep(.arco-card-body) { padding: 12px; }
.total-text { font-size: 13px; color: var(--color-text-3); }
.search-input { margin-bottom: 12px; max-width: 360px; }
.dist-cell { display: flex; flex-direction: column; gap: 4px; }
.dist-line { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.sec-name { color: var(--color-text-2); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.sec-val { color: var(--color-text-1); font-weight: 600; white-space: nowrap; }
</style>
