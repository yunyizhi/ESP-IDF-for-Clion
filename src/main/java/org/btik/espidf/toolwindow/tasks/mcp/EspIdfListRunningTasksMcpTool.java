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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.EMPTY_JSON;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.resolveProject;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.schema;

/**
 * 列出当前项目的运行中任务（经 {@link CmdTaskManager} 启动的非终端任务），
 * 供 {@code espidf_terminate_task} 按 taskId 终止。
 */
public class EspIdfListRunningTasksMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    private final McpToolDescriptor descriptor;

    public EspIdfListRunningTasksMcpTool() {
        String projectPathParam = Schema_utilKt.getProjectPathParameterName();
        McpToolSchema inputSchema = schema()
                .string(projectPathParam, $i18n("espidf.mcp.common.param.projectPath"))
                .required(projectPathParam)
                .build();

        McpToolSchema outputSchema = schema()
                .integer("count", $i18n("espidf.mcp.list.running.tasks.field.count"))
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
            List<CmdTaskManager.ActiveTask> tasks = CmdTaskManager.activeTasks(project);
            long now = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder();
            sb.append($i18n("espidf.mcp.list.running.tasks.header")).append('\n');
            List<JsonElement> items = new ArrayList<>(tasks.size());
            for (CmdTaskManager.ActiveTask task : tasks) {
                long runningMillis = now - task.startTimeMillis();
                sb.append("- [").append(task.taskId()).append("] ").append(task.name())
                        .append("  (").append(runningMillis / 1000L).append("s)\n");
                Map<String, JsonElement> item = new LinkedHashMap<>();
                item.put("taskId", JsonElementKt.JsonPrimitive(task.taskId()));
                item.put("name", JsonElementKt.JsonPrimitive(task.name()));
                item.put("executionId", JsonElementKt.JsonPrimitive(task.executionId()));
                item.put("startTimeMillis", JsonElementKt.JsonPrimitive(task.startTimeMillis()));
                item.put("runningMillis", JsonElementKt.JsonPrimitive(runningMillis));
                items.add(new JsonObject(item));
            }
            if (tasks.isEmpty()) {
                sb.append($i18n("espidf.mcp.list.running.tasks.empty")).append('\n');
            }
            Map<String, JsonElement> content = new LinkedHashMap<>();
            content.put("count", JsonElementKt.JsonPrimitive(tasks.size()));
            content.put("tasks", new JsonArray(items));
            return McpToolCallResult.Companion.text(sb.toString(), new JsonObject(content));
        } catch (Throwable e) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.list.running.tasks.failed", e.getMessage()), EMPTY_JSON);
        }
    }

}
