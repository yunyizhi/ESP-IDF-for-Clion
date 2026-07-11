package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCategory;
import com.intellij.mcpserver.McpToolDescriptor;
import com.intellij.mcpserver.McpToolSchema;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
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
        McpToolSchema schema = McpToolSchema.Companion.ofPropertiesMap(
                new LinkedHashMap<>(), Set.of(), new LinkedHashMap<>(), McpToolSchema.DEFAULT_DEFINITIONS_PATH);
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
        StringBuilder sb = new StringBuilder();
        sb.append($i18n("espidf.mcp.list.tasks.header")).append("\n\n");
        for (EspIdfTasksMcpRegistry.Entry e : registry.getEntries()) {
            sb.append("- ").append(e.id());
            if (!e.displayName().equalsIgnoreCase(e.id())) {
                sb.append("  (").append($i18n("espidf.mcp.list.tasks.display")).append(' ')
                        .append(e.displayName()).append(')');
            }
            sb.append('\n');
            sb.append("    ").append(e.description()).append('\n');
        }
        if (registry.getEntries().isEmpty()) {
            sb.append($i18n("espidf.mcp.list.tasks.empty")).append('\n');
        }
        return McpToolCallResult.Companion.text(sb.toString(), EMPTY_JSON);
    }
}
