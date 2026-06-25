import { reactive } from 'vue'

export const i18n = reactive({
  lang: 'zh' as 'zh' | 'en',
})

const messages: Record<string, Record<string, string>> = {
  zh: {
    title: 'ESP-IDF 固件大小分析', target: '目标', totalFirmware: '总大小',
    error: '加载失败', errorHint: '请先运行 "Size分析" 任务',
    // 图表
    sectionTree: '占用分布', memUsage: '区域使用量',
    distTree: '分布', top10: 'Top 10',
    // 表格通用
    size: '大小', pct: '占比', total: '合计',
    used: '已用', back: '返回',
    // 库统计
    allLibraries: '库用量统计', searchArchives: '搜索库名称...',
    library: '库', sourceFile: '源文件',
    searchObjFiles: '搜索文件名...',
    // 符号
    symbol: '符号',
    searchSymbols: '搜索符号...',
    distCol: '区域/段分布',
  },
  en: {
    title: 'ESP-IDF Size Analysis', target: 'Target', totalFirmware: 'Total Size',
    error: 'Load Error', errorHint: 'Please run "Size Analysis" task first',
    // Charts
    sectionTree: 'Occupancy', memUsage: 'Region Usage',
    distTree: 'Distribution', top10: 'Top 10',
    // Table common
    size: 'Size', pct: 'Pct', total: 'Total',
    used: 'Used', back: 'Back',
    // Library stats
    allLibraries: 'Library Usage', searchArchives: 'Search library...',
    library: 'Library', sourceFile: 'Source File',
    searchObjFiles: 'Search file...',
    // Symbols
    symbol: 'Symbol',
    searchSymbols: 'Search symbol...',
    distCol: 'Region/Section',
  },
}

export function t(key: string): string {
  return messages[i18n.lang]?.[key] ?? key
}
