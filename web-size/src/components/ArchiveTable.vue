<template>
  <a-card size="small" class="panel">
    <template #title>
      <span>{{ t('allLibraries') }} ({{ archives.length }})</span>
    </template>
    <a-input-search v-model="search" :placeholder="t('searchArchives')" allow-clear class="search-input" />

    <a-table
      :data="filtered" :pagination="false" :bordered="{ cell: true }" size="small"
      :stripe="true" row-key="key" :scroll="{ y: Math.min(filtered.length * 35, 480) }"
    >
      <template #columns>
        <a-table-column :title="t('library')" data-index="abbrev" :sortable="{ sortDirections: ['ascend','descend'] }" :width="180">
          <template #cell="{ record }">
            <a-link icon @click="$emit('drill', record)">{{ record.abbrev }}</a-link>
          </template>
        </a-table-column>

        <a-table-column :title="t('size')" :width="90" :sortable="{ sortDirections: ['ascend','descend'] }">
          <template #cell="{ record }">{{ fmtBytes(record.totalSize) }}</template>
        </a-table-column>

        <a-table-column :title="t('pct')" :width="75">
          <template #cell="{ record }">{{ fmtPct(record.totalSize, totalFirmwareSize) }}</template>
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
import { fmtBytes, fmtPct } from '../utils'
import { t } from '../i18n'
import type { ArchiveAggInfo } from '../useData'

const props = defineProps<{
  archives: ArchiveAggInfo[]
  memTypes: string[]
  totalFirmwareSize: number
}>()

defineEmits<{ drill: [a: ArchiveAggInfo] }>()

const search = ref('')

const filtered = computed(() => {
  const q = search.value.toLowerCase()
  const list = q
    ? props.archives.filter(a => a.abbrev.toLowerCase().includes(q) || a.key.toLowerCase().includes(q))
    : [...props.archives]
  return list.sort((a, b) => b.totalSize - a.totalSize)
})
</script>

<style scoped>
.panel { margin-bottom: 16px; box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.panel :deep(.arco-card-body) { padding: 12px; }
.dim { color: var(--color-text-4); }
.search-input { margin-bottom: 12px; max-width: 360px; }
</style>
