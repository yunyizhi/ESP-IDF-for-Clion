package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpCallInfoKt;
import com.intellij.mcpserver.McpTool;
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
import kotlinx.serialization.json.JsonPrimitive;
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
        Map<String, JsonElement> properties = new LinkedHashMap<>();
        properties.put("project", stringProperty($i18n("espidf.mcp.list.tasks.param.project")));
        McpToolSchema schema = McpToolSchema.Companion.ofPropertiesMap(
                properties, Set.of(), new LinkedHashMap<>(), McpToolSchema.DEFAULT_DEFINITIONS_PATH);
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
        Project project = resolveProject(input, continuation);
        StringBuilder sb = new StringBuilder();
        sb.append($i18n("espidf.mcp.list.tasks.header")).append("\n\n");
        for (EspIdfTasksMcpRegistry.Entry e : registry.getEntries(project)) {
            sb.append("- ").append(e.id());
            if (!e.displayName().equalsIgnoreCase(e.id())) {
                sb.append("  (").append($i18n("espidf.mcp.list.tasks.display")).append(' ')
                        .append(e.displayName()).append(')');
            }
            sb.append('\n');
            sb.append("    ").append(e.description()).append('\n');
        }
        if (registry.getEntries(project).isEmpty()) {
            sb.append($i18n("espidf.mcp.list.tasks.empty")).append('\n');
        }
        return McpToolCallResult.Companion.text(sb.toString(), EMPTY_JSON);
    }

    private static @Nullable Project resolveProject(@NotNull JsonObject input,
                                                    @Nullable Continuation<? super McpToolCallResult> continuation) {
        String hint = readString(input, "project");
        Project contextProject = McpCallInfoKt.getProjectOrNull(
                continuation != null ? continuation.getContext()
                        : kotlin.coroutines.EmptyCoroutineContext.INSTANCE);
        return McpProjectResolver.resolve(hint, contextProject);
    }

    private static @Nullable String readString(@NotNull JsonObject input, @NotNull String key) {
        JsonElement e = input.get(key);
        if (!(e instanceof JsonPrimitive p) || !p.isString()) {
            return null;
        }
        return p.getContent();
    }

    private static JsonElement stringProperty(String description) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("type", JsonElementKt.JsonPrimitive("string"));
        m.put("description", JsonElementKt.JsonPrimitive(description));
        return new JsonObject(m);
    }
}
