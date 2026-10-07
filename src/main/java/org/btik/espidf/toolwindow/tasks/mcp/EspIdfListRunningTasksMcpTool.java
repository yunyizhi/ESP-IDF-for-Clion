package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCategory;
import com.intellij.mcpserver.McpToolDescriptor;
import com.intellij.mcpserver.McpToolSchema;
import com.intellij.mcpserver.impl.util.Schema_utilKt;
import com.intellij.openapi.project.Project;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonArray;
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;
import org.btik.espidf.util.CmdTaskManager;
import org.btik.espidf.util.RunContentOutputs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.EMPTY_JSON;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.readBool;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.resolveProject;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.schema;

/**
 * 列出当前项目的 ESP-IDF 任务执行情况（经 {@link CmdTaskManager} 启动的非终端任务）：
 * 既包含仍在运行的任务，也包含已经结束（或未能启动）的任务记录及退出码（工具 id 保持不变）。
 * <p>
 * 运行中任务的 {@code taskId} 可交给 {@code espidf_terminate_task} 终止或
 * {@code espidf_fetch_task_output} 拉取增量输出；已结束任务可用
 * {@code espidf_fetch_task_output} 读取运行窗口中保留的完整输出。
 */
public class EspIdfListRunningTasksMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    private final McpToolDescriptor descriptor;

    public EspIdfListRunningTasksMcpTool() {
        String projectPathParam = Schema_utilKt.getProjectPathParameterName();
        McpToolSchema inputSchema = schema()
                .bool("includeFinished", $i18n("espidf.mcp.list.running.tasks.param.includeFinished"), true)
                .string(projectPathParam, $i18n("espidf.mcp.common.param.projectPath"))
                .required(projectPathParam)
                .build();

        McpToolSchema outputSchema = schema()
                .integer("count", $i18n("espidf.mcp.list.running.tasks.field.count"))
                .integer("runningCount", $i18n("espidf.mcp.list.running.tasks.field.runningCount"))
                .integer("finishedCount", $i18n("espidf.mcp.list.running.tasks.field.finishedCount"))
                .array("tasks", $i18n("espidf.mcp.list.running.tasks.field.tasks"))
                .build();

        this.descriptor = new McpToolDescriptor(
                "espidf_list_running_tasks",
                $i18n("espidf.mcp.list.running.tasks.name"),
                $i18n("espidf.mcp.list.running.tasks.desc"),
                CATEGORY,
                "espidf_list_running_tasks",
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
        try {
            // 参数缺省视为 true：默认语义是「查看执行情况」，包含已结束的记录
            boolean includeFinished = !input.containsKey("includeFinished") || readBool(input, "includeFinished");
            List<CmdTaskManager.CmdTask> all = CmdTaskManager.tasks(project);
            List<CmdTaskManager.CmdTask> tasks = includeFinished
                    ? all
                    : all.stream().filter(CmdTaskManager.CmdTask::isAlive).toList();

            StringBuilder sb = new StringBuilder();
            sb.append($i18n("espidf.mcp.list.running.tasks.header")).append('\n');
            List<JsonElement> items = new ArrayList<>(tasks.size());
            int runningCount = 0;
            for (CmdTaskManager.CmdTask task : tasks) {
                boolean running = task.isAlive();
                if (running) {
                    runningCount++;
                }
                sb.append(taskLine(task, running)).append('\n');
                items.add(taskItem(task, running));
            }
            if (tasks.isEmpty()) {
                sb.append($i18n(includeFinished
                        ? "espidf.mcp.list.running.tasks.empty"
                        : "espidf.mcp.list.running.tasks.empty.running")).append('\n');
            }
            Map<String, JsonElement> content = new LinkedHashMap<>();
            content.put("count", JsonElementKt.JsonPrimitive(tasks.size()));
            content.put("runningCount", JsonElementKt.JsonPrimitive(runningCount));
            content.put("finishedCount", JsonElementKt.JsonPrimitive(tasks.size() - runningCount));
            content.put("tasks", new JsonArray(items));
            return McpToolCallResult.Companion.text(sb.toString(), new JsonObject(content));
        } catch (Throwable e) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.list.running.tasks.failed", e.getMessage()), EMPTY_JSON);
        }
    }

    /** 文本行：运行中标注 running，已结束标注状态、退出码与耗时；两者都带上输出可用性。 */
    private static String taskLine(@NotNull CmdTaskManager.CmdTask task, boolean running) {
        long seconds = task.durationMillis() / 1000L;
        String output = outputState(RunContentOutputs.isOutputAvailable(task.descriptor()));
        if (running) {
            return $i18nF("espidf.mcp.list.running.tasks.item.running",
                    task.taskId(), task.name(), seconds, output);
        }
        return $i18nF("espidf.mcp.list.running.tasks.item.finished", task.taskId(), task.name(),
                stateName(task.state()), task.exitCode(), seconds, output);
    }

    /**
     * 输出可用性的文本标记：结构化字段只在 JSON 里，只读文本的调用方会误以为输出一定取得到，
     * 因此文本行同样标注（已结束任务的输出以运行窗口控制台存活为前提）。
     */
    private static String outputState(boolean available) {
        return $i18n(available
                ? "espidf.mcp.list.running.tasks.output.available"
                : "espidf.mcp.list.running.tasks.output.unavailable");
    }

    private static JsonObject taskItem(@NotNull CmdTaskManager.CmdTask task, boolean running) {
        Map<String, JsonElement> item = new LinkedHashMap<>();
        item.put("taskId", JsonElementKt.JsonPrimitive(task.taskId()));
        item.put("name", JsonElementKt.JsonPrimitive(task.name()));
        item.put("state", JsonElementKt.JsonPrimitive(stateName(task.state())));
        item.put("executionId", JsonElementKt.JsonPrimitive(task.executionId()));
        item.put("startTimeMillis", JsonElementKt.JsonPrimitive(task.startTimeMillis()));
        item.put("durationMillis", JsonElementKt.JsonPrimitive(task.durationMillis()));
        if (!running) {
            item.put("exitCode", JsonElementKt.JsonPrimitive(task.exitCode()));
        }
        // 输出能否读取取决于运行窗口内容是否还在：已结束任务的输出以控制台存活为前提
        item.put("outputAvailable", JsonElementKt.JsonPrimitive(
                RunContentOutputs.isOutputAvailable(task.descriptor())));
        return new JsonObject(item);
    }

    private static String stateName(@NotNull CmdTaskManager.TaskState state) {
        return state.name().toLowerCase(Locale.ROOT);
    }
}
