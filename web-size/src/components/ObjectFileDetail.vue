<template>
  <a-card size="small" class="panel">
    <template #title>
      <a-breadcrumb>
        <a-breadcrumb-item><a-link @click="$emit('back')">{{ t('allSections') }} / {{ section.memoryType }} / {{ section.abbrev }}</a-link></a-breadcrumb-item>
        <a-breadcrumb-item>{{ archive.abbrev }}</a-breadcrumb-item>
      </a-breadcrumb>
    </template>
    <template #extra>
      <span class="total-text">{{ t('total') }}: <b>{{ fmtBytes(archive.size) }}</b></span>
    </template>

    <DrillCharts :items="chartItems" :dark="dark" />
    <a-table :data="objFiles" :pagination="false" :bordered="{ cell: true }" size="small" :stripe="true"
      row-key="key" :scroll="{ y: Math.min(objFiles.length * 35, 400) }">
      <template #columns>
        <a-table-column :title="t('sourceFile')">
          <template #cell="{ record }"><a-link @click="$emit('drill', record)">{{ record.abbrev }}</a-link></template>
        </a-table-column>
        <a-table-column :title="t('size')" :width="90">
          <template #cell="{ record }">{{ fmtBytes(record.size) }}</template>
        </a-table-column>
        <a-table-column :title="t('pct')" :width="80">
          <template #cell="{ record }">{{ fmtPct(record.size, archive.size) }}</template>
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

const props = defineProps<{ section: SectionInfo; archive: ArchiveInfo; dark: boolean }>()
defineEmits<{ back: []; drill: [obj: ObjFileInfo] }>()

const objFiles = computed<ObjFileInfo[]>(() => {
  const obj = props.archive.object_files || {}
  return Object.keys(obj).map(k => {
    const syms = obj[k].symbols || {}
    const symbols = Object.keys(syms).map(sk => ({ key: sk, abbrev: syms[sk].abbrev_name || sk, size: syms[sk].size || 0 }))
    return { key: k, abbrev: obj[k].abbrev_name || k, size: obj[k].size || 0, symbols }
  }).sort((a, b) => b.size - a.size)
})
const chartItems = computed(() => objFiles.value.map(o => ({ name: o.abbrev, size: o.size })))
</script>

<style scoped>
.panel { margin-bottom: 16px; box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.total-text { font-size: 13px; color: var(--color-text-3); }
:deep(.arco-link) { text-decoration: underline; }
</style>
