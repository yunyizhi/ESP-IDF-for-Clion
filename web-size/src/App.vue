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
        <SizeCharts v-if="!drillTarget" :sections="allSections()" :mem-types="data.memory_types" :dark="dark" />
        <SectionsTable v-if="!drillTarget" :sections="allSections()" @drill="drillTarget=$event" />
        <ArchiveDetail v-if="drillTarget && !drillArchive && !drillObj" :section="drillTarget" :archives="archivesOf(drillTarget)" :dark="dark" @back="drillTarget=null" @drill="drillArchive=$event" />
        <ObjectFileDetail v-if="drillTarget && drillArchive && !drillObj" :section="drillTarget" :archive="drillArchive" :dark="dark" @back="drillArchive=null" @drill="drillObj=$event" />
        <SymbolDetail v-if="drillTarget && drillArchive && drillObj" :section="drillTarget" :archive="drillArchive" :obj-file="drillObj" :dark="dark" @back="drillObj=null" />
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
import SectionsTable from './components/SectionsTable.vue'
import ArchiveDetail from './components/ArchiveDetail.vue'
import ObjectFileDetail from './components/ObjectFileDetail.vue'
import SymbolDetail from './components/SymbolDetail.vue'
import type { SectionInfo, ArchiveInfo, ObjFileInfo } from './useData'

const props = defineProps<{ settings: { lang?: string; dark?: boolean } }>()

const { data, error, allSections, archivesOf } = useData()
const drillTarget = ref<SectionInfo | null>(null)
const drillArchive = ref<ArchiveInfo | null>(null)
const drillObj = ref<ObjFileInfo | null>(null)
const dark = ref(false)
const arcoLocale = computed(() => i18n.lang === 'zh' ? zhCN : enUS)

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
.panel { margin-bottom: 16px; box-shadow: none; background: var(--color-bg-2); border-color: var(--color-border-2); }
.panel :deep(.arco-card-body) { padding: 12px; }
.total-text { font-size: 13px; color: var(--color-text-3); }
</style>
