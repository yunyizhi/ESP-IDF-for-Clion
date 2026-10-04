package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.impl.util.Schema_utilKt;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCategory;
import com.intellij.mcpserver.McpToolDescriptor;
import com.intellij.mcpserver.McpToolSchema;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonObject;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.EMPTY_JSON;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.resolveProject;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.schema;

/**
 * 返回当前项目的关键信息，供 MCP 客户端在无头环境下复用与界面一致的构建环境：
 * <ul>
 *     <li>获取环境变量的脚本路径（toolchain 关联的 export 脚本，客户端可自行 source 得到构建环境变量）；</li>
 *     <li>当前激活的 CMake profile 及全部可用 profile（含目标芯片、构建目录）；</li>
 *     <li>项目级配置（串口、监视波特率、下载波特率、CMake profile）。</li>
 * </ul>
 * 相比直接返回解析后的环境变量，返回脚本路径更轻量，也避免了环境变量随机器差异带来的问题。
 */
public class EspIdfProjectInfoMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    private final McpToolDescriptor descriptor;

    public EspIdfProjectInfoMcpTool() {
        String projectPathParam = Schema_utilKt.getProjectPathParameterName();
        McpToolSchema toolSchema = schema()
                .string(projectPathParam, $i18n("espidf.mcp.common.param.projectPath"))
                .required(projectPathParam)
                .build();
        this.descriptor = new McpToolDescriptor(
                "espidf_get_project_info",
                $i18n("espidf.mcp.project.info.name"),
                $i18n("espidf.mcp.project.info.desc"),
                CATEGORY,
                "espidf_get_project_info",
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
                    $i18n("espidf.mcp.project.info.no.project"), EMPTY_JSON);
        }
        try {
            IdfEnvironmentService envService = project.getService(IdfEnvironmentService.class);
            IdfProjectConfigService configService = project.getService(IdfProjectConfigService.class);

            StringBuilder sb = new StringBuilder();
            sb.append($i18n("espidf.mcp.project.info.header")).append('\n');
            sb.append($i18n("espidf.mcp.project.info.project"))
                    .append(' ').append(project.getName()).append('\n');
            sb.append($i18n("espidf.mcp.project.info.base.path"))
                    .append(' ').append(project.getBasePath()).append('\n');

            IdfProfileInfo profileInfo = configService.getSelectedIdfProfileInfo();
            if (profileInfo != null) {
                sb.append($i18n("espidf.mcp.project.info.profile"))
                        .append(' ').append(profileInfo.getDisplayName()).append('\n');
                if (profileInfo.getTarget() != null) {
                    sb.append($i18n("espidf.mcp.project.info.target"))
                            .append(' ').append(profileInfo.getTarget()).append('\n');
                }
                if (profileInfo.getBuildDir() != null) {
                    sb.append($i18n("espidf.mcp.project.info.build.dir"))
                            .append(' ').append(profileInfo.getBuildDir()).append('\n');
                }
            }

            appendProfiles(sb, configService.getIdfProfileInfoList(), profileInfo);

            CPPToolchains.Toolchain toolchain = ApplicationManager.getApplication().runReadAction(
                    (Computable<CPPToolchains.Toolchain>) envService::getToolChianOfCheckedProfile);
            if (toolchain == null) {
                sb.append($i18n("espidf.mcp.project.info.no.toolchain")).append('\n');
            } else {
                sb.append($i18n("espidf.mcp.project.info.toolchain"))
                        .append(' ').append(toolchain.getName()).append('\n');
                String envScript = toolchain.getEnvironment();
                if (StringUtils.isEmpty(envScript)) {
                    sb.append($i18nF("espidf.mcp.project.info.no.env.script", toolchain.getName())).append('\n');
                } else {
                    sb.append($i18n("espidf.mcp.project.info.env.script"))
                            .append(' ').append(envScript).append('\n');
                }
            }

            IdfProjectConfig config = configService.getProjectConfig();
            sb.append($i18n("espidf.mcp.project.info.config")).append('\n');
            if (config != null) {
                sb.append("  ").append($i18n("espidf.mcp.project.info.config.port"))
                        .append(' ').append(nvl(config.getPort())).append('\n');
                sb.append("  ").append($i18n("espidf.mcp.project.info.config.monitor.baud"))
                        .append(' ').append(nvl(config.getMonitorBaud())).append('\n');
                sb.append("  ").append($i18n("espidf.mcp.project.info.config.upload.baud"))
                        .append(' ').append(nvl(config.getUploadBaud())).append('\n');
                sb.append("  ").append($i18n("espidf.mcp.project.info.config.cmake.profile"))
                        .append(' ').append(nvl(config.getCmakeProfile())).append('\n');
            }

            return McpToolCallResult.Companion.text(sb.toString(), EMPTY_JSON);
        } catch (Throwable e) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.project.info.failed", e.getMessage()), EMPTY_JSON);
        }
    }

    /**
     * 追加「全部可用 CMake profile」列表，标注当前激活项，并附带每个 profile 的目标芯片与构建目录，
     * 供 MCP 客户端调用 {@code espidf_set_project_config} 切换 profile 时选用。
     */
    private static void appendProfiles(StringBuilder sb, List<IdfProfileInfo> profiles, IdfProfileInfo active) {
        sb.append($i18n("espidf.mcp.project.info.profiles")).append('\n');
        if (profiles == null || profiles.isEmpty()) {
            sb.append("  ").append($i18n("espidf.mcp.project.info.profiles.empty")).append('\n');
            return;
        }
        String activeName = active == null ? null : active.getDisplayName();
        for (IdfProfileInfo p : profiles) {
            sb.append("  - ").append(p.getDisplayName());
            if (Objects.equals(p.getDisplayName(), activeName)) {
                sb.append(" [").append($i18n("espidf.mcp.project.info.profiles.active")).append(']');
            }
            if (p.getTarget() != null) {
                sb.append("  ").append($i18n("espidf.mcp.project.info.target"))
                        .append(' ').append(p.getTarget());
            }
            if (p.getBuildDir() != null) {
                sb.append("  ").append($i18n("espidf.mcp.project.info.build.dir"))
                        .append(' ').append(p.getBuildDir());
            }
            sb.append('\n');
        }
    }

    private static String nvl(String v) {
        return StringUtils.isEmpty(v) ? "-" : v;
    }
}
