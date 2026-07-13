package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.openapi.project.Project;
import org.btik.espidf.toolwindow.tasks.EspIdfTaskTreeFactory;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskActionNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskConsoleCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskFolderNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskTreeNode;
import org.btik.espidf.toolwindow.tasks.model.LocalExecNode;
import org.btik.espidf.toolwindow.tasks.model.RawCommandNode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 加载 ESP-IDF 默认任务树（来自 defaultTree.xml），构建「任务标识 -> 任务节点」的注册表，
 * 供 MCP 的 {@code espidf_run_task} / {@code espidf_list_tasks} 两个入口复用，
 * 从而替代原先每个任务一个 MCP 工具的做法。
 *
 * <p>自定义任务（项目级 esp_custom_tasks.xml）不存放于此处的共享实例，
 * 而是由 {@link EspIdfCustomTasksService} 按项目隔离缓存；本类在查询时向其委托，
 * 以保证不同项目之间的自定义任务互不干扰。
 */
public final class EspIdfTasksMcpRegistry {

    /** 一个可被 MCP 调用的任务条目的描述，用于列表展示。 */
    public record Entry(
            @NotNull String id,
            @NotNull String displayName,
            @NotNull String description,
            @NotNull String path,
            boolean useMonitor) {
    }

    /** 默认任务树（来自 defaultTree.xml），构建一次后不可变。 */
    private final Map<String, EspIdfTaskTreeNode> registry = new HashMap<>();
    private final List<Entry> entries = new ArrayList<>();

    private EspIdfTasksMcpRegistry() {
    }

    public static @NotNull EspIdfTasksMcpRegistry build() {
        EspIdfTasksMcpRegistry r = new EspIdfTasksMcpRegistry();
        DefaultMutableTreeNode root = EspIdfTaskTreeFactory.load();
        if (root != null) {
            collect(root, "", new StringBuilder(), r.registry, r.entries);
        }
        return r;
    }

    /**
     * 遍历任务树，将可执行且允许暴露给 MCP 的节点登记进目标集合。
     * {@code keyPrefix} 用于为某一类任务（如自定义任务）统一加标识前缀，避免与内置任务冲突。
     */
    public static void collect(@NotNull DefaultMutableTreeNode node,
                               @NotNull String keyPrefix,
                               @NotNull StringBuilder pathBuilder,
                               @NotNull Map<String, EspIdfTaskTreeNode> targetRegistry,
                               @NotNull List<Entry> targetEntries) {
        int childCount = node.getChildCount();
        if (childCount == 0) {
            Object userObject = node.getUserObject();
            if (userObject instanceof EspIdfTaskTreeNode taskNode
                    && isExecutable(taskNode) && !isTerminalTask(taskNode)
                    && taskNode.isMcp()) {
                register(taskNode, pathBuilder, keyPrefix, targetRegistry, targetEntries);
            }
            return;
        }
        for (int i = 0; i < childCount; i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object userObject = child.getUserObject();
            if (userObject instanceof EspIdfTaskTreeNode childNode) {
                // mcp="false" 的节点（含其子树）不暴露给 MCP
                if (!childNode.isMcp()) {
                    continue;
                }
                int start = pathBuilder.length();
                if (start > 0) {
                    pathBuilder.append('/');
                }
                pathBuilder.append(childNode.getDisplayName());
                collect(child, keyPrefix, pathBuilder, targetRegistry, targetEntries);
                pathBuilder.setLength(start);
            } else {
                collect(child, keyPrefix, pathBuilder, targetRegistry, targetEntries);
            }
        }
    }

