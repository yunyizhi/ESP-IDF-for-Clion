# 文档更新操作记录（Writerside → `../docs`）

本文记录「改文档 → 生成站点产物 → 提交」这条链路怎么走，供以后照做。
本项目帮助文档由 **Writerside**（运行配置类型内部名为 `StardustRunConfiguration`）生成，中文单语。

---

## 一、目录与文件

| 路径 | 作用 | 是否随版本控制 |
|------|------|----------------|
| `doc_source/topics/zh/*.md` | 文档正文（唯一需要手写的地方） | 是 |
| `../doc_source/espidf.tree` | 目录树（`toc-element topic="xxx.md"`），**新增/移动页面必须在这里登记** | 是 |
| `../doc_source/v.list`、`c.list`、`writerside.cfg` | Writerside 变量/配置 | 是 |
| `../doc_source/images` | 文档图片 | 是 |
| `../doc_source/webHelpESPIDF2-all.zip` | 中间产物：Writerside 打出的 webhelp 归档 | 否（生成物） |
| `../docs` | 站点产物，被插件打成帮助包 | 是 |
| `../docs/esp-tasks.xsd`、`docs/esp-tasks-v0.7.xsd`、`docs/esp-tasks-v0.9.xsd` | 自定义任务 XSD，**手工维护**，不被解压流程覆盖 | 是 |

> `updateDocs` 清理 `../docs` 时会 **排除 `**/*.xsd`**，所以自定义任务的 XSD 放在 `docs/` 下是安全的，不会被生成的 html 冲掉。

---

## 二、操作步骤

### 1. 生成 webhelp 归档

运行 Writerside 的 `Web Archive (espidf)` 运行配置（配置定义：`folderPath=$PROJECT_DIR$/doc_source`、`instanceId=espidf`、factory `save-as-zip`）。

- 在 IDEA 里：直接点该运行配置运行；
- 或通过 JetBrains MCP 调用（`idea` 服务器）：

```json
{
  "tool": "execute_run_configuration",
  "projectPath": "/home/lustre/codes/idea/esp-idf",
  "configurationName": "Web Archive (espidf)",
  "waitForExit": true,
  "timeout": 600000
}
```

期望输出：

```
Building "webHelpESPIDF2-all.zip"
Writing topics...
Writing assets...
```

**注意**：MCP 调用返回的 `exitCode` 可能是 `-1`（平台侧未取到退出码），**不能作为成败判据**。请用产物校验：

```bash
ls -l --time-style=+%F\ %H:%M doc_source/webHelpESPIDF2-all.zip   # 时间戳应为刚刚
```

### 2. 解压到 `../docs`

```bash
./gradlew updateDocs --offline
```

该任务由两部分组成（见 `../build.gradle.kts`）：

1. `unzipWebHelpAndPreserveXsd`：删除 `../docs` 下 **除 `**/*.xsd` 之外**的所有文件；
2. `actuallyUnzipWebHelp`：把 `../doc_source/webHelpESPIDF2-all.zip` 解压到 `docs/`。

期望输出结尾：

```
> Task :unzipWebHelpAndPreserveXsd
> Task :actuallyUnzipWebHelp
Unzipping webHelpESPIDF2-all.zip into /home/lustre/codes/idea/esp-idf/docs
BUILD SUCCESSFUL
```

### 3. 校验产物（可选但推荐）

生成的是静态页，直接 grep 本次改动的关键字，确认不是旧产物：

```bash
grep -c "outputAvailable" docs/mcp.html      # 应为 1
grep -c "输出可用" docs/mcp.html              # 应为 2
```

### 4. 提交

```bash
git add -- doc_source docs   # 如同时改了代码/其它文档，按需一并加入
git commit -m "docs: 更新 xxx 文档" -m "说明本次改了什么"
```

**不要提交**这些（生成物或 IDE 缓存）：

| 路径 | 原因 |
|------|------|
| `../doc_source/webHelpESPIDF2-all.zip` | 每次生成的 3.7 MB 归档 |
| `../.idea`、`.intellijPlatform/` | IDE / IntelliJ Platform 缓存 |
| `../web-size/package-lock.json` | `npm install` 产物（是否纳入由项目决定，目前未跟踪） |

---

## 三、本次执行记录（2026-10-07）

背景：`../doc_source/topics/zh/mcp.md` 增加了 `outputAvailable` 字段说明与文本标记示例，需要重新生成站点页。

| 步骤 | 操作 | 结果 |
|------|------|------|
| 1 | idea MCP `execute_run_configuration` → `Web Archive (espidf)` | 输出 `Building "webHelpESPIDF2-all.zip" / Writing topics... / Writing assets...`；MCP 返回 `exitCode=-1`，产物 `../doc_source/webHelpESPIDF2-all.zip` 3.7 MB、时间戳 23:48 ✓ |
| 2 | `./gradlew updateDocs --offline` | `Unzipping webHelpESPIDF2-all.zip into .../docs`，`BUILD SUCCESSFUL in 815ms` ✓ |
| 3 | 校验 | `../docs/mcp.html`：`outputAvailable` 1 处、`输出可用` 2 处 ✓ |
| 4 | `git commit` | `a60bae6 fix(mcp): 任务列表暴露输出可用性并去除验证器告警`（26 files，+515/−139，含 `docs/*.html`） |

---

## 四、常见问题

1. **只改了 md，`../docs` 为什么没变？**
   `../docs` 完全来自 `updateDocs` 解压 zip，而 zip 来自 Writerside 运行配置；只跑其中一步都不会更新站点。

2. **`updateDocs` 会不会删掉我放在 `../docs` 下的东西？**
   会， **除了 `.xsd`**。自定义任务 XSD 因此可以放心放在 `../docs` 下；其它手工文件请放到 `doc_source/` 或别处。

3. **新增一篇页面**
   在 `../doc_source/topics/zh` 建 md，并在 `doc_source/espidf.tree` 里挂上 `toc-element topic="你的文件.md"`（可嵌套，例如 `customTask.md` 挂在 `toolTree.md` 下），否则不会出现在目录里。

4. **MCP 调用返回 `exitCode=-1` 是不是失败？**
   不是判据，看 zip 时间戳与运行窗口输出；必要时在 IDEA 里直接点运行配置重试。

5. **构建离线可用吗？**
   可以，示例均带 `--offline`；`updateDocs` 只做本地删文件 + 解压 zip，不依赖网络。
