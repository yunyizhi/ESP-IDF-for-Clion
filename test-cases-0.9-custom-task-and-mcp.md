# ESP-IDF for CLion 0.9 回归测试用例：自定义任务 XML 与 MCP

面向在**其他机器**上执行的 AI Agent。全部为**正向用例**，不需要覆盖"关闭运行窗口后再取输出"这类边界。

执行者需要的能力：能调用 CLion 内置 MCP 服务暴露的 `espidf_*` 工具、能在目标项目根目录读写文件、能判断命令输出内容。

---

## 一、本轮更新摘要（git，2026-10-03 ~ 2026-10-07）

| 日期 | 提交 | 内容 |
|------|------|------|
| 10-03 | `64481ea` | 运行配置改用 `ExecutionException` 替代 `UnsupportedOperationException` |
| 10-04 | `e3ee1f4` | **自定义任务支持环境变量**：`env` 属性 / `<env>` 子标签；新增 `esp-tasks-v0.9.xsd` |
| 10-04 | `3d9057a` | **自定义任务支持变量组 `profile` 与占位符展开**：`<profile>`（匿名/命名，含 `<envs>` / `<macros>`）、`${env:key}` / `${v:key}`；gutter 运行图标同步支持 |
| 10-04 | `6065f93` | XSD 修正：`exec` 的 `args` 与 `env` 允许任意顺序 |
| 10-04 | `e8bc975` | **任务管理 + 拉取输出**：新增 `CmdTaskManager` / `CmdTaskRegistry`，重写 `espidf_run_task`，新增 `espidf_fetch_task_output`、`espidf_list_running_tasks`、`espidf_terminate_task`、`McpSchemaUtils`、`McpTaskOutputCollector`；移除旧 `CmdTaskExecutor` |
| 10-04 | `6b2decd` | MCP 输出按完整行截断（`McpOutputView`），`espidf_run_task` 显式暴露 `taskId` |
| 10-04 | `4e28427` | 新增 `espidf_set_project_config`，`espidf_get_project_info` 展示全部 CMake profile |
| 10-04 | `76e34cc` | 文档：`mcp.md`、`customTask.md`、`toolTree.md` |
| 10-07 | `353d60b` | **任务记录保留至结束**：`espidf_list_running_tasks` 语义变为"任务执行情况"（含已结束记录，`includeFinished` 默认 true），已结束任务可读取运行窗口保留的输出，`espidf_terminate_task` 对已结束任务返回 `not_running` |

本轮测试重点：自定义任务 XML 的**宏 / 环境变量 / exec 输出**，以及 MCP 的**异步构建 + 输出获取 + 常驻任务终止**。

---

## 二、环境与前置检查

1. 一台已安装 **CLion + ESP-IDF 插件 0.9（或更新）** 的机器。
2. 一个**已配置好 toolchain 的 ESP-IDF 项目**，该项目能用 IDE 里的 `Build` 任务构建成功（`<PROJECT_PATH>` 指其根目录绝对路径）。
3. CLion 的内置 **MCP Server 已启用**，并且执行者能调用插件注册的 `espidf_*` 工具。

**前置断言（不满足则停止并报告环境问题）**

| 检查 | 方法 | 期望 |
|------|------|------|
| MCP 工具可用 | 调用 `espidf_list_tasks`（`projectPath=<PROJECT_PATH>`） | 返回任务清单，至少含 `Build` |
| 插件版本为本轮版本 | 查看 `espidf_list_running_tasks` 的**输入参数** | 含 `includeFinished` 参数（说明是 10-07 之后的版本） |
| 项目可构建 | 见用例 B | `Build` 任务的 `exitCode=0` |
| 脚本运行时可用 | 目标机执行 `python --version`（或 `python3 --version`） | 能正常输出版本号（供 B6 常驻脚本使用） |

> 说明：MCP 侧读取自定义任务时会**每次重新解析** `esp_custom_tasks.xml`（包括编辑器中未保存的内容），因此改完 XML 后**不需要**点任务树的 `Load` 按钮即可被 MCP 工具识别；任务树 UI 侧才需要手动 Load，本用例不涉及 UI 侧。

---

## 三、用例 A：自定义任务 XML（宏、环境变量、echo 验证）

### A0. 写入测试用自定义任务文件

