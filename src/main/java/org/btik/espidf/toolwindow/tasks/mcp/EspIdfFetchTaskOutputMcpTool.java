package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCategory;
import com.intellij.mcpserver.McpToolDescriptor;
import com.intellij.mcpserver.McpToolSchema;
import com.intellij.mcpserver.impl.util.Schema_utilKt;
import com.intellij.openapi.project.Project;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;
import org.btik.espidf.util.CmdTaskManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.EMPTY_JSON;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.readInt;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.readLong;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.resolveProject;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.schema;

/**
 * 拉取「正在运行的 ESP-IDF 任务」的增量输出：只返回本次调用开始到等待结束之间新产生的输出，
 * 最多 {@code maxLines} 行（保留末尾行）。
 * <p>
 * 配合 {@code espidf_run_task}(async=true) 使用：先异步触发长任务拿到 taskId，
 * 再反复调用本工具轮询日志，避免一次性等到任务结束。
 */
public class EspIdfFetchTaskOutputMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    /** 默认等待窗口：30 秒 */
    private static final long DEFAULT_WAIT_MILLIS = 30 * 1000L;

    /** 默认最多返回的输出行数（保留末尾若干行） */
    private static final int DEFAULT_MAX_OUTPUT_LINES = 200;

    /** {@link McpTaskOutputCollector#fetchFrom} 的超时标记 */
    private static final int STILL_RUNNING = -2;

    /** {@link McpTaskOutputCollector#fetchFrom} 的采集异常标记 */
    private static final int COLLECT_FAILED = -3;

    private final McpToolDescriptor descriptor;

    public EspIdfFetchTaskOutputMcpTool() {
        String projectPathParam = Schema_utilKt.getProjectPathParameterName();
        McpToolSchema inputSchema = schema()
                .integer("taskId", $i18n("espidf.mcp.fetch.task.param.taskId"))
                .integer("waitSeconds", $i18n("espidf.mcp.fetch.task.param.waitSeconds"))
                .integer("maxLines", $i18n("espidf.mcp.fetch.task.param.maxLines"), DEFAULT_MAX_OUTPUT_LINES)
                .string(projectPathParam, $i18n("espidf.mcp.common.param.projectPath"))
                .required("taskId", projectPathParam)
                .build();

        McpToolSchema outputSchema = schema()
                .string("status", $i18n("espidf.mcp.fetch.task.field.status"))
                .integer("taskId", $i18n("espidf.mcp.fetch.task.field.taskId"))
                .string("taskName", $i18n("espidf.mcp.fetch.task.field.taskName"))
                .integer("exitCode", $i18n("espidf.mcp.fetch.task.field.exitCode"))
                .string("output", $i18n("espidf.mcp.fetch.task.field.output"))
                .bool("truncated", $i18n("espidf.mcp.fetch.task.field.truncated"))
                .integer("omittedLines", $i18n("espidf.mcp.fetch.task.field.omittedLines"))
                .string("message", $i18n("espidf.mcp.fetch.task.field.message"))
                .build();

        this.descriptor = new McpToolDescriptor(
                "espidf_fetch_task_output",
                $i18n("espidf.mcp.fetch.task.name"),
                $i18n("espidf.mcp.fetch.task.desc"),
                CATEGORY,
                "espidf_fetch_task_output",
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
            return McpToolCallResult.Companion.error($i18n("espidf.mcp.run.task.no.project"), EMPTY_JSON);
        }
        Long taskId = readLong(input, "taskId");
        if (taskId == null) {
            return McpToolCallResult.Companion.error($i18n("espidf.mcp.fetch.task.missing.param"), EMPTY_JSON);
        }
        try {
            CmdTaskManager.ActiveTask task = CmdTaskManager.findTask(project, taskId);
            if (task == null) {
                return McpToolCallResult.Companion.error($i18nF("espidf.mcp.fetch.task.not.found", taskId),
                        structuredResult("not_found", taskId, null, null, null, null));
            }
            ProcessHandler processHandler = task.processHandler();
            // 任务已结束：没有可拉取的增量输出，直接返回结果码
            if (processHandler.isProcessTerminated()) {
                int exitCode = processHandler.getExitCode() == null ? -1 : processHandler.getExitCode();
                return McpToolCallResult.Companion.text(
                        $i18nF("espidf.mcp.fetch.task.finished", task.name(), exitCode),
                        structuredResult("exited", taskId, task.name(), exitCode, OutputView.EMPTY, null));
            }

            Integer waitSeconds = readInt(input, "waitSeconds");
            long waitMillis = waitSeconds != null ? Math.max(0, waitSeconds) * 1000L : DEFAULT_WAIT_MILLIS;
            Integer maxLinesArg = readInt(input, "maxLines");
            int maxLines = maxLinesArg != null ? maxLinesArg : DEFAULT_MAX_OUTPUT_LINES;

            McpTaskOutputCollector.McpTaskOutput out =
                    McpTaskOutputCollector.fetchFrom(processHandler, waitMillis);
            if (out.exitCode == COLLECT_FAILED) {
                String message = $i18nF("espidf.mcp.fetch.task.collect.failed", task.name());
                return McpToolCallResult.Companion.error(message,
                        structuredResult("error", taskId, task.name(), null, null, message));
            }
            OutputView limited = limitLines(out.text, maxLines);
            String body = limited.text().isEmpty() ? $i18n("espidf.mcp.fetch.task.no.new.output") : limited.text();
            if (out.exitCode == STILL_RUNNING) {
                String header = $i18nF("espidf.mcp.fetch.task.running", task.name(), waitMillis / 1000L);
                return McpToolCallResult.Companion.text(header + body,
                        structuredResult("running", taskId, task.name(), null, limited, null));
            }
            String header = $i18nF("espidf.mcp.fetch.task.exited", task.name(), out.exitCode);
            return McpToolCallResult.Companion.text(header + body,
                    structuredResult("exited", taskId, task.name(), out.exitCode, limited, null));
        } catch (Throwable e) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.fetch.task.failed", e.getMessage()), EMPTY_JSON);
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

    private static JsonObject structuredResult(@NotNull String status,
                                               long taskId,
                                               @Nullable String taskName,
                                               @Nullable Integer exitCode,
                                               @Nullable OutputView output,
                                               @Nullable String message) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("status", JsonElementKt.JsonPrimitive(status));
        m.put("taskId", JsonElementKt.JsonPrimitive(taskId));
        if (taskName != null) {
            m.put("taskName", JsonElementKt.JsonPrimitive(taskName));
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
}
