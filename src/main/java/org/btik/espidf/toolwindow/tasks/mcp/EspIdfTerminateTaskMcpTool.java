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
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.readLong;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.resolveProject;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.schema;

/**
 * 按 taskId 终止当前项目正在运行的 ESP-IDF 任务（taskId 来自 {@code espidf_list_running_tasks}）。
 * <p>
 * 已结束的任务（记录保留在列表中）无需也不能终止，此时返回 {@code status=not_running}，
 * 其结果可通过 {@code espidf_fetch_task_output} 读取。
 */
public class EspIdfTerminateTaskMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    private final McpToolDescriptor descriptor;

    public EspIdfTerminateTaskMcpTool() {
        String projectPathParam = Schema_utilKt.getProjectPathParameterName();
        McpToolSchema inputSchema = schema()
                .integer("taskId", $i18n("espidf.mcp.terminate.task.param.taskId"))
                .string(projectPathParam, $i18n("espidf.mcp.common.param.projectPath"))
                .required("taskId", projectPathParam)
                .build();

        McpToolSchema outputSchema = schema()
                .string("status", $i18n("espidf.mcp.terminate.task.field.status"))
                .integer("taskId", $i18n("espidf.mcp.terminate.task.field.taskId"))
                .integer("exitCode", $i18n("espidf.mcp.terminate.task.field.exitCode"))
                .build();

        this.descriptor = new McpToolDescriptor(
                "espidf_terminate_task",
                $i18n("espidf.mcp.terminate.task.name"),
                $i18n("espidf.mcp.terminate.task.desc"),
                CATEGORY,
                "espidf_terminate_task",
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
            return McpToolCallResult.Companion.error($i18n("espidf.mcp.terminate.task.missing.param"), EMPTY_JSON);
        }
        try {
            CmdTaskManager.CmdTask task = CmdTaskManager.findTask(project, taskId);
            if (task == null) {
                return McpToolCallResult.Companion.error($i18nF("espidf.mcp.terminate.task.not.found", taskId),
                        structuredResult("not_found", taskId));
            }
            // 已结束的任务保留在记录里，只能查询结果，不能再终止
            if (!task.isAlive()) {
                return McpToolCallResult.Companion.error(
                        $i18nF("espidf.mcp.terminate.task.not.running", task.name(), taskId, task.exitCode()),
                        structuredResult("not_running", taskId, task.exitCode()));
            }
            CmdTaskManager.terminate(project, taskId);
            return McpToolCallResult.Companion.text($i18nF("espidf.mcp.terminate.task.terminated", task.name(), taskId),
                    structuredResult("terminated", taskId));
        } catch (Throwable e) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.terminate.task.failed", e.getMessage()), EMPTY_JSON);
        }
    }

    private static JsonObject structuredResult(@NotNull String status, long taskId) {
        return structuredResult(status, taskId, null);
    }

    private static JsonObject structuredResult(@NotNull String status, long taskId, @Nullable Integer exitCode) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("status", JsonElementKt.JsonPrimitive(status));
        m.put("taskId", JsonElementKt.JsonPrimitive(taskId));
        if (exitCode != null) {
            m.put("exitCode", JsonElementKt.JsonPrimitive(exitCode));
        }
        return new JsonObject(m);
    }
}
