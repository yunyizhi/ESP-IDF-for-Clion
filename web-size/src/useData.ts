import { ref } from 'vue'

export interface SectionInfo {
  memoryType: string
  sectionName: string
  abbrev: string
  size: number
  archiveCount: number
  topArchives: { key: string; abbrev: string; size: number }[]
  fullArchives: Record<string, { abbrev_name: string; size: number; object_files: Record<string, { abbrev_name: string; size: number }> }>
}

export interface SymbolItem {
  key: string
  abbrev: string
  size: number
}

export interface ObjFileInfo {
  key: string
  abbrev: string
  size: number
  symbols: SymbolItem[]
}

export interface ArchiveInfo {
  key: string
  abbrev: string
  size: number
  object_files: Record<string, { abbrev_name: string; size: number; symbols?: Record<string, { abbrev_name: string; size: number }> }>
}

export function useData() {
  const data = ref<any>(null)
  const error = ref('')

  const params = new URLSearchParams(window.location.search)
  const dataPath = params.get('path') || ''
  const apiUrl = '/esp-idf-size-analysis/api/data' + (dataPath ? '?path=' + encodeURIComponent(dataPath) : '')

  fetch(apiUrl)
    .then(r => (r.ok ? r.json() : Promise.reject('HTTP ' + r.status)))
    .then(json => {
      if (json.error) throw new Error(json.error)
      data.value = json
    })
    .catch(e => { error.value = e.message })

  function allSections(): SectionInfo[] {
    if (!data.value) return []
    const res: SectionInfo[] = []
    Object.entries(data.value.memory_types).forEach(([mt, info]: [string, any]) => {
      Object.entries(info.sections || {}).forEach(([sn, sec]: [string, any]) => {
        const archives = sec.archives || {}
        const archiveKeys = Object.keys(archives)
        const topArchives = archiveKeys
          .map(k => ({ key: k, abbrev: archives[k].abbrev_name || k, size: archives[k].size || 0 }))
          .sort((a, b) => b.size - a.size)
          .slice(0, 3)
        res.push({
          memoryType: mt, sectionName: sn,
          abbrev: sec.abbrev_name || sn,
          size: sec.size || 0,
          archiveCount: archiveKeys.length,
          topArchives,
          fullArchives: archives,
        })
      })
    })
    return res
  }

  function archivesOf(section: SectionInfo): ArchiveInfo[] {
    const arch = section.fullArchives || {}
    return Object.keys(arch).map(k => ({
      key: k,
      abbrev: arch[k].abbrev_name || k,
      size: arch[k].size || 0,
      object_files: arch[k].object_files || {},
    }))
  }

  return { data, error, allSections, archivesOf }
}
