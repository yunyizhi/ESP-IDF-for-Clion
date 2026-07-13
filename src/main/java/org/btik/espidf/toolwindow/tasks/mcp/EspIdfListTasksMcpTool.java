package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpCallInfoKt;
import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.impl.util.Schema_utilKt;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCategory;
import com.intellij.mcpserver.McpToolDescriptor;
import com.intellij.mcpserver.McpToolSchema;
import com.intellij.openapi.project.Project;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * 列出所有可被 MCP 调用的 ESP-IDF 任务（不含终端/TUI 类型），
 * 返回每个任务的标识、显示名与描述，供 {@code espidf_run_task} 选用。
 */
public class EspIdfListTasksMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    private static final JsonObject EMPTY_JSON = new JsonObject(new LinkedHashMap<>());

    private final EspIdfTasksMcpRegistry registry;
    private final McpToolDescriptor descriptor;

    public EspIdfListTasksMcpTool(@NotNull EspIdfTasksMcpRegistry registry) {
        this.registry = registry;
        String projectPathParam = Schema_utilKt.getProjectPathParameterName();
        Map<String, JsonElement> properties = new LinkedHashMap<>();
        properties.put(projectPathParam, stringProperty($i18n("espidf.mcp.common.param.projectPath")));
        McpToolSchema schema = McpToolSchema.Companion.ofPropertiesMap(
                properties, Set.of(projectPathParam), new LinkedHashMap<>(), McpToolSchema.DEFAULT_DEFINITIONS_PATH);
        this.descriptor = new McpToolDescriptor(
                "espidf_list_tasks",
                $i18n("espidf.mcp.list.tasks.name"),
                $i18n("espidf.mcp.list.tasks.desc"),
                CATEGORY,
                "espidf_list_tasks",
                schema,
                schema,
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

    private static @Nullable Project resolveProject(@Nullable Continuation<? super McpToolCallResult> continuation) {
        return McpCallInfoKt.getProjectOrNull(
                continuation != null ? continuation.getContext()
                        : kotlin.coroutines.EmptyCoroutineContext.INSTANCE);
    }

    private static JsonElement stringProperty(String description) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("type", JsonElementKt.JsonPrimitive("string"));
        m.put("description", JsonElementKt.JsonPrimitive(description));
        return new JsonObject(m);
    }
}
