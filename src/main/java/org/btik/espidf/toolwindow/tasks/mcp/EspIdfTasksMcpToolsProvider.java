package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.McpToolsProvider;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 通过 JetBrains MCP 扩展点 {@code mcpServer.mcpToolsProvider} 动态暴露
 * ESP-IDF 任务树上的所有可执行任务。
 */
public class EspIdfTasksMcpToolsProvider implements McpToolsProvider {

    private static final Logger LOG = Logger.getInstance(EspIdfTasksMcpToolsProvider.class);

    private static volatile List<McpTool> cachedTools;

    @Override
    public @NotNull List<McpTool> getTools() {
        List<McpTool> tools = cachedTools;
        if (tools == null) {
            synchronized (EspIdfTasksMcpToolsProvider.class) {
                tools = cachedTools;
                if (tools == null) {
                    try {
                        tools = buildTools();
                        LOG.info("EspIdfTasksMcpToolsProvider built " + tools.size() + " MCP tools");
                    } catch (Throwable t) {
                        LOG.error("EspIdfTasksMcpToolsProvider.getTools failed", t);
                        tools = List.of();
                    }
                    cachedTools = tools;
                }
            }
        }
        return tools;
    }

    private static @NotNull List<McpTool> buildTools() {
        EspIdfTasksMcpRegistry registry = EspIdfTasksMcpRegistry.build();
        return List.of(
                new EspIdfListTasksMcpTool(registry),
                new EspIdfRunTaskMcpTool(registry),
                new EspIdfProjectInfoMcpTool(),
                new EspIdfSerialPortsMcpTool());
    }
}
