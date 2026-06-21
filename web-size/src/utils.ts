export function fmtBytes(bytes: number): string {
  if (!bytes || bytes === 0) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(2) + ' MB'
}

export function fmtPct(part: number, total: number): string {
  return total > 0 ? ((part / total) * 100).toFixed(1) + '%' : '0%'
}

export function usagePct(used: number, size: number): number {
  return size > 0 ? (used / size) * 100 : 0
}

export function progressColor(pct: number): string {
  if (pct > 90) return '#f38ba8'
  if (pct > 70) return '#bd9741'
  return '#3bad31'
}

export const CHART_COLORS = [
  '#5470c6', '#945136', '#498ba5', '#3ba272', '#ee6666',
  '#9a60b4', '#c32395', '#997624', '#4fa426', '#e0a725',
]

export function memColorIdx(memKeys: string[], name: string): number {
  const i = memKeys.indexOf(name)
  return i >= 0 ? i % CHART_COLORS.length : 0
}