    private static void register(@NotNull EspIdfTaskTreeNode node,
                                 @NotNull StringBuilder pathBuilder,
                                 @NotNull String keyPrefix,
                                 @NotNull Map<String, EspIdfTaskTreeNode> targetRegistry,
                                 @NotNull List<Entry> targetEntries) {
        String path = pathBuilder.toString();
        String id = node.getId();
        // 自定义任务（来自 esp_custom_tasks.xml）的 XSD 没有 id 属性，仅 name 唯一，
        // 因此 id 为空时改用 name（displayName）派生标识；空白字符替换为 '-'，其余非安全字符仍为 '_'。
        String baseKey = StringUtils.isNotEmpty(id) ? sanitize(id) : sanitizeName(node.getDisplayName());
        String key = keyPrefix + baseKey;
        String lowerKey = key.toLowerCase(Locale.ROOT);
        targetRegistry.put(lowerKey, node);
        // 同时接受 displayName 与完整路径作为别名，方便调用方传参（同样加前缀以隔离）
        targetRegistry.putIfAbsent(keyPrefix + node.getDisplayName().toLowerCase(Locale.ROOT), node);
        if (!path.isEmpty()) {
            targetRegistry.putIfAbsent(keyPrefix + path.toLowerCase(Locale.ROOT), node);
        }
        targetEntries.add(new Entry(key, node.getDisplayName(), describe(node), path, isUseMonitor(node)));
    }

    /**
     * 判断节点是否为「使用 monitor（串口监视）」的任务（对应 XML 中的 use-monitor 属性）。
     * 这类任务通常长时间运行，无法像普通命令那样等待其结束；
     * 调用方可通过 {@code espidf_run_task} 的 {@code monitorWaitSeconds} 参数采集一段时间的日志。
     */
    public static boolean isUseMonitor(@Nullable EspIdfTaskTreeNode node) {
        return node instanceof EspIdfTaskCommandNode
                && ((EspIdfTaskCommandNode) node).isUseMonitor();
    }

    /**
     * 按任务标识查找节点。可接受任务 id、displayName 或完整路径（大小写不敏感）。
     * 先查项目级自定义任务，再回退到默认任务树。
     */
    public @Nullable EspIdfTaskTreeNode lookup(@Nullable Project project, @Nullable String key) {
        if (key == null) {
            return null;
        }
        if (project != null) {
            EspIdfTaskTreeNode node = EspIdfCustomTasksService.getInstance(project).lookup(key);
            if (node != null) {
                return node;
            }
        }
        return registry.get(key.toLowerCase(Locale.ROOT));
    }

    /**
     * 返回合并后的任务条目列表：默认任务打底，项目级自定义任务（带 {@code c.} 前缀）覆盖同标识的默认任务。
     */
    public @NotNull List<Entry> getEntries(@Nullable Project project) {
        List<Entry> custom = project != null
                ? EspIdfCustomTasksService.getInstance(project).getEntries()
                : List.of();
        if (custom.isEmpty()) {
            return entries;
        }
        Map<String, Entry> merged = new LinkedHashMap<>();
        for (Entry e : entries) {
            merged.put(e.id(), e);
        }
        for (Entry e : custom) {
            merged.put(e.id(), e);
        }
        return new ArrayList<>(merged.values());
    }

    private static boolean isExecutable(EspIdfTaskTreeNode node) {
        return node instanceof EspIdfTaskCommandNode
                || node instanceof EspIdfTaskConsoleCommandNode
                || node instanceof LocalExecNode
                || node instanceof RawCommandNode
                || node instanceof EspIdfTaskActionNode
                || node instanceof EspIdfTaskFolderNode;
    }

    /**
     * 终端/TUI 类型任务需在 IDE 内置终端中交互执行，
     * 无法在无头 MCP 环境里正确运行，故不暴露为 MCP 工具。
     */
    private static boolean isTerminalTask(EspIdfTaskTreeNode node) {
        if (node instanceof LocalExecNode n) {
            return n.isUseTerminal();
        }
        return node instanceof EspIdfTaskConsoleCommandNode;
    }

    private static String describe(EspIdfTaskTreeNode node) {
        String tip = node.getToolTip();
        if (StringUtils.isNotEmpty(tip)) {
            return "Execute the ESP-IDF task '" + node.getDisplayName() + "'. " + tip;
        }
        return "Execute the ESP-IDF task '" + node.getDisplayName() +
                "' defined in the ESP-IDF task tree.";
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
        return sb.toString();
    }

    /**
     * 用于自定义任务：XSD 无 id 属性，以 name 作为唯一标识。
     * 与 {@link #sanitize} 不同，空白字符（空格/制表/换行等）替换为 '-'，便于阅读。
     */
    private static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isWhitespace(c)) {
                sb.append('-');
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }
}
