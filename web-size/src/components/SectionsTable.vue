<template>
  <a-card size="small" class="panel">
    <template #title>
      <span>{{ t('allSections') }} ({{ sections.length }})</span>
    </template>
    <a-input-search v-model="search" :placeholder="t('searchPlaceholder')" allow-clear class="search-input"/>
    <a-table
        :data="filtered" :pagination="false" :bordered="{ cell: true }" size="small"
        :stripe="true" row-key="sectionName" :scroll="{ y: Math.min(filtered.length * 35, 480) }"
    >
      <template #columns>
        <a-table-column :title="t('memRegion')" :width="150" :sortable="{ sortDirections: ['ascend','descend'] }">
          <template #cell="{ record }">
            <a-tag :color="memColor(record.memoryType)" size="small">{{ record.memoryType }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column :title="t('section')" data-index="abbrev" :sortable="{ sortDirections: ['ascend','descend'] }">
          <template #cell="{ record }">
            <a-tooltip v-if="record.topArchives.length" position="right" mini>
              <template #content>
                <div style="min-width:260px">
                  <b>{{ record.abbrev }}</b> &mdash; {{ t('topLibs') }}
                  <a-table :data="record.topArchives" :pagination="false" size="small" style="margin-top:6px">
                    <a-table-column title="Library" data-index="abbrev"/>
                    <a-table-column title="Size" :width="70">
                      <template #cell="{ record: r }">{{ fmtBytes(r.size) }}</template>
                    </a-table-column>
                    <a-table-column title="%" :width="55">
                      <template #cell="{ record: r }">{{ fmtPct(r.size, record.size) }}</template>
                    </a-table-column>
                  </a-table>
                </div>
              </template>
              <a-link @click="$emit('drill', record)">{{ record.abbrev }}</a-link>
            </a-tooltip>
            <a-link v-else @click="$emit('drill', record)">{{ record.abbrev }}</a-link>
          </template>
        </a-table-column>
        <a-table-column :title="t('size')" :width="90" :sortable="{ sortDirections: ['ascend','descend'] }">
          <template #cell="{ record }">{{ fmtBytes(record.size) }}</template>
        </a-table-column>
        <a-table-column :title="t('pct')" :width="80">
          <template #cell="{ record }">{{ fmtPct(record.size, totalSize) }}</template>
        </a-table-column>
        <a-table-column :title="t('libs')" :width="70">
          <template #cell="{ record }">{{ record.archiveCount }}</template>
        </a-table-column>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import {ref, computed} from 'vue'
import {fmtBytes, fmtPct, CHART_COLORS, memColorIdx} from '../utils'
import {t} from '../i18n'
import type {SectionInfo} from '../useData'

const props = defineProps<{ sections: SectionInfo[] }>()
defineEmits<{ drill: [sec: SectionInfo] }>()

const search = ref('')
const totalSize = computed(() => props.sections.reduce((s, s2) => s + s2.size, 0))
const memKeys = computed(() => props.sections.map(s => s.memoryType))
const memColor = (name: string) => CHART_COLORS[memColorIdx(memKeys.value, name) % CHART_COLORS.length]


const filtered = computed(() => {
  const q = search.value.toLowerCase()
  const list = q ? props.sections.filter(s => s.abbrev.toLowerCase().includes(q) || s.memoryType.toLowerCase().includes(q)) : [...props.sections]
  return list.sort((a, b) => b.size - a.size)
})
</script>

<style scoped>
.panel { margin-bottom: 16px; box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.panel :deep(.arco-card-body) { padding: 12px; }
:deep(.arco-link) { text-decoration: underline; }
</style>
