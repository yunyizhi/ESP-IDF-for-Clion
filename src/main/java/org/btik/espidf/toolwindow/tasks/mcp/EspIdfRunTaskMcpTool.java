package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.execution.process.ProcessListener;
import com.intellij.mcpserver.McpCallInfoKt;
import com.intellij.mcpserver.McpTool;
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
import kotlinx.serialization.json.JsonPrimitive;
import org.btik.espidf.toolwindow.tasks.TreeNodeCmdExecutor;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskActionNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskConsoleCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskTreeNode;
import org.btik.espidf.toolwindow.tasks.model.LocalExecNode;
import org.btik.espidf.toolwindow.tasks.model.RawCommandNode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;

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

    private static final JsonObject EMPTY_JSON = new JsonObject(new LinkedHashMap<>());

    private static final long AWAIT_TIMEOUT_MS = 10 * 60 * 1000L;

    private final EspIdfTasksMcpRegistry registry;
    private final McpToolDescriptor descriptor;

    public EspIdfRunTaskMcpTool(@NotNull EspIdfTasksMcpRegistry registry) {
        this.registry = registry;
        Map<String, JsonElement> properties = new LinkedHashMap<>();
        properties.put("task", stringProperty($i18n("espidf.mcp.run.task.param.task")));
        properties.put("project", stringProperty($i18n("espidf.mcp.run.task.param.project")));
        McpToolSchema inputSchema = McpToolSchema.Companion.ofPropertiesMap(
                properties, Set.of("task"), new LinkedHashMap<>(), McpToolSchema.DEFAULT_DEFINITIONS_PATH);
        // 输出 schema 必须宽松：工具返回的结构化内容（EMPTY_JSON）不含 task 等属性，
        // 若复用含 required 的输入 schema，服务端会对输出做校验并报错。
        McpToolSchema outputSchema = McpToolSchema.Companion.ofPropertiesMap(
                new LinkedHashMap<>(), Set.of(), new LinkedHashMap<>(), McpToolSchema.DEFAULT_DEFINITIONS_PATH);
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

    private static JsonElement stringProperty(String description) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("type", JsonElementKt.JsonPrimitive("string"));
        m.put("description", JsonElementKt.JsonPrimitive(description));
        return new JsonObject(m);
    }

    @Override
    public @NotNull McpToolDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Object call(@NotNull JsonObject input,
                       @Nullable Continuation<? super McpToolCallResult> continuation) {
        Project project = resolveProject(input, continuation);
        if (project == null) {
            String hint = readString(input, "project");
            if (StringUtils.isNotEmpty(hint)) {
                return McpToolCallResult.Companion.error(
                        $i18nF("espidf.mcp.run.task.unknown.project", hint), EMPTY_JSON);
            }
            return McpToolCallResult.Companion.error(
                    $i18n("espidf.mcp.run.task.no.project"), EMPTY_JSON);
        }
        String task = readString(input, "task");
        if (StringUtils.isEmpty(task)) {
            return McpToolCallResult.Companion.error(
                    $i18n("espidf.mcp.run.task.missing.param"), EMPTY_JSON);
        }
        EspIdfTaskTreeNode node = registry.lookup(task);
        if (node == null) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.run.task.unknown", task), EMPTY_JSON);
        }
        try {
            boolean longRunning = node instanceof EspIdfTaskCommandNode
                    && ((EspIdfTaskCommandNode) node).isUseMonitor();
            boolean capturable = node instanceof EspIdfTaskCommandNode
                    || node instanceof LocalExecNode
                    || node instanceof RawCommandNode;

            if (!capturable || longRunning) {
                ApplicationManager.getApplication().invokeLater(() -> executeTask(project, node, null));
                return McpToolCallResult.Companion.text(
                        $i18nF("espidf.mcp.run.task.triggered", node.getDisplayName()),
                        EMPTY_JSON);
            }

            String basePath = StringUtils.defaultString(project.getBasePath());
            String id = node.getId();
            String uniqueName = StringUtils.isNotEmpty(id) ? sanitize(id) : sanitize(node.getDisplayName());
            String key = basePath + "::" + uniqueName;
            ProcessListener listener = McpTaskOutputCollector.register(key);
            ApplicationManager.getApplication().invokeLater(() -> executeTask(project, node, listener));

            McpTaskOutputCollector.McpTaskOutput out = McpTaskOutputCollector.await(key, AWAIT_TIMEOUT_MS);
            if (out == null) {
                return McpToolCallResult.Companion.text(
                        $i18nF("espidf.mcp.run.task.no.output", node.getDisplayName()), EMPTY_JSON);
            }
            String header = $i18nF("espidf.mcp.run.task.finished", node.getDisplayName(), out.exitCode);
            return McpToolCallResult.Companion.text(header + out.text, EMPTY_JSON);
        } catch (Throwable e) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.run.task.failed", node.getDisplayName(), e.getMessage()), EMPTY_JSON);
        }
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
