package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.execution.process.ProcessListener;
import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.impl.util.Schema_utilKt;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCategory;
import com.intellij.mcpserver.McpToolDescriptor;
import com.intellij.mcpserver.McpToolSchema;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;
import org.btik.espidf.toolwindow.tasks.TreeNodeCmdExecutor;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskActionNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskConsoleCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskTreeNode;
import org.btik.espidf.toolwindow.tasks.model.LocalExecNode;
import org.btik.espidf.toolwindow.tasks.model.RawCommandNode;
import org.btik.espidf.util.CmdTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.readBool;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.readInt;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.readString;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.resolveProject;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.schema;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 统一的 ESP-IDF 任务执行入口：通过 {@code task} 参数指定任务
 * （id / displayName / 完整路径，大小写不敏感），复用与界面点击相同的执行路径。
 */
public class EspIdfRunTaskMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    private static final long AWAIT_TIMEOUT_MS = 10 * 60 * 1000L;

    /** 默认最多返回的输出行数（保留末尾若干行） */
    private static final int DEFAULT_MAX_OUTPUT_LINES = 200;

    private final EspIdfTasksMcpRegistry registry;
    private final McpToolDescriptor descriptor;

    public EspIdfRunTaskMcpTool(@NotNull EspIdfTasksMcpRegistry registry) {
        this.registry = registry;
        String projectPathParam = Schema_utilKt.getProjectPathParameterName();
        McpToolSchema inputSchema = schema()
                .string("task", $i18n("espidf.mcp.run.task.param.task"))
                .bool("async", $i18n("espidf.mcp.run.task.param.async"))
                .integer("waitSeconds", $i18n("espidf.mcp.run.task.param.waitSeconds"))
                .integer("maxLines", $i18n("espidf.mcp.run.task.param.maxLines"), DEFAULT_MAX_OUTPUT_LINES)
                .string(projectPathParam, $i18n("espidf.mcp.common.param.projectPath"))
                .required("task", projectPathParam)
                .build();
        // 输出结构化字段的 schema：声明 status/taskName/exitCode/output/message，均为可选（required 为空），
        // 便于客户端按字段解析，后续也能平滑加入 truncated 等字段。
        McpToolSchema outputSchema = schema()
                .string("status", $i18n("espidf.mcp.run.task.field.status"))
                .string("taskName", $i18n("espidf.mcp.run.task.field.taskName"))
                .integer("taskId", $i18n("espidf.mcp.run.task.field.taskId"))
                .integer("exitCode", $i18n("espidf.mcp.run.task.field.exitCode"))
                .string("output", $i18n("espidf.mcp.run.task.field.output"))
                .bool("truncated", $i18n("espidf.mcp.run.task.field.truncated"))
                .integer("omittedLines", $i18n("espidf.mcp.run.task.field.omittedLines"))
                .string("message", $i18n("espidf.mcp.run.task.field.message"))
                .build();
        this.descriptor = new McpToolDescriptor(
                "espidf_run_task",
                $i18n("espidf.mcp.run.task.name"),
                $i18n("espidf.mcp.run.task.desc"),
                CATEGORY,
                "espidf_run_task",
                inputSchema,
                outputSchema,
                new ToolAnnotations());
    }

    @Override
    public @NotNull McpToolDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Object call(@NotNull JsonObject input,
                       @Nullable Continuation<? super McpToolCallResult> continuation) {
        Project project = resolveProject(continuation);
        if (project == null) {
            String message = $i18n("espidf.mcp.run.task.no.project");
            return McpToolCallResult.Companion.error(message,
                    structuredResult("error", null, null, null, null, message));
        }
        String task = readString(input, "task");
        if (StringUtils.isEmpty(task)) {
            String message = $i18n("espidf.mcp.run.task.missing.param");
            return McpToolCallResult.Companion.error(message,
                    structuredResult("error", null, null, null, null, message));
        }
        EspIdfTaskTreeNode node = registry.lookup(project, task);
        if (node == null) {
            String message = $i18nF("espidf.mcp.run.task.unknown", task);
            return McpToolCallResult.Companion.error(message,
                    structuredResult("error", null, null, null, null, message));
        }
        try {
            boolean async = readBool(input, "async");
            Integer waitSeconds = readInt(input, "waitSeconds");
            // waitSeconds 为通用的等待超时时间（秒），不再区分是否 monitor 任务；未指定时使用默认超时
            long waitMillis = (waitSeconds != null && waitSeconds > 0)
                    ? waitSeconds * 1000L : AWAIT_TIMEOUT_MS;
            Integer maxLinesArg = readInt(input, "maxLines");
            int maxLines = maxLinesArg != null ? maxLinesArg : DEFAULT_MAX_OUTPUT_LINES;
            boolean capturable = node instanceof EspIdfTaskCommandNode
                    || node instanceof LocalExecNode
                    || node instanceof RawCommandNode;

            // 异步调用（或无法采集输出的任务）：仅触发任务后立即返回，任务在后台继续运行
            if (async || !capturable) {
                Long taskId = startTask(project, node, null);
                String text = $i18nF("espidf.mcp.run.task.triggered", node.getDisplayName())
                        + (taskId != null ? "\n" + $i18nF("espidf.mcp.run.task.triggered.task.id", taskId) : "");
                return McpToolCallResult.Companion.text(text,
                        structuredResult("running", node.getDisplayName(), taskId, null, OutputView.EMPTY, null));
            }

            // 同步调用：注册输出收集器，等待任务结束（最多 waitSeconds 秒）
            String basePath = StringUtils.defaultString(project.getBasePath());
            String id = node.getId();
            String uniqueName = StringUtils.isNotEmpty(id) ? sanitize(id) : sanitize(node.getDisplayName());
            String key = basePath + "::" + uniqueName;
            ProcessListener listener = McpTaskOutputCollector.register(key);
            Long taskId = startTask(project, node, listener);

            McpTaskOutputCollector.McpTaskOutput out = McpTaskOutputCollector.await(key, waitMillis);
            if (out == null) {
                return McpToolCallResult.Companion.text(
                        $i18nF("espidf.mcp.run.task.no.output", node.getDisplayName()),
                        structuredResult("exited", node.getDisplayName(), taskId, -1, OutputView.EMPTY, null));
            }
            // exitCode == -2 表示等待超时，任务仍在后台运行，status 记为 running
            if (out.exitCode == -2) {
                OutputView limited = limitLines(out.text, maxLines);
                String header = $i18nF("espidf.mcp.run.task.timeout",
                        node.getDisplayName(), waitMillis / 1000L);
                return McpToolCallResult.Companion.text(header + limited.text(),
                        structuredResult("running", node.getDisplayName(), taskId, out.exitCode, limited, null));
            }
            OutputView limited = limitLines(out.text, maxLines);
            String header = $i18nF("espidf.mcp.run.task.finished", node.getDisplayName(), out.exitCode);
            return McpToolCallResult.Companion.text(header + limited.text(),
                    structuredResult("exited", node.getDisplayName(), taskId, out.exitCode, limited, null));
        } catch (Throwable e) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.run.task.failed", node.getDisplayName(), e.getMessage()),
                    structuredResult("error", node.getDisplayName(), null, null, null, e.getMessage()));
        }
    }

    /**
     * 在 EDT 上启动任务，并返回本次新登记的任务 id（无法跟踪的任务类型或未启动时返回 {@code null}）。
     * <p>
     * 用 {@code invokeAndWait} 保证在返回前任务已完成登记，因此异步调用也能先拿到 taskId，
     * 供客户端随后用 {@code espidf_list_running_tasks} / {@code espidf_terminate_task} 查询或终止。
     */
    private static @Nullable Long startTask(@NotNull Project project, @NotNull EspIdfTaskTreeNode node,
                                            @Nullable ProcessListener listener) {
        Set<Long> before = new HashSet<>();
        for (CmdTaskManager.ActiveTask task : CmdTaskManager.activeTasks(project)) {
            before.add(task.taskId());
        }
        Runnable start = () -> executeTask(project, node, listener);
        if (ApplicationManager.getApplication().isDispatchThread()) {
            start.run();
        } else {
            ApplicationManager.getApplication().invokeAndWait(start);
        }
        String name = node.getDisplayName();
        return CmdTaskManager.activeTasks(project).stream()
                .filter(task -> !before.contains(task.taskId()))
                .filter(task -> name.equals(task.name()))
                .map(CmdTaskManager.ActiveTask::taskId)
                .findFirst()
                .orElse(null);
    }

    private static void executeTask(Project project, EspIdfTaskTreeNode node, ProcessListener listener) {
        if (node instanceof EspIdfTaskCommandNode n) {
            TreeNodeCmdExecutor.execute(n, project, listener);
        } else if (node instanceof EspIdfTaskConsoleCommandNode n) {
            TreeNodeCmdExecutor.execute(n, project);
        } else if (node instanceof LocalExecNode n) {
            TreeNodeCmdExecutor.execute(n, project, listener);
        } else if (node instanceof RawCommandNode n) {
            TreeNodeCmdExecutor.execute(n, project, listener);
        } else if (node instanceof EspIdfTaskActionNode n) {
            TreeNodeCmdExecutor.execute(n, project);
        }
    }

    /** 输出视图：可能被截断的文本 + 截断信息。 */
    private record OutputView(String text, boolean truncated, int omittedLines) {
        static final OutputView EMPTY = new OutputView("", false, 0);

        static OutputView of(String text) {
            return new OutputView(text == null ? "" : text, false, 0);
        }
    }

    /**
     * 保留输出末尾最多 {@code maxLines} 行：超出部分从开头丢弃并记录，{@code maxLines <= 0} 表示不限制。
     */
    private static OutputView limitLines(String text, int maxLines) {
        if (text == null || text.isEmpty()) {
            return OutputView.EMPTY;
        }
        if (maxLines <= 0) {
            return OutputView.of(text);
        }
        String[] lines = text.split("\n", -1);
        if (lines.length <= maxLines) {
            return OutputView.of(text);
        }
        int omitted = lines.length - maxLines;
        StringBuilder sb = new StringBuilder();
        for (int i = omitted; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) {
                sb.append('\n');
            }
        }
        return new OutputView(sb.toString(), true, omitted);
    }

    /**
     * 构造返回给 MCP 客户端的结构化结果：{@code status} / {@code taskName} / {@code exitCode} /
     * {@code output}（可选 {@code truncated} / {@code omittedLines} / {@code message}），均为可选字段。
     * 客户端可直接按字段解析，无需从自然语言里抠取。
     */
    private static JsonObject structuredResult(@NotNull String status,
                                               @Nullable String taskName,
                                               @Nullable Long taskId,
                                               @Nullable Integer exitCode,
                                               @Nullable OutputView output,
                                               @Nullable String message) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("status", JsonElementKt.JsonPrimitive(status));
        if (taskName != null) {
            m.put("taskName", JsonElementKt.JsonPrimitive(taskName));
        }
        if (taskId != null) {
            m.put("taskId", JsonElementKt.JsonPrimitive(taskId));
        }
        if (exitCode != null) {
            m.put("exitCode", JsonElementKt.JsonPrimitive(exitCode));
        }
        if (output != null) {
            m.put("output", JsonElementKt.JsonPrimitive(output.text()));
            if (output.truncated()) {
                m.put("truncated", JsonElementKt.JsonPrimitive(true));
                m.put("omittedLines", JsonElementKt.JsonPrimitive(output.omittedLines()));
            }
        }
        if (message != null) {
            m.put("message", JsonElementKt.JsonPrimitive(message));
        }
        return new JsonObject(m);
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}
