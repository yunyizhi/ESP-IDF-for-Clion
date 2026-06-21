<template>
  <a-card size="small" class="panel">
    <template #title>
      <a-breadcrumb>
        <a-breadcrumb-item><a-link @click="$emit('back')">{{ section.memoryType }} / {{ section.abbrev }} / {{ archive.abbrev }}</a-link></a-breadcrumb-item>
        <a-breadcrumb-item>{{ objFile.abbrev }}</a-breadcrumb-item>
      </a-breadcrumb>
    </template>
    <template #extra>
      <span class="total-text">{{ t('total') }}: <b>{{ fmtBytes(objFile.size) }}</b></span>
    </template>

    <DrillCharts :items="chartItems" :dark="dark" />
    <a-table :data="symbols" :pagination="false" :bordered="{ cell: true }" size="small" :stripe="true"
      row-key="key" :scroll="{ y: Math.min(symbols.length * 35, 400) }">
      <template #columns>
        <a-table-column :title="t('symbol')" data-index="abbrev" />
        <a-table-column :title="t('size')" :width="90">
          <template #cell="{ record }">{{ fmtBytes(record.size) }}</template>
        </a-table-column>
        <a-table-column :title="t('pct')" :width="80">
          <template #cell="{ record }">{{ fmtPct(record.size, objFile.size) }}</template>
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
import type { SectionInfo, ArchiveInfo, ObjFileInfo } from '../useData'
import DrillCharts from './DrillCharts.vue'

const props = defineProps<{ section: SectionInfo; archive: ArchiveInfo; objFile: ObjFileInfo; dark: boolean }>()
defineEmits<{ back: [] }>()

const symbols = computed(() => [...props.objFile.symbols].sort((a, b) => b.size - a.size))
const chartItems = computed(() => symbols.value.map(s => ({ name: s.abbrev, size: s.size })))
</script>

<style scoped>
.panel { margin-bottom: 16px; box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.total-text { font-size: 13px; color: var(--color-text-3); }
</style>
