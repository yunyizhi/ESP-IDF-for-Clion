# MCP 工具

>0.9版本及以上

插件会向 CLion 内置的 MCP 服务（JetBrains MCP Server）注册一组 MCP 工具，
使外部 AI 客户端（如接入 MCP 的对话助手）能够在无头环境下复用插件的能力：
执行任务树上的命令、拉取任务输出（运行中增量与已结束结果）、获取项目构建环境信息、更新项目配置、查询可用串口等。

这些工具与插件界面「任务树」点击执行的路径完全一致，因此 MCP 触发的结果与在 IDE 里手动运行一致。

> 工具由 `EspIdfTasksMcpToolsProvider` 通过 `mcpServer.mcpToolsProvider` 扩展点动态注册，
> 无需额外配置即可在开启了 CLion MCP 服务时被发现。

## 工具一览

| 工具名                          | 用途                                   |
|------------------------------|--------------------------------------|
| `espidf_list_tasks`          | 列出当前项目可运行的 ESP-IDF 任务            |
| `espidf_run_task`            | 按名称运行某个 ESP-IDF 任务                 |
| `espidf_fetch_task_output`   | 拉取任务输出：运行中为等待窗口内的增量，已结束为运行窗口保留的结果    |
| `espidf_list_running_tasks`  | 列出任务执行情况（运行中与已结束）                    |
| `espidf_terminate_task`      | 按 `taskId` 终止正在运行的任务              |
| `espidf_get_project_info`    | 获取项目的构建环境信息（环境变量脚本、Profiles、配置等） |
| `espidf_set_project_config`  | 更新项目配置（串口、监视/下载波特率、CMake Profile） |
| `espidf_list_serial_ports`   | 列出当前机器上可用的串口                      |

典型的长时间任务流程：先用 `espidf_list_tasks` 找到任务名，再用 `espidf_run_task`（`async=true`）触发并拿到 `taskId`，
随后用 `espidf_fetch_task_output` 轮询增量日志，必要时用 `espidf_terminate_task` 结束任务；
任务结束后仍可用同一个 `taskId` 调用 `espidf_fetch_task_output` 回看完整结果，
任务执行情况（含已结束记录）可用 `espidf_list_running_tasks` 查看。

---

## espidf_list_tasks

列出所有可以通过 `espidf_run_task` 执行的 ESP-IDF 任务（交互式终端 / TUI 类任务会被排除）。

返回每个任务的：

- `id`：任务标识（可作为 `espidf_run_task` 的 `task` 参数）
- 显示名（display name）
- 描述

对于使用串口监视器（`use-monitor=true`）的任务，会在条目后追加 `[monitor]` 标记，提示该任务通常长时间运行，适合用 `espidf_run_task` 的 `async=true` 触发后立即返回，或用 `waitSeconds` 限定等待超时后拿到已采集的部分日志。

**参数**

| 参数           | 类型   | 必填 | 说明                                         |
|--------------|--------|----|--------------------------------------------|
| `projectPath`| string | 否  | 目标项目基目录绝对路径；多项目打开时用于定位项目，通常可省略 |

---

## espidf_run_task

按名称运行任意非交互式 ESP-IDF 任务（build、flash、size、自定义命令等）。

`task` 参数接受任务的 `id`、显示名或完整路径（大小写不敏感），例如 `"Build"`、`"flash/Flash"`。
可先调用 `espidf_list_tasks` 获取可用任务名。

默认（`async=false`）为同步调用：等待任务结束（或到达 `waitSeconds` 等待超时）后返回捕获的输出与退出码，结构化结果中的 `status` 为 `exited`（若等待超时、任务仍在运行则为 `running`）；
将 `async` 设为 `true` 则仅触发任务并立即返回，`status` 为 `running`，任务在 IDE 控制台后台继续运行，适用于长时间运行或串口监视类任务。

无论是同步还是异步调用，只要任务可被跟踪，都会同时返回 `taskId`，可用于后续 `espidf_fetch_task_output` 拉取输出或 `espidf_terminate_task` 终止任务。

**参数**