在 `<PROJECT_PATH>/esp_custom_tasks.xml` 写入以下内容（若文件已存在，先备份再覆盖）：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<esp-tasks xmlns="https://yunyizhi.github.io/ESP-IDF-for-Clion/schema/v0.9"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="https://yunyizhi.github.io/ESP-IDF-for-Clion/schema/v0.9 https://yunyizhi.github.io/ESP-IDF-for-Clion/esp-tasks-v0.9.xsd">

    <!-- 匿名变量组：对所有任务生效 -->
    <profile>
        <envs>
            TC_GLOBAL_ENV=global-env-value
        </envs>
        <macros>
            TC_GLOBAL_MACRO=global-macro-value
        </macros>
    </profile>

    <!-- 命名变量组：由任务的 profile 属性引用 -->
    <profile name="tc">
        <envs>
            TC_PROFILE_ENV=profile-env-value
        </envs>
        <macros>
            TC_PROFILE_MACRO=profile-macro-value
        </macros>
    </profile>

    <!-- A1：宏展开（与 shell 无关，跨平台一致） -->
    <exec name="TC Echo Macro" profile="tc"
          args="echo TC_MACRO_GLOBAL=${v:TC_GLOBAL_MACRO} TC_MACRO_PROFILE=${v:TC_PROFILE_MACRO}"/>

    <!-- A2：环境变量注入 + ${env:key} 展开（含任务级内联 env） -->
    <exec name="TC Echo Env" profile="tc" env="TC_INLINE_ENV=inline-env-value"
          args="echo TC_ENV_GLOBAL=${env:TC_GLOBAL_ENV} TC_ENV_PROFILE=${env:TC_PROFILE_ENV} TC_ENV_INLINE=${env:TC_INLINE_ENV}"/>

    <!-- A5：自定义 command 任务（走 idf.py，需项目可构建） -->
    <command name="TC Custom Build" value="build" console-filter="true"/>
</esp-tasks>
```

注意事项：

- 用例中的 `exec` **不要**加 `in-terminal="true"`：终端类任务不会暴露给 MCP，会查不到任务。
- 只有 `exec`（非终端）、`command` 可以被 MCP 执行；`console-command`、`in-terminal` 的 `exec` 不在 MCP 任务列表中。
- 命名冲突：任务 `name` 在文件内必须唯一。

### A1. 宏展开（`${v:key}`）

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_list_tasks {projectPath: "<PROJECT_PATH>"}` | 出现 `TC Echo Macro`，标识形如 `custom:TC-Echo-Macro`（自定义任务统一带 `custom:` 前缀，按任务名派生，大小写不敏感） |
| 2 | `espidf_run_task {projectPath: "<PROJECT_PATH>", task: "TC Echo Macro"}` | `status=exited`、`exitCode=0`；`output` 含 `TC_MACRO_GLOBAL=global-macro-value` 与 `TC_MACRO_PROFILE=profile-macro-value` |

判定要点：匿名变量组的宏与命名变量组的宏都被展开，输出中**不应**残留 `${v:` 字样，也不应出现 `${...}` 被替换成空值（即等号后为空）。

