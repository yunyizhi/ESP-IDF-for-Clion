package org.btik.espidf.toolwindow.tasks.mcp;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 加载 ESP-IDF 任务树，构建「任务标识 -> 任务节点」的注册表，
 * 供 MCP 的 {@code espidf_run_task} / {@code espidf_list_tasks} 两个入口复用，
 * 从而替代原先每个任务一个 MCP 工具的做法。
 */
public final class EspIdfTasksMcpRegistry {

    /** 一个可被 MCP 调用的任务条目的描述，用于列表展示。 */
    public record Entry(
            @NotNull String id,
            @NotNull String displayName,
            @NotNull String description,
            @NotNull String path) {
    }

    private final Map<String, EspIdfTaskTreeNode> registry = new HashMap<>();
    private final List<Entry> entries = new ArrayList<>();

    private EspIdfTasksMcpRegistry() {
    }

    public static @NotNull EspIdfTasksMcpRegistry build() {
        EspIdfTasksMcpRegistry r = new EspIdfTasksMcpRegistry();
        DefaultMutableTreeNode root = EspIdfTaskTreeFactory.load();
        if (root != null) {
            r.walk(root, new StringBuilder());
        }
        return r;
    }

    private void walk(@NotNull DefaultMutableTreeNode node, @NotNull StringBuilder pathBuilder) {
        int childCount = node.getChildCount();
        if (childCount == 0) {
            Object userObject = node.getUserObject();
            if (userObject instanceof EspIdfTaskTreeNode taskNode
                    && isExecutable(taskNode) && !isTerminalTask(taskNode)) {
                register(taskNode, pathBuilder);
            }
            return;
        }
        for (int i = 0; i < childCount; i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object userObject = child.getUserObject();
            if (userObject instanceof EspIdfTaskTreeNode childNode) {
                int start = pathBuilder.length();
                if (start > 0) {
                    pathBuilder.append('/');
                }
                pathBuilder.append(childNode.getDisplayName());
                walk(child, pathBuilder);
                pathBuilder.setLength(start);
            } else {
                walk(child, pathBuilder);
            }
        }
    }

    private void register(@NotNull EspIdfTaskTreeNode node, @NotNull StringBuilder pathBuilder) {
        String path = pathBuilder.toString();
        String id = node.getId();
        String key = StringUtils.isNotEmpty(id) ? sanitize(id) : sanitize(path);
        String lowerKey = key.toLowerCase(Locale.ROOT);
        registry.put(lowerKey, node);
        // 同时接受 displayName 与完整路径作为别名，方便调用方传参
        registry.putIfAbsent(node.getDisplayName().toLowerCase(Locale.ROOT), node);
        if (!path.isEmpty()) {
            registry.putIfAbsent(path.toLowerCase(Locale.ROOT), node);
        }
        entries.add(new Entry(key, node.getDisplayName(), describe(node), path));
    }

    /**
     * 按任务标识查找节点。可接受任务 id、displayName 或完整路径（大小写不敏感）。
     */
    public @Nullable EspIdfTaskTreeNode lookup(@Nullable String key) {
        if (key == null) {
            return null;
        }
        return registry.get(key.toLowerCase(Locale.ROOT));
    }

    public @NotNull List<Entry> getEntries() {
        return entries;
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
}