| 参数            | 类型    | 必填 | 说明                                                                                     |
|---------------|---------|----|----------------------------------------------------------------------------------------|
| `task`        | string  | 是  | 要运行的 ESP-IDF 任务名称（id / 显示名 / 完整路径，大小写不敏感）                                          |
| `async`       | boolean | 否  | 为 `true` 时仅触发任务并立即返回（`status=running`）；为 `false`（默认）时等待任务结束后返回（`status=exited`） |
| `waitSeconds` | integer | 否  | 通用的等待超时时间（秒），适用于所有任务；默认 600 秒。`async=true` 时忽略                          |
| `maxLines`    | integer | 否  | 返回输出最多保留末尾多少行，默认 200；设为 0 或负数表示不限制                                              |
| `projectPath` | string  | 否  | 目标项目基目录绝对路径；多项目打开时建议填写                                                      |

**返回字段**

| 字段             | 类型      | 说明                                                        |
|----------------|---------|-----------------------------------------------------------|
| `status`       | string  | `running`（仍在运行或仅触发）/ `exited`（已结束）/ `error`              |
| `taskName`     | string  | 任务显示名                                                      |
| `taskId`       | integer | 本次启动的任务标识，可用于后续拉取输出或终止任务                                    |
| `exitCode`     | integer | 进程退出码；任务仍在运行或未取得时缺省                                         |
| `output`       | string  | 捕获的 stdout/stderr 合并输出                                     |
| `truncated`    | boolean | 输出被截断时返回 `true`（仅保留末尾 `maxLines` 行）                         |
| `omittedLines` | integer | 因截断被丢弃的前置行数                                                 |
| `message`      | string  | 错误或非结果状态下的可读信息                                              |

**示例**

```json
{
  "task": "monitor",
  "async": true
}
```

上述调用会立即触发 `monitor` 任务并返回 `status=running` 与 `taskId`，其输出可在 IDE 控制台查看，或用 `espidf_fetch_task_output` 轮询。

```json
{
  "task": "build",
  "waitSeconds": 300,
  "maxLines": 500
}
```

上述调用会等待 `build` 任务结束（最多 300 秒），返回捕获的输出、退出码与 `status=exited`。

---

## espidf_fetch_task_output

按任务状态返回输出，有两种语义：

- **任务仍在运行**：返回「本次调用开始」到「等待结束」之间**新产生的输出**（增量轮询，最多 `maxLines` 行，保留末尾行）。
  调用会订阅任务的实时输出并阻塞至多 `waitSeconds` 秒（默认 30 秒；设为 `0` 立即返回当前已到达的输出）。
  `status` 为 `running`（等待结束时仍在运行）或 `exited`（等待期间结束，返回该窗口的新增输出与退出码）。
- **任务已结束（或未能启动）**：任务的运行窗口里保留的控制台输出即执行结果，直接返回该完整快照（`status=exited`），
  因此已经结束的任务仍可事后读取结果；若任务根本未能启动，则只返回「未能启动」的提示。

> **已结束任务的输出以运行窗口（控制台）仍存活为前提**：tab 被关闭、或运行内容被复用替换后，平台会释放对应的
> 运行内容，控制台文本随之不可读，此时本工具只能返回退出码与「输出不可用」的提示；这类记录随后也会在任务清理中
> 被淘汰（见 `espidf_list_running_tasks`）。调用前可用该工具的每条记录里的 `outputAvailable` 字段、
> 或文本行末尾的 `输出：输出可用/输出不可用` 标记来判断，无需先试错。

通常与 `espidf_run_task(async=true)` 配合，用于轮询长时间运行的任务；任务结束后仍可用同一个 `taskId` 回看结果。

**参数**

| 参数            | 类型     | 必填 | 说明                                                        |
|---------------|--------|----|-----------------------------------------------------------|
| `taskId`      | integer | 是  | 目标任务标识（来自 `espidf_list_running_tasks` 或 `espidf_run_task` 的返回） |
| `waitSeconds` | integer | 否  | 运行中任务等待新输出的最长时间（秒），默认 30；设为 0 表示立即返回；任务已结束时不生效            |
| `maxLines`    | integer | 否  | 返回输出最多保留末尾多少行，默认 200；设为 0 或负数表示不限制                                     |
| `projectPath` | string  | 否  | 目标项目基目录绝对路径；多项目打开时建议填写                                      |

