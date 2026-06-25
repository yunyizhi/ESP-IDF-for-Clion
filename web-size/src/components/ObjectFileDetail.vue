<template>
  <a-card size="small" class="panel">
    <template #title>
      <a-breadcrumb>
        <a-breadcrumb-item><a-link icon @click="$emit('back')">{{ t('allLibraries') }}</a-link></a-breadcrumb-item>
        <a-breadcrumb-item>{{ archive.abbrev }}</a-breadcrumb-item>
      </a-breadcrumb>
    </template>
    <template #extra>
      <span class="total-text">{{ t('total') }}: <b>{{ fmtBytes(archive.totalSize) }}</b></span>
    </template>

    <DrillCharts :items="chartItems" :dark="dark" />
    <a-input-search v-model="search" :placeholder="t('searchObjFiles')" allow-clear class="search-input" />

    <a-table
      :data="filtered" :pagination="false" :bordered="{ cell: true }" size="small" :stripe="true"
      row-key="key" :scroll="{ y: Math.min(filtered.length * 35, 400) }"
    >
      <template #columns>
        <a-table-column :title="t('sourceFile')" data-index="abbrev" :sortable="{ sortDirections: ['ascend','descend'] }" :width="180">
          <template #cell="{ record }">
            <a-link icon @click="$emit('drill', record)">{{ record.abbrev }}</a-link>
          </template>
        </a-table-column>

        <a-table-column :title="t('size')" :width="90" :sortable="{ sortDirections: ['ascend','descend'] }">
          <template #cell="{ record }">{{ fmtBytes(record.totalSize) }}</template>
        </a-table-column>

        <a-table-column :title="t('pct')" :width="75">
          <template #cell="{ record }">{{ fmtPct(record.totalSize, archive.totalSize) }}</template>
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
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { fmtBytes, fmtPct } from '@/utils'
import { t } from '@/i18n'
import type { ArchiveAggInfo, ObjFileAggInfo } from '@/useData'
import DrillCharts from './DrillCharts.vue'

const props = defineProps<{
  archive: ArchiveAggInfo
  memTypes: string[]
  objFiles: ObjFileAggInfo[]
  dark: boolean
}>()

defineEmits<{ back: []; drill: [obj: ObjFileAggInfo] }>()

const search = ref('')

const filtered = computed(() => {
  const q = search.value.toLowerCase()
  const list = q
    ? props.objFiles.filter(o => o.abbrev.toLowerCase().includes(q) || o.key.toLowerCase().includes(q))
    : [...props.objFiles]
  return list.sort((a, b) => b.totalSize - a.totalSize)
})

const chartItems = computed(() => props.objFiles.map(o => ({ name: o.abbrev, size: o.totalSize })))
</script>

<style scoped>
.panel { margin-bottom: 16px; box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.panel :deep(.arco-card-body) { padding: 12px; }
.total-text { font-size: 13px; color: var(--color-text-3); }
.dim { color: var(--color-text-4); }
.search-input { margin-bottom: 12px; max-width: 360px; }
</style>
