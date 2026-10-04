package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.impl.util.Schema_utilKt;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCategory;
import com.intellij.mcpserver.McpToolDescriptor;
import com.intellij.mcpserver.McpToolSchema;
import com.intellij.openapi.project.Project;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.EMPTY_JSON;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.resolveProject;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.schema;

/**
 * 列出所有可被 MCP 调用的 ESP-IDF 任务（不含终端/TUI 类型），
 * 返回每个任务的标识、显示名与描述，供 {@code espidf_run_task} 选用。
 */
public class EspIdfListTasksMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    private final EspIdfTasksMcpRegistry registry;
    private final McpToolDescriptor descriptor;

    public EspIdfListTasksMcpTool(@NotNull EspIdfTasksMcpRegistry registry) {
        this.registry = registry;
        String projectPathParam = Schema_utilKt.getProjectPathParameterName();
        McpToolSchema toolSchema = schema()
                .string(projectPathParam, $i18n("espidf.mcp.common.param.projectPath"))
                .required(projectPathParam)
                .build();
        this.descriptor = new McpToolDescriptor(
                "espidf_list_tasks",
                $i18n("espidf.mcp.list.tasks.name"),
                $i18n("espidf.mcp.list.tasks.desc"),
                CATEGORY,
                "espidf_list_tasks",
                toolSchema,
                toolSchema,
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
            return McpToolCallResult.Companion.error(
                    $i18n("espidf.mcp.list.tasks.no.project"), EMPTY_JSON);
        }
        StringBuilder sb = new StringBuilder();
        sb.append($i18n("espidf.mcp.list.tasks.header")).append("\n\n");
        for (EspIdfTasksMcpRegistry.Entry e : registry.getEntries(project)) {
            sb.append("- ").append(e.id());
            if (!e.displayName().equalsIgnoreCase(e.id())) {
                sb.append("  (").append($i18n("espidf.mcp.list.tasks.display")).append(' ')
                        .append(e.displayName()).append(')');
            }
            if (e.useMonitor()) {
                sb.append("  [").append($i18n("espidf.mcp.list.tasks.monitor.hint")).append(']');
            }
            sb.append('\n');
            sb.append("    ").append(e.description()).append('\n');
        }
        if (registry.getEntries(project).isEmpty()) {
            sb.append($i18n("espidf.mcp.list.tasks.empty")).append('\n');
        }
        return McpToolCallResult.Companion.text(sb.toString(), EMPTY_JSON);
    }
}
