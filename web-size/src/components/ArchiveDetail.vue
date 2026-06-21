<template>
  <a-card size="small" class="panel">
    <template #title>
      <a-breadcrumb>
        <a-breadcrumb-item><a-link @click="$emit('back')">{{ t('allSections') }}</a-link></a-breadcrumb-item>
        <a-breadcrumb-item>{{ section.memoryType }} / {{ section.abbrev }}</a-breadcrumb-item>
      </a-breadcrumb>
    </template>
    <template #extra>
      <span class="total-text">{{ t('total') }}: <b>{{ fmtBytes(section.size) }}</b></span>
    </template>

    <DrillCharts :items="chartItems" :dark="dark" />
    <a-input-search v-model="search" :placeholder="t('searchLibs')" allow-clear class="search-input" />
    <a-table :data="filteredArchives" :pagination="false" :bordered="{ cell: true }" size="small" :stripe="true"
      row-key="key" :scroll="{ y: Math.min(filteredArchives.length * 35, 400) }">
      <template #columns>
        <a-table-column :title="t('library')">
          <template #cell="{ record }"><a-link @click="$emit('drill', record)">{{ record.abbrev }}</a-link></template>
        </a-table-column>
        <a-table-column :title="t('size')" :width="90">
          <template #cell="{ record }">{{ fmtBytes(record.size) }}</template>
        </a-table-column>
        <a-table-column :title="t('pct')" :width="80">
          <template #cell="{ record }">{{ fmtPct(record.size, section.size) }}</template>
        </a-table-column>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { fmtBytes, fmtPct } from '../utils'
import { t } from '../i18n'
import type { SectionInfo, ArchiveInfo } from '../useData'
import DrillCharts from './DrillCharts.vue'

const props = defineProps<{ section: SectionInfo; archives: ArchiveInfo[]; dark: boolean }>()
defineEmits<{ back: []; drill: [a: ArchiveInfo] }>()

const search = ref('')

const filteredArchives = computed(() => {
  const q = search.value.toLowerCase()
  const list = q ? props.archives.filter(a => a.abbrev.toLowerCase().includes(q) || a.key.toLowerCase().includes(q)) : [...props.archives]
  return list.sort((a, b) => b.size - a.size)
})
const chartItems = computed(() => props.archives.map(a => ({ name: a.abbrev, size: a.size })))
</script>

<style scoped>
.panel { margin-bottom: 16px; box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.total-text { font-size: 13px; color: var(--color-text-3); }
:deep(.arco-link) { text-decoration: underline; }
</style>