**返回字段**

| 字段             | 类型      | 说明                                                       |
|----------------|---------|----------------------------------------------------------|
| `status`       | string  | `running` / `exited` / `not_found` / `error`              |
| `taskId`       | integer | 被拉取的任务标识                                                 |
| `taskName`     | string  | 任务显示名                                                    |
| `exitCode`     | integer | 进程退出码；任务仍在运行时缺省                                          |
| `output`       | string  | 运行中任务：本次等待窗口内新产生的输出；已结束任务：运行窗口保留的控制台输出（均可能被截断）            |
| `truncated`    | boolean | 输出被截断时返回 `true`                                           |
| `omittedLines` | integer | 因截断被丢弃的前置行数                                              |
| `message`      | string  | 错误或非结果状态下的可读信息                                           |

---

## espidf_list_running_tasks

列出当前项目的 ESP-IDF 任务执行情况（工具名保持不变）：由插件启动、经任务管理器登记的非终端任务，
既包含仍在运行的任务，也包含已经结束（或未能启动）的保留记录及其状态、退出码与耗时。

- 运行中任务的 `taskId` 可交给 `espidf_terminate_task` 终止，或用 `espidf_fetch_task_output` 拉取增量输出；
- 已结束任务的 `taskId` 可用 `espidf_fetch_task_output` 读取运行窗口中保留的完整输出
  （**以运行窗口的控制台仍存活为前提**，每条记录的 `outputAvailable` 给出该判定）。

已结束记录有两种淘汰时机：

- 该任务在运行窗口里的内容（tab）被关闭后，记录会在**下一次新建任务或任务结束**时被顺带清理（不在关闭瞬间响应，允许一点延迟）；
- 作为兜底，最多保留 20 条已结束记录，超出时淘汰最早结束的。

运行中的任务不受这两种限制。

**参数**

| 参数                | 类型      | 必填 | 说明                                         |
|-------------------|---------|----|--------------------------------------------|
| `includeFinished` | boolean | 否  | 是否包含已结束（或未能启动）的任务记录，默认 `true`              |
| `projectPath`     | string  | 否  | 目标项目基目录绝对路径；多项目打开时用于定位项目，通常可省略              |

**返回字段**

| 字段              | 类型      | 说明                                                                                                          |
|-----------------|---------|-------------------------------------------------------------------------------------------------------------|
| `count`         | integer | 返回的任务执行记录数量                                                                                                 |
| `runningCount`  | integer | 仍在运行的任务数量                                                                                                   |
| `finishedCount` | integer | 已结束（或未能启动）的任务数量                                                                                             |
| `tasks`         | array   | 任务执行记录列表，每项包含 `taskId`、`name`、`state`（`running` / `exited` / `not_started`）、`executionId`、`startTimeMillis`、`durationMillis`、`outputAvailable`（该任务的运行窗口输出当前是否可读），已结束时还包含 `exitCode` |

> `outputAvailable=false` 表示对应运行窗口 tab 已关闭或运行内容已被复用替换：此时 `espidf_fetch_task_output`
> 对该任务只能返回退出码与提示，接下来这条记录也会在任务清理中被淘汰。
>
> 返回的**文本**里每行末尾同样给出输出可用性（`输出：输出可用` / `输出：输出不可用`），
> 因此只读文本、不解析结构化字段的调用方也能直接判断，示例：
>
> ```text
> ESP-IDF 任务执行情况
> - [2] Monitor  （运行中，167 秒，输出：输出可用）
> - [1] Monitor  （已结束：exited，exitCode=0，4 秒，输出：输出不可用）
> ```

---

## espidf_terminate_task

按 `taskId` 终止一个正在运行的 ESP-IDF 任务（`taskId` 来自 `espidf_list_running_tasks` 或 `espidf_run_task` 的返回）。
已结束的任务仍会出现在任务执行列表中，但无法终止：此时返回 `status=not_running` 与退出码，
其结果可用 `espidf_fetch_task_output` 读取。

