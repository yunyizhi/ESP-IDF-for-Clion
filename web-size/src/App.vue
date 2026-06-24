<template>
  <a-config-provider :locale="arcoLocale">
    <header class="hd">
      <h2>📊 {{ t('title') }}</h2>
      <div class="hd-r">
        <a-tag v-if="data" :bordered="false" color="arcoblue">{{ t('target') }}: {{ data.target }}</a-tag>
        <a-tag v-if="data" :bordered="false">{{ t('totalFirmware') }}: {{ fmtBytes(data.image_size) }}</a-tag>
        <a-switch :model-value="i18n.lang==='zh'" @change="i18n.lang = $event ? 'zh' : 'en'">
          <template #checked>中</template>
          <template #unchecked>EN</template>
        </a-switch>
        <a-button size="mini" shape="circle" @click="toggleDark">{{ dark ? '☀' : '☾' }}</a-button>
      </div>
    </header>

    <main class="container">
      <a-spin v-if="!data && !error" :loading="true" class="loading" />
      <div v-else-if="error" class="error">{{ t('error') }}: {{ error }}<br /><small>{{ t('errorHint') }}</small></div>
      <template v-else>
        <!-- 顶层：图表 + 库统计表格 -->
        <SizeCharts v-if="!drillArchive" :sections="allSections()" :mem-types="data.memory_types" :dark="dark" />

        <ArchiveTable
          v-if="!drillArchive && !drillObj"
          :archives="allArchives()"
          :mem-types="memTypes"
          :total-firmware-size="data.image_size"
          @drill="drillArchive = $event"
        />

        <!-- 下钻第一级：库 → 目标文件列表 -->
        <ObjectFileDetail
          v-if="drillArchive && !drillObj"
          :archive="drillArchive"
          :mem-types="memTypes"
          :obj-files="objectFilesOf(drillArchive.key)"
          :dark="dark"
          @back="drillArchive = null"
          @drill="drillObj = $event"
        />

        <!-- 下钻第二级：目标文件 → 符号列表 -->
        <SymbolDetail
          v-if="drillArchive && drillObj"
          :archive="drillArchive"
          :obj-file="drillObj"
          :mem-types="memTypes"
          :symbols="symbolsOf(drillArchive.key, drillObj.key)"
          :dark="dark"
          @back="drillObj = null"
        />
      </template>
    </main>
  </a-config-provider>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import zhCN from '@arco-design/web-vue/es/locale/lang/zh-cn'
import enUS from '@arco-design/web-vue/es/locale/lang/en-us'
import { i18n, t } from './i18n'
import { fmtBytes } from './utils'
import { useData } from './useData'
import SizeCharts from './components/SizeCharts.vue'
import ArchiveTable from './components/ArchiveTable.vue'
import ObjectFileDetail from './components/ObjectFileDetail.vue'
import SymbolDetail from './components/SymbolDetail.vue'
import type { ArchiveAggInfo, ObjFileAggInfo } from './useData'

const props = defineProps<{ settings: { lang?: string; dark?: boolean } }>()

const { data, error, allSections, allArchives, objectFilesOf, symbolsOf, memoryTypeKeys } = useData()
const drillArchive = ref<ArchiveAggInfo | null>(null)
const drillObj = ref<ObjFileAggInfo | null>(null)
const dark = ref(false)
const arcoLocale = computed(() => i18n.lang === 'zh' ? zhCN : enUS)

const memTypes = computed(() => memoryTypeKeys())

function toggleDark() {
  dark.value = !dark.value
  document.body.setAttribute('arco-theme', dark.value ? 'dark' : '')
}

onMounted(() => {
  if (props.settings?.lang) i18n.lang = props.settings.lang as 'zh' | 'en'
  if (props.settings?.dark) {
    dark.value = true
    document.body.setAttribute('arco-theme', 'dark')
  }
})
</script>

<style>
body { background: var(--color-bg-1); color: var(--color-text-1); transition: background .2s; }
</style>

<style scoped>
.hd { background: var(--color-bg-2); border-bottom: 1px solid var(--color-border-2); padding: 10px 20px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.hd h2 { font-size: 17px; font-weight: 600; margin: 0; }
.hd-r { display: flex; align-items: center; gap: 8px; }
.container { max-width: 1300px; margin: 0 auto; padding: 16px 20px; }
.loading { display: flex; justify-content: center; padding: 80px 0; }
.error { text-align: center; padding: 60px; color: rgb(var(--danger-6)); }
</style>
