import { reactive } from 'vue'

export const i18n = reactive({
  lang: 'zh' as 'zh' | 'en',
})

const messages: Record<string, Record<string, string>> = {
  zh: {
    title: 'ESP-IDF 内存分析', target: '目标', totalFirmware: '总固件',
    loading: '加载中...', error: '加载失败', errorHint: '请先运行 "Size分析" 任务',
    allSections: '内存段总览', memRegion: '内存区域', section: 'Section',
    size: '大小', pct: '占比', libs: '库数', dist: '分布',
    searchPlaceholder: '搜索段名称...', searchLibs: '搜索库名称...',
    noMatch: '无匹配结果', total: '合计', library: 'Library库', sourceFile: '源文件',
    topLibs: 'Top库', back: '返回', usage: '使用率',
    sectionTree: '内存分布', memUsage: '内存块用量', used: '已用',
    distTree: '分布', top10: 'Top 10', symbol: '符号',
    // 新增翻译键
    allLibraries: '库用量统计', searchArchives: '搜索库名称...',
    objFiles: '目标文件', searchObjFiles: '搜索文件名...',
    symbolDetail: '符号详情',
  },
  en: {
    title: 'ESP-IDF Size Analysis', target: 'Target', totalFirmware: 'Total Firmware',
    loading: 'Loading...', error: 'Load Error', errorHint: 'Please run "Size Analysis" task first',
    allSections: 'All Sections', memRegion: 'Region', section: 'Section',
    size: 'Size', pct: 'Pct', libs: 'Libs', dist: 'Dist',
    searchPlaceholder: 'Search section...', searchLibs: 'Search library...',
    noMatch: 'No match', total: 'Total', library: 'Library', sourceFile: 'Source File',
    topLibs: 'Top Libs', back: 'Back', usage: 'Usage',
    sectionTree: 'Memory Layout', memUsage: 'Memory Usage', used: 'Used',
    distTree: 'Distribution', top10: 'Top 10', symbol: 'Symbol',
    // New keys
    allLibraries: 'Library Usage', searchArchives: 'Search library...',
    objFiles: 'Object Files', searchObjFiles: 'Search file...',
    symbolDetail: 'Symbols',
  },
}

export function t(key: string): string {
  return messages[i18n.lang]?.[key] ?? key
}