**参数**

| 参数            | 类型     | 必填 | 说明                          |
|---------------|--------|----|-----------------------------|
| `taskId`      | integer | 是  | 要终止的运行中任务标识                  |
| `projectPath` | string  | 否  | 目标项目基目录绝对路径；多项目打开时用于定位项目，通常可省略 |

**返回字段**

| 字段         | 类型      | 说明                                                              |
|------------|---------|-----------------------------------------------------------------|
| `status`   | string  | 找到运行中任务并发起终止时为 `terminated`；任务已结束为 `not_running`；未找到该任务时为 `not_found` |
| `taskId`   | integer | 请求终止的 `taskId`                                                  |
| `exitCode` | integer | 已结束任务的退出码；仅在 `status=not_running` 时返回                             |

---

## espidf_get_project_info

返回当前项目的关键信息，供 MCP 客户端在无头环境下复用与界面一致的构建环境：

- 项目名称与基目录
- 当前激活的 CMake Profile，及其 Target、构建目录
- 全部可用 CMake Profile（标注当前激活项，并附带各自的 Target 与构建目录），供切换 Profile 时选用
- 关联的工具链（Toolchain）名称及其环境变量导出脚本路径
  （客户端可自行 `source` 该脚本以获得完整构建环境变量，如果不是特别关注每个任务输出，也可以令 Ai Agent 通过环境变量脚本加命令行操作，实现更灵活的文本处理。）
- 项目级配置：串口、监视波特率、下载波特率、CMake Profile

**参数**

| 参数           | 类型   | 必填 | 说明                                         |
|--------------|--------|----|--------------------------------------------|
| `projectPath`| string | 否  | 目标项目基目录绝对路径；多项目打开时用于定位项目，通常可省略 |

---

## espidf_set_project_config

更新项目级配置，等价于在设置面板中点击「保存」：

- 串口（`ESPPORT`）
- 监视波特率、下载波特率
- 激活的 CMake Profile（会同步切换运行/调试配置的执行目标）

只修改传入的参数，未传入的字段保持当前值不变；**至少需要传入一个参数**。
可先用 `espidf_get_project_info` 查看当前值与可用的 CMake Profile。

**参数**

| 参数            | 类型     | 必填 | 说明                                                                             |
|---------------|--------|----|--------------------------------------------------------------------------------|
| `port`        | string  | 否  | 用于烧录 / 监视的串口（如 `/dev/ttyUSB0` 或 `COM3`）；省略则保持当前值                               |
| `monitorBaud` | string  | 否  | 监视波特率（如 `115200`）；省略则保持当前值                                                     |
| `uploadBaud`  | string  | 否  | 下载 / 烧录波特率（如 `460800`）；省略则保持当前值                                                |
| `profile`     | string  | 否  | 要激活的 CMake Profile 显示名，必须是 `espidf_get_project_info` 返回的可用 Profile 之一；省略则保持当前值 |
| `projectPath` | string  | 否  | 目标项目基目录绝对路径；多项目打开时建议填写                                                     |

返回更新后的串口、监视波特率、下载波特率与 CMake Profile。

---

## espidf_list_serial_ports

列出当前机器上可用的串口，便于为烧录 / 监视选择 `ESPPORT`。

串口是机器级资源，与具体项目无关，因此该工具不需要 `projectPath` 参数。

返回每个串口的：

- 端口路径 / 名称（`comPort`）
- 描述信息（`descriptivePortName`、`portDescription`）
- 对于可识别的 USB 串口，还会返回厂商 / 产品信息
  （`vendorId`、`productId`、`vendorName`、`productName`）

**参数**：无。

---

## 使用前提

1. 在 CLion 中启用 JetBrains MCP Server（CLion 内置 MCP 服务）。
2. 将本插件注册的工具暴露给外部 AI 客户端。
3. 当存在多个打开的项目时，通过 `projectPath` 明确指定目标项目。


### 其他说明
和本插件提供的工具无关，clion自带mcp工具中 `xdebug_start_debugger_session` 可以启动 本插件提供的`ESP-IDF Debug`运行配置，用于调试。
