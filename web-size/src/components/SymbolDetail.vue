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

    <a-table
      :data="symbols" :pagination="false" :bordered="{ cell: true }" size="small" :stripe="true"
      row-key="key" :scroll="{ y: Math.min(symbols.length * 35, 400) }"
    >
      <template #columns>
        <a-table-column :title="t('symbol')" data-index="abbrev" :sortable="{ sortDirections: ['ascend','descend'] }" :width="180" />

        <a-table-column :title="t('size')" :width="90" :sortable="{ sortDirections: ['ascend','descend'] }">
          <template #cell="{ record }">{{ fmtBytes(record.totalSize) }}</template>
        </a-table-column>

        <a-table-column :title="t('pct')" :width="75">
          <template #cell="{ record }">{{ fmtPct(record.totalSize, objFile.totalSize) }}</template>
        </a-table-column>

        <!-- 每个内存区域一列 -->
        <a-table-column
          v-for="mt in memTypes" :key="mt" :title="mt" :width="95"
          :sortable="{ sortDirections: ['ascend','descend'] }"
        >
          <template #cell="{ record }">
            <span v-if="record.memSizes[mt]">{{ fmtBytes(record.memSizes[mt]) }}</span>
            <span v-else class="dim">—</span>
          </template>
        </a-table-column>
      </template>
    </a-table>

    <a-button size="small" type="outline" @click="$emit('back')" style="margin-top:12px">{{ t('back') }}</a-button>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { fmtBytes, fmtPct } from '../utils'
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

const chartItems = computed(() => props.symbols.map(s => ({ name: s.abbrev, size: s.totalSize })))
</script>

<style scoped>
.panel { margin-bottom: 16px; box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.panel :deep(.arco-card-body) { padding: 12px; }
.total-text { font-size: 13px; color: var(--color-text-3); }
.dim { color: var(--color-text-4); }
</style>
