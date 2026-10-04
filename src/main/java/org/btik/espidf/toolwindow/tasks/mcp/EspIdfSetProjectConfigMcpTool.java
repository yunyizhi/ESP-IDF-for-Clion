package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.impl.util.Schema_utilKt;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCategory;
import com.intellij.mcpserver.McpToolDescriptor;
import com.intellij.mcpserver.McpToolSchema;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.btik.espidf.util.EspIdfProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.EMPTY_JSON;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.readString;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.resolveProject;
import static org.btik.espidf.toolwindow.tasks.mcp.McpSchemaUtils.schema;

/**
 * 更新项目级配置，等价于工具窗口设置面板中「保存」所做的事情：
 * <ul>
 *     <li>串口（ESPPORT）；</li>
 *     <li>监视波特率、下载波特率；</li>
 *     <li>激活的 CMake profile（会同步切换运行/调试配置的执行目标）。</li>
 * </ul>
 * 仅修改传入的参数，未传入的字段保持原有配置不变；至少需要传入一个参数。
 */
public class EspIdfSetProjectConfigMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    private final McpToolDescriptor descriptor;

    public EspIdfSetProjectConfigMcpTool() {
        String projectPathParam = Schema_utilKt.getProjectPathParameterName();
        McpToolSchema toolSchema = schema()
                .string("port", $i18n("espidf.mcp.set.config.param.port"))
                .string("monitorBaud", $i18n("espidf.mcp.set.config.param.monitorBaud"))
                .string("uploadBaud", $i18n("espidf.mcp.set.config.param.uploadBaud"))
                .string("profile", $i18n("espidf.mcp.set.config.param.profile"))
                .string(projectPathParam, $i18n("espidf.mcp.common.param.projectPath"))
                .required(projectPathParam)
                .build();
        this.descriptor = new McpToolDescriptor(
                "espidf_set_project_config",
                $i18n("espidf.mcp.set.config.name"),
                $i18n("espidf.mcp.set.config.desc"),
                CATEGORY,
                "espidf_set_project_config",
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
                    $i18n("espidf.mcp.set.config.no.project"), EMPTY_JSON);
        }
        try {
            IdfProjectConfigService configService = project.getService(IdfProjectConfigService.class);
            String port = readString(input, "port");
            String monitorBaud = readString(input, "monitorBaud");
            String uploadBaud = readString(input, "uploadBaud");
            String profile = readString(input, "profile");

            if (StringUtils.isEmpty(port) && StringUtils.isEmpty(monitorBaud)
                    && StringUtils.isEmpty(uploadBaud) && StringUtils.isEmpty(profile)) {
                return McpToolCallResult.Companion.error(
                        $i18n("espidf.mcp.set.config.missing.param"), EMPTY_JSON);
            }
            if (StringUtils.isNotEmpty(profile) && configService.getIdfProfileInfo(profile) == null) {
                return McpToolCallResult.Companion.error(
                        $i18nF("espidf.mcp.set.config.unknown.profile", profile, availableProfiles(configService)),
                        EMPTY_JSON);
            }

            IdfProjectConfig current = configService.getProjectConfig();
            IdfProjectConfig next = current != null ? current : new IdfProjectConfig();
            if (StringUtils.isNotEmpty(port)) {
                next.setPort(port);
            }
            if (StringUtils.isNotEmpty(monitorBaud)) {
                next.setMonitorBaud(monitorBaud);
            }
            if (StringUtils.isNotEmpty(uploadBaud)) {
                next.setUploadBaud(uploadBaud);
            }
            if (StringUtils.isNotEmpty(profile)) {
                next.setCmakeProfile(profile);
            }

            // 与设置面板「保存」一致：持久化配置并同步切换运行/调试配置的执行目标
            Runnable apply = () -> {
                configService.updateProjectConfig(next);
                if (StringUtils.isNotEmpty(next.getCmakeProfile())) {
                    EspIdfProjectUtil.switchProfileTarget(project, next.getCmakeProfile());
                }
            };
            if (ApplicationManager.getApplication().isDispatchThread()) {
                apply.run();
            } else {
                ApplicationManager.getApplication().invokeAndWait(apply);
            }

            StringBuilder sb = new StringBuilder();
            sb.append($i18nF("espidf.mcp.set.config.updated", project.getName())).append('\n');
            sb.append("  ").append($i18n("espidf.mcp.project.info.config.port"))
                    .append(' ').append(nvl(next.getPort())).append('\n');
            sb.append("  ").append($i18n("espidf.mcp.project.info.config.monitor.baud"))
                    .append(' ').append(nvl(next.getMonitorBaud())).append('\n');
            sb.append("  ").append($i18n("espidf.mcp.project.info.config.upload.baud"))
                    .append(' ').append(nvl(next.getUploadBaud())).append('\n');
            sb.append("  ").append($i18n("espidf.mcp.project.info.config.cmake.profile"))
                    .append(' ').append(nvl(next.getCmakeProfile())).append('\n');
            return McpToolCallResult.Companion.text(sb.toString(), EMPTY_JSON);
        } catch (Throwable e) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.set.config.failed", e.getMessage()), EMPTY_JSON);
        }
    }

    private static String availableProfiles(IdfProjectConfigService configService) {
        List<IdfProfileInfo> profiles = configService.getIdfProfileInfoList();
        if (profiles == null || profiles.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (IdfProfileInfo p : profiles) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(p.getDisplayName());
        }
        return sb.toString();
    }

    private static String nvl(String v) {
        return StringUtils.isEmpty(v) ? "-" : v;
    }
}