### A2. 环境变量注入 + `${env:key}` 展开

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_run_task {projectPath: "<PROJECT_PATH>", task: "TC Echo Env"}` | `status=exited`、`exitCode=0`；`output` 含 `TC_ENV_GLOBAL=global-env-value`、`TC_ENV_PROFILE=profile-env-value`、`TC_ENV_INLINE=inline-env-value` |

判定要点：覆盖了三种来源的合并顺序（匿名 → 命名 → 任务内联），输出中不应残留 `${env:`。

### A3. 环境变量确实注入了进程环境（shell 原生语法）

插件对 `exec` 使用**系统默认 shell**（Linux/macOS：`bash -c` / `zsh -c`；Windows：`cmd.exe /c`），因此可以用 shell 自身的变量语法交叉验证。

追加以下任务（**按平台选一条**）并重新加载：

```xml
<!-- Linux / macOS -->
<exec name="TC Echo Shell Env" profile="tc"
      args="echo SHELL_SEES_GLOBAL=$TC_GLOBAL_ENV SHELL_SEES_PROFILE=$TC_PROFILE_ENV"/>
```

```xml
<!-- Windows -->
<exec name="TC Echo Shell Env" profile="tc"
      args="echo SHELL_SEES_GLOBAL=%TC_GLOBAL_ENV% SHELL_SEES_PROFILE=%TC_PROFILE_ENV%"/>
```

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_run_task {projectPath: "<PROJECT_PATH>", task: "TC Echo Shell Env"}` | `exitCode=0`；`output` 含 `SHELL_SEES_GLOBAL=global-env-value`、`SHELL_SEES_PROFILE=profile-env-value` |

判定要点：等号右侧是实际值，而不是空值或原样的 `$TC_GLOBAL_ENV`。

### A4. `exec` 使用 `idf-env` 继承 CMake Profile 环境（可选，需项目已配置 toolchain）

追加任务：

```xml
<exec name="TC Echo Idf Env" idf-env="true" args="echo IDF_VERSION=$ESP_IDF_VERSION"/>
```

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_run_task {projectPath: "<PROJECT_PATH>", task: "TC Echo Idf Env"}` | `exitCode=0`；`output` 含 `IDF_VERSION=` 且后面是形如 `v5.5.4` 的版本号（非空、无残留 `$ESP_IDF_VERSION`） |

判定要点：说明任务继承了 ESP-IDF 环境（`idf-env="true"` 生效）。若项目未配置 toolchain，此用例记为"跳过（环境不具备）"。

### A5. 自定义 `command` 任务执行构建（可选）

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_list_tasks` | 出现 `TC Custom Build`，标识形如 `custom:TC-Custom-Build` |
| 2 | `espidf_run_task {projectPath: "<PROJECT_PATH>", task: "TC Custom Build", waitSeconds: 600}` | `status=exited`、`exitCode=0`；`output` 含 `Project build complete` |

判定要点：自定义 `command` 与内置任务走同一条执行路径（等价于点任务树的 `Build`）。

---

## 四、用例 B：MCP 异步构建与输出获取

> 全程**不要关闭**相关任务的运行窗口（tab），包括下面 B6 用到的常驻任务。本批次不覆盖"关闭后再取输出"。
> `<N>` 表示 B1 返回的 `taskId`，`<M>` 表示 B6 返回的 `taskId`。

### B1. 异步触发构建

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_run_task {projectPath: "<PROJECT_PATH>", task: "Build", async: true}` | `status=running`；返回 `taskId`（记为 `<N>`）与"任务已触发"的说明文本 |

判定要点：**必须返回 `taskId`**；若 `taskId` 缺失，本批用例不成立（这是 10-04 提交专门修正的点）。

### B2. 在任务执行情况里能看到它

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_list_running_tasks {projectPath: "<PROJECT_PATH>", includeFinished: false}` | `tasks` 中含 `taskId=<N>`；若构建尚未结束，`state=running`，`runningCount>=1` |
| 2 | `espidf_list_running_tasks {projectPath: "<PROJECT_PATH>"}`（默认 `includeFinished=true`） | 一定含 `taskId=<N>`；`state` 为 `running` 或 `exited`（取决于构建是否已结束），`count = runningCount + finishedCount`，且该记录 `outputAvailable=true`（运行窗口未关闭） |

判定要点：任务结束时记录**不会消失**（这是 10-07 提交的行为）；`includeFinished=false` 只返回仍在运行的任务。

### B3. 构建过程中拉取增量输出（中途）

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_fetch_task_output {projectPath: "<PROJECT_PATH>", taskId: <N>, waitSeconds: 20, maxLines: 200}` | 二选一：<br>· 构建仍在进行：`status=running`，`output` 为本窗口内新增输出（可能为空）<br>· 构建已结束：`status=exited` 且带 `exitCode` |
| 2 | 若步骤 1 为 `running`，可再调用一次同样参数 | 输出应随构建推进而变化（累计日志增长），`status` 仍可能为 `running` |

判定要点：`waitSeconds` 表示"最多阻塞等待新输出的秒数"，不应出现长时间无响应的错误；`output` 中不应残留 ANSI 控制字符以外的异常内容（正常构建日志即可）。

### B4. 构建结束后取结果（本批核心断言）

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | 重复 `espidf_fetch_task_output {waitSeconds: 30}` 直到 `status=exited`（最多轮询 20 次） | 某次返回 `status=exited`、`exitCode=0` |
| 2 | 构建结束后**再调用一次** `espidf_fetch_task_output {projectPath: "<PROJECT_PATH>", taskId: <N>, maxLines: 200}` | `status=exited`、`exitCode=0`；`output` 为运行窗口保留的日志，含 `Project build complete` |

判定要点：

- 结束后的这次调用返回的是**运行窗口里保留的完整输出**（不是增量），因此日志长度应明显大于单次轮询窗口的内容，并包含构建结尾的关键行 `Project build complete`。
- `exitCode=0` 表示构建成功；`truncated=true` 时说明被 `maxLines` 截断，只保留末尾行——此时仍应能看到结尾关键行。

### B5. 结束后的记录与终止行为

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_list_running_tasks {projectPath: "<PROJECT_PATH>"}` | 含 `taskId=<N>`，`state=exited`，`exitCode=0` |
| 2 | `espidf_terminate_task {projectPath: "<PROJECT_PATH>", taskId: <N>}` | 返回错误文本 + 结构化字段 `status=not_running`、`exitCode=0`（已结束的任务不能再终止，提示改用 `espidf_fetch_task_output` 取结果） |

### B6. 常驻任务与终止（自定义 python 脚本）

不再用 Monitor / fullclean 这类不确定耗时的任务，改用一个**自己写的常驻 python 脚本**：耗时可控、可被信号优雅退出，并且不占用串口。

#### B6.0 脚本与任务定义

把下面的脚本写到 `<PROJECT_PATH>/tc_long_task.py`（**工作目录就是 ESP-IDF 项目根目录**，脚本里的相对路径都按项目根解析；脚本第一行会打印实际 cwd 供核对）：

```python
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""常驻测试任务：验证 MCP 的异步执行、增量拉取输出与终止。

- 工作目录：ESP-IDF 项目根目录（插件启动进程时已设置为项目根目录）
- 每秒打印一行心跳；必须 flush=True，否则 MCP 拉不到增量输出
- 收到 SIGINT / SIGTERM（Windows 上还有 SIGBREAK）后打印退出信息并正常结束
- --seconds 为自保护上限，避免忘记终止时一直占用
"""
import argparse
import os
import signal
import sys
import time

_stop = False


def _on_signal(signum, _frame):
    global _stop
    _stop = True
    print(f"[tc_long_task] signal {signum} received, exiting", flush=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seconds", type=int, default=300, help="最长运行秒数")
    parser.add_argument("--interval", type=float, default=1.0, help="心跳间隔秒数")
    args = parser.parse_args()

    for name in ("SIGINT", "SIGTERM", "SIGBREAK"):
        sig = getattr(signal, name, None)
        if sig is None:
            continue
        try:
            signal.signal(sig, _on_signal)
        except (ValueError, OSError):
            pass

    print(f"[tc_long_task] started, pid={os.getpid()}, cwd={os.getcwd()}", flush=True)
    print(f"[tc_long_task] argv={sys.argv[1:]}", flush=True)

    deadline = time.monotonic() + max(1, args.seconds)
    beat = 0
    while not _stop and time.monotonic() < deadline:
        beat += 1
        print(f"[tc_long_task] heartbeat {beat}", flush=True)
        time.sleep(max(0.1, args.interval))

    print(f"[tc_long_task] stopped after {beat} heartbeats", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

在 `esp_custom_tasks.xml` 的 `</esp-tasks>` 之前追加宏与任务（脚本路径用宏复用，写法与其它自定义任务一致；文件中可以有多个匿名 `<profile>`，它们的 env/宏会合并后全局生效）：

```xml
    <!-- 常驻脚本路径（相对项目根目录；进程工作目录即项目根目录） -->
    <profile>
        <macros>
            tc_long_task=tc_long_task.py
        </macros>
    </profile>

    <exec name="TC Long Task" idf-env="true" path="python"
          toolTip="常驻心跳任务：验证异步执行、增量拉取输出与终止">
        <args>${v:tc_long_task} --seconds 300 --interval 1</args>
    </exec>
```

提示：

- `path="python"` 依赖 `idf-env="true"` 注入的 ESP-IDF python 环境；若该机器上 `python` 不在 PATH（常见于只装了 `python3` 的 Linux），改成 `path="python3"` 并去掉 `idf-env="true"` 即可（脚本只用标准库）。
- `--seconds 300` 是自保护：即使忘记终止，任务最多跑 5 分钟自行结束。想缩短可改成 `--seconds 60`。

#### B6.1 用例步骤

| 步骤 | 调用 | 期望 |
|------|------|------|
| 1 | `espidf_list_tasks {projectPath: "<PROJECT_PATH>"}` | 出现 `TC Long Task`，标识形如 `custom:TC-Long-Task` |
| 2 | `espidf_run_task {projectPath: "<PROJECT_PATH>", task: "TC Long Task", async: true}` | `status=running`，取得 `taskId=<M>` |
| 3 | `espidf_fetch_task_output {projectPath: "<PROJECT_PATH>", taskId: <M>, waitSeconds: 5, maxLines: 50}` | `status=running`；`output` 含 `[tc_long_task] heartbeat`（心跳随等待增长） |
| 4 | `espidf_terminate_task {projectPath: "<PROJECT_PATH>", taskId: <M>}` | `status=terminated` |
| 5 | `espidf_fetch_task_output {projectPath: "<PROJECT_PATH>", taskId: <M>, maxLines: 200}` | `status=exited`；`output` 为运行窗口保留的完整输出，含 `[tc_long_task] started`、`[tc_long_task] argv=[...tc_long_task.py...]`、`heartbeat` |
| 6 | `espidf_list_running_tasks {projectPath: "<PROJECT_PATH>", includeFinished: true}` | `<M>` 的 `state=exited`，`outputAvailable=true`，且文本行末尾显示 `输出：输出可用`（本用例不关闭运行窗口） |

判定要点：

- 步骤 3 证明**运行中可增量拉取输出**（心跳行持续出现）；步骤 5 证明**结束后还能拿到完整输出**，且 `argv` 里能看到 `${v:tc_long_task}` 已被替换为脚本路径（宏在执行自定义任务时同样生效）。
- 本用例的前提是**不关闭该任务的运行窗口**：已结束任务的输出以控制台存活为前提，一旦 tab 被关闭，`outputAvailable` 会变为 `false`，`espidf_fetch_task_output` 只能返回退出码与提示（该场景本批次不覆盖）。
- 步骤 5 的 `output` 里第一行应为 `[tc_long_task] started, pid=..., cwd=<PROJECT_PATH>`：**cwd 必须等于项目根目录**，这是"工作目录为当前 IDF 项目根目录"的验证点。
- 在 **Linux / macOS** 上，`output` 还应含脚本的退出提示（形如 `[tc_long_task] signal 15 received, exiting`）与 `[tc_long_task] stopped after N heartbeats`，即脚本是被信号**正常唤醒退出**的。平台对插件使用的 `KillableColoredProcessHandler` 走优雅终止，Unix 下即 SIGTERM（15）；信号号以实际输出为准，不要因为不是 15 就判失败。
- 脚本在 `time.sleep` 中被信号打断后会先执行处理函数，因此退出延迟不超过一个 `--interval`（默认 1 秒）——这就是"可被轻易通过信号退出"的验证点。
- 在 **Windows** 上终止走 `taskkill /f`，脚本可能来不及打印退出信息，因此`signal ... received`/`stopped after` 两行**不作为断言**；只要步骤 5 为 `status=exited`、步骤 6 为 `state=exited` 即通过。
- 若步骤 4 之前任务已自行结束（例如 `--seconds` 设得过小），`terminate` 返回 `not_running` 属可接受结果，此时以 `--seconds 300` 重跑本用例。

---

## 五、收尾与结果回传

1. **清理**：删除或还原 `<PROJECT_PATH>/esp_custom_tasks.xml`（测试前若有备份则恢复），并删除 `<PROJECT_PATH>/tc_long_task.py`。
2. **回传格式**（逐条填写，不要只给结论）：

| 用例 | 结论（通过/失败/跳过） | 关键证据（工具返回的字段或输出片段） |
|------|------------------------|--------------------------------------|
| A1 宏展开 | | 例如 `TC_MACRO_GLOBAL=global-macro-value` |
| A2 环境变量（`${env:}`） | | |
| A3 shell 原生取值 | | |
| A4 `idf-env` 继承 | | |
| A5 自定义 command 构建 | | |
| B1 异步触发 + taskId | | `taskId=<N>` |
| B2 执行情况列表 | | `state`、计数 |
| B3 中途增量输出 | | `status`、输出片段 |
| B4 结束后完整输出 | | `exitCode`、是否含 `Project build complete` |
| B5 已结束记录与终止 | | `not_running` + `exitCode` |
| B6 常驻脚本（增量输出 / 终止 / 结束后完整输出） | | `heartbeat`、`signal ... received`、`cwd=<PROJECT_PATH>`、`status=terminated` |

失败时请附：完整工具调用参数、原始返回（文本 + 结构化字段）、CLion 版本、插件版本、操作系统、项目是否已成功构建过一次。
