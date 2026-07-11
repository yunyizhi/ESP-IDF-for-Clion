package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 在 MCP 调用上下文中解析目标项目。
 *
 * <p>JetBrains MCP 服务端默认通过 {@code IJ_MCP_SERVER_PROJECT_PATH} 请求头把请求路由到
 * 具体项目；但 ESP-IDF 的任务定义来自插件内置资源（跨项目共享），只有执行环节才依赖
 * 具体项目。因此允许调用方通过参数传入项目路径或名称，由本类从已打开项目中解析出
 * 真实 {@link Project}，从而无需在客户端（如 mcp.json）配置项目路由请求头。</p>
 */
public final class McpProjectResolver {

    private McpProjectResolver() {
    }

    /**
     * 根据线索（项目基路径或名称）从已打开项目中解析目标 {@link Project}。
     *
     * @param hint 项目基路径或名称；为空时返回 {@code null}，交由调用方回退到上下文项目
     * @return 匹配到的项目，未匹配到返回 {@code null}
     */
    public static @Nullable Project resolve(@Nullable String hint) {
        if (StringUtils.isEmpty(hint)) {
            return null;
        }
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        Project byExactPath = null;
        Project byPathSuffix = null;
        Project byName = null;
        for (Project p : openProjects) {
            String base = p.getBasePath();
            if (base != null) {
                if (base.equals(hint)) {
                    byExactPath = p;
                } else if (base.endsWith(hint) || hint.endsWith(base)) {
                    byPathSuffix = p;
                }
            }
            if (p.getName().equals(hint)) {
                byName = p;
            }
        }
        if (byExactPath != null) {
            return byExactPath;
        }
        if (byName != null) {
            return byName;
        }
        return byPathSuffix;
    }

    /**
     * 把调用方传入的线索与上下文项目合并解析：优先用线索匹配已打开项目，
     * 未提供线索或未匹配到时回退到 MCP 调用上下文提供的项目。
     */
    public static @Nullable Project resolve(@Nullable String hint, @Nullable Project contextProject) {
        Project resolved = resolve(hint);
        if (resolved != null) {
            return resolved;
        }
        return contextProject;
    }

    public static @NotNull String describe(@NotNull Project project) {
        String base = project.getBasePath();
        return base != null ? base : project.getName();
    }
}
