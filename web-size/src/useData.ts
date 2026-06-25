import { ref } from 'vue'

// ---- 旧类型（保留向后兼容） ----

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

// ---- 新增：跨内存区域聚合类型 ----

/** 跨所有内存区域聚合后的符号信息 */
export interface SymbolAggInfo {
  key: string
  abbrev: string
  totalSize: number
  memSizes: Record<string, number> // 每个内存区域的大小
  mtSections: Record<string, Record<string, number>> // 内存区域 → 段名 → 大小
}

/** 跨所有内存区域聚合后的目标文件信息 */
export interface ObjFileAggInfo {
  key: string
  abbrev: string
  totalSize: number
  memSizes: Record<string, number>
  symbols: SymbolAggInfo[]
}

/** 跨所有内存区域聚合后的库信息 */
export interface ArchiveAggInfo {
  key: string
  abbrev: string
  totalSize: number
  memSizes: Record<string, number>
  objectFiles: ObjFileAggInfo[]
}

// ---- useData ----

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

  // ---- 旧函数（保留向后兼容） ----

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

  // ---- 新增：跨内存区域聚合 ----

  /** 获取有序的内存类型名称列表 */
  function memoryTypeKeys(): string[] {
    if (!data.value) return []
    return Object.keys(data.value.memory_types || {})
  }

  /** 跨所有内存区域聚合所有库 */
  function allArchives(): ArchiveAggInfo[] {
    if (!data.value) return []
    const memTypes = data.value.memory_types || {}

    // archiveMap: key → 聚合数据
    const archiveMap: Record<string, {
      abbrev: string
      memSizes: Record<string, number>
      objFileMap: Record<string, {
        abbrev: string
        memSizes: Record<string, number>
        symMap: Record<string, {
          memSizes: Record<string, number>                  // memType → size
          mtSections: Record<string, Record<string, number>> // memType → sectionName → size
        }>
        symAbbrevMap: Record<string, string>                // symKey → abbrev
      }>
    }> = {}

    for (const [mt, mtInfo] of Object.entries(memTypes) as [string, any][]) {
      const sections = mtInfo.sections || {}
      for (const [sn, sec] of Object.entries(sections) as [string, any][]) {
        const archives = sec.archives || {}
        for (const [ak, arch] of Object.entries(archives) as [string, any][]) {
          // 初始化库条目
          if (!archiveMap[ak]) {
            archiveMap[ak] = { abbrev: arch.abbrev_name || ak, memSizes: {}, objFileMap: {} }
          }
          archiveMap[ak].memSizes[mt] = (archiveMap[ak].memSizes[mt] || 0) + (arch.size || 0)

          // 聚合目标文件
          const objFiles = arch.object_files || {}
          for (const [ok, obj] of Object.entries(objFiles) as [string, any][]) {
            if (!archiveMap[ak].objFileMap[ok]) {
              archiveMap[ak].objFileMap[ok] = {
                abbrev: obj.abbrev_name || ok,
                memSizes: {},
                symMap: {},
                symAbbrevMap: {},
              }
            }
            archiveMap[ak].objFileMap[ok].memSizes[mt] =
              (archiveMap[ak].objFileMap[ok].memSizes[mt] || 0) + (obj.size || 0)

            // 聚合符号
            const symbols = obj.symbols || {}
            for (const [sk, sym] of Object.entries(symbols) as [string, any][]) {
              if (!archiveMap[ak].objFileMap[ok].symMap[sk]) {
                archiveMap[ak].objFileMap[ok].symMap[sk] = { memSizes: {}, mtSections: {} }
                archiveMap[ak].objFileMap[ok].symAbbrevMap[sk] = sym.abbrev_name || sk
              }
              const symEntry = archiveMap[ak].objFileMap[ok].symMap[sk]
              const symSize = sym.size || 0
              symEntry.memSizes[mt] = (symEntry.memSizes[mt] || 0) + symSize
              if (!symEntry.mtSections[mt]) symEntry.mtSections[mt] = {}
              symEntry.mtSections[mt][sn] = (symEntry.mtSections[mt][sn] || 0) + symSize
            }
          }
        }
      }
    }

    // 转换为最终结构
    return Object.entries(archiveMap).map(([key, val]) => {
      const objectFiles: ObjFileAggInfo[] = Object.entries(val.objFileMap)
        .map(([ok, ov]) => {
          const symbols: SymbolAggInfo[] = Object.entries(ov.symMap)
            .map(([sk, sv]) => ({
              key: sk,
              abbrev: ov.symAbbrevMap[sk] || sk,
              totalSize: Object.values(sv.memSizes).reduce((a, b) => a + b, 0),
              memSizes: { ...sv.memSizes },
              mtSections: { ...sv.mtSections },
            }))
            .sort((a, b) => b.totalSize - a.totalSize)

          return {
            key: ok,
            abbrev: ov.abbrev,
            totalSize: Object.values(ov.memSizes).reduce((a, b) => a + b, 0),
            memSizes: { ...ov.memSizes },
            symbols,
          }
        })
        .sort((a, b) => b.totalSize - a.totalSize)

      return {
        key,
        abbrev: val.abbrev,
        totalSize: Object.values(val.memSizes).reduce((a, b) => a + b, 0),
        memSizes: { ...val.memSizes },
        objectFiles,
      }
    }).sort((a, b) => b.totalSize - a.totalSize)
  }

  /** 获取指定库的所有目标文件（已跨内存区域聚合） */
  function objectFilesOf(archiveKey: string): ObjFileAggInfo[] {
    const archive = allArchives().find(a => a.key === archiveKey)
    return archive ? archive.objectFiles : []
  }

  /** 获取指定目标文件的所有符号（已跨内存区域聚合） */
  function symbolsOf(archiveKey: string, objFileKey: string): SymbolAggInfo[] {
    const archive = allArchives().find(a => a.key === archiveKey)
    if (!archive) return []
    const objFile = archive.objectFiles.find(o => o.key === objFileKey)
    return objFile ? objFile.symbols : []
  }

  // 获取内存类型的 used/size 信息
  function memUsageInfo(): Record<string, { used: number; size: number }> {
    if (!data.value) return {}
    const info: Record<string, { used: number; size: number }> = {}
    const memTypes = data.value.memory_types || {}
    for (const [mt, mtInfo] of Object.entries(memTypes) as [string, any][]) {
      info[mt] = { used: mtInfo.used || 0, size: mtInfo.size || 0 }
    }
    return info
  }

  return { data, error, allSections, archivesOf, memoryTypeKeys, allArchives, objectFilesOf, symbolsOf, memUsageInfo }
}
