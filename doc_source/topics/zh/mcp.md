# MCP 工具

>0.9版本及以上

插件会向 CLion 内置的 MCP 服务（JetBrains MCP Server）注册一组 MCP 工具，
使外部 AI 客户端（如接入 MCP 的对话助手）能够在无头环境下复用插件的能力：
执行任务树上的命令、获取项目构建环境信息、查询可用串口等。

这些工具与插件界面「任务树」点击执行的路径完全一致，因此 MCP 触发的结果与在 IDE 里手动运行一致。

> 工具由 `EspIdfTasksMcpToolsProvider` 通过 `mcpServer.mcpToolsProvider` 扩展点动态注册，
> 无需额外配置即可在开启了 CLion MCP 服务时被发现。

## 工具一览

| 工具名                       | 用途                                   |
|---------------------------|--------------------------------------|
| `espidf_list_tasks`       | 列出当前项目可运行的 ESP-IDF 任务            |
| `espidf_run_task`         | 按名称运行某个 ESP-IDF 任务                 |
| `espidf_get_project_info` | 获取项目的构建环境信息（环境变量脚本、配置等）        |
| `espidf_list_serial_ports`| 列出当前机器上可用的串口                      |

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

**参数**

| 参数            | 类型    | 必填 | 说明                                                                                     |
|---------------|---------|----|----------------------------------------------------------------------------------------|
| `task`        | string  | 是  | 要运行的 ESP-IDF 任务名称（id / 显示名 / 完整路径，大小写不敏感）                                          |
| `async`       | boolean | 否  | 为 `true` 时仅触发任务并立即返回（`status=running`）；为 `false`（默认）时等待任务结束后返回（`status=exited`） |
| `waitSeconds` | integer | 否  | 通用的等待超时时间（秒），适用于所有任务；默认 600 秒。`async=true` 时忽略                          |
| `projectPath` | string  | 否  | 目标项目基目录绝对路径；多项目打开时建议填写                                                      |

**示例**

```json
{
  "task": "monitor",
  "async": true
}
```

上述调用会立即触发 `monitor` 任务并返回 `status=running`，其输出可在 IDE 控制台查看。

```json
{
  "task": "build",
  "waitSeconds": 300
}
```

上述调用会等待 `build` 任务结束（最多 300 秒），返回捕获的输出、退出码与 `status=exited`。

---

## espidf_get_project_info

返回当前项目的关键信息，供 MCP 客户端在无头环境下复用与界面一致的构建环境：

- 项目名称与基目录
- 当前选中的 CMake Profile、Target、构建目录
- 关联的工具链（Toolchain）名称及其环境变量导出脚本路径
  （客户端可自行 `source` 该脚本以获得完整构建环境变量，如果不是特别关注每个任务输出，也可以令Ai Agent通过环境变量脚本加命令行操作，实现更灵活的文本处理。
- 项目级配置：串口、监视波特率、下载波特率、CMake Profile

**参数**

| 参数           | 类型   | 必填 | 说明                                         |
|--------------|--------|----|--------------------------------------------|
| `projectPath`| string | 否  | 目标项目基目录绝对路径；多项目打开时用于定位项目，通常可省略 |

---

## espidf_list_serial_ports

列出当前机器上可用的串口，便于为烧录 / 监视选择 `ESPPORT`。

串口是机器级资源，与具体项目无关，因此该工具不需要 `projectPath` 参数。

返回每个串口的：

- 端口路径 / 名称
- 描述信息（descriptivePortName、portDescription）
- 对于可识别的 USB 串口，还会返回厂商 / 产品信息
  （vendorId、productId、vendorName、productName）

**参数**：无。

---

## 使用前提

1. 在 CLion 中启用 JetBrains MCP Server（CLion 内置 MCP 服务）。
2. 将本插件注册的工具暴露给外部 AI 客户端。
3. 当存在多个打开的项目时，通过 `projectPath` 明确指定目标项目。


### 其他说明
和本插件提供的工具无关，clion自带mcp工具中 `xdebug_start_debugger_session`可以启动 本插件提供的`ESP-IDF Debug`运行配置，用于调试。