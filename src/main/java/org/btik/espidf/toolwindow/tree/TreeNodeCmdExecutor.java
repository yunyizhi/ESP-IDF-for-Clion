package org.btik.espidf.toolwindow.tree;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.sh.run.ShConfigurationType;
import com.intellij.sh.run.ShRunConfiguration;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskActionNode;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskConsoleCommandNode;
import org.btik.espidf.toolwindow.tree.model.RawCommandNode;
import org.btik.espidf.util.CmdTaskExecutor;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.btik.espidf.service.IdfEnvironmentService.*;
import static org.btik.espidf.service.IdfProjectConfigService.PORT_CONF_AUTO;
import static org.btik.espidf.util.EnvironmentVarUtil.diffWithSystem;
import static org.btik.espidf.util.OsUtil.*;
import static org.btik.espidf.util.OsUtil.Const.POWER_SHELL_ENV_PREFIX;

/**
 * @author lustre
 * @since 2024/2/18 18:51
 */
public class TreeNodeCmdExecutor {
    public static void execute(EspIdfTaskCommandNode commandNode,@NotNull Project project) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(getCmdEnv());
        commandLine.setWorkDirectory(project.getBasePath());
        commandLine.withEnvironment(getEnvsWithProjectSettings(project));
        commandLine.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        if (IS_WINDOWS) {
            commandLine.addParameters(getCmdArg(),
                    getIdfExe(),
                    commandNode.getCommand());
        } else {
            commandLine.addParameters(getCmdArg(),
                    getIdfExe() + " " + commandNode.getCommand());
        }


        try {
            CmdTaskExecutor.execute(project, new IdfConsoleRunProfile(commandNode.getDisplayName(),
                    EspIdfIcon.IDF_16_16, commandLine), null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void execute(EspIdfTaskConsoleCommandNode commandNode,@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project)
                .createConfiguration(commandNode.getDisplayName(), ShConfigurationType.class);
        ShRunConfiguration runConfiguration = (ShRunConfiguration) settings.getConfiguration();
        runConfiguration.setExecuteInTerminal(commandNode.isInTerminal());
        runConfiguration.setExecuteScriptFile(false);
        runConfiguration.setInterpreterPath(getCmdEnv());
        Map<String, String> environments = getEnvsWithProjectSettings(project);
        String command = commandNode.getCommand();
        if (IS_WINDOWS) {
            StringBuilder envPrefixBuilder = new StringBuilder();
            diffWithSystem(environments).forEach((key, value) -> {
                envPrefixBuilder.append(POWER_SHELL_ENV_PREFIX).append(key).append("=");
                if (!value.startsWith("\"")) {
                    envPrefixBuilder.append("\"").append(value).append("\"");
                } else {
                    envPrefixBuilder.append(value);
                }
                envPrefixBuilder.append(";");
            });
            String envPrefix = envPrefixBuilder.toString();
            runConfiguration.setScriptText(StringUtil.isEmpty(command) ?
                    envPrefix : envPrefix + Const.WIN_IDF_EXE + " " + command);
        } else {
            runConfiguration.setEnvData(EnvironmentVariablesData.create(environments, false));
            runConfiguration.setScriptText(StringUtil.isEmpty(command) ? "" : Const.UNIX_IDF_EXE + " " + command);
        }
        runConfiguration.setScriptWorkingDirectory(basePath);

        ExecutionEnvironmentBuilder builder =
                ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), runConfiguration);
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build());
        }
    }

    public static void execute(RawCommandNode commandNode,@NotNull Project project) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(getCmdEnv());
        commandLine.setWorkDirectory(project.getBasePath());
        commandLine.withEnvironment(getEnvsWithProjectSettings(project));
        commandLine.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        commandLine.addParameters(getCmdArg(), commandNode.getCommand());
        try {
            CmdTaskExecutor.execute(project, new IdfConsoleRunProfile(commandNode.getDisplayName(),
                    EspIdfIcon.IDF_16_16, commandLine), null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void execute(EspIdfTaskActionNode actionNode, Project project) {
        EspIdfActionMap.exec(actionNode, project);
    }

    private static Map<String, String> getEnvsWithProjectSettings(@NotNull Project project) {
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        Map<String, String> environments = environmentService.getEnvironments();

        IdfProjectConfigService service = project.getService(IdfProjectConfigService.class);
        IdfProjectConfig projectConfig = service.getProjectConfig();
        if (projectConfig.isEmpty()) {
            return environments;
        }
        // 避免使用Map.of的返回结果
        if (environments.isEmpty()) {
            environments = new HashMap<>();
        }
        Map<String, String> projectEnvs = buildProjectSettingToEnvs(projectConfig);
        environments.putAll(projectEnvs);
        return environments;
    }

    private static Map<String, String> buildProjectSettingToEnvs(@NotNull IdfProjectConfig projectConfig) {
        if (projectConfig.isEmpty()) {
            return Map.of();
        }
        Map<String, String> envs = new HashMap<>();
        String monitorBaud = projectConfig.getMonitorBaud();
        if (StringUtil.isNotEmpty(monitorBaud) && !monitorBaud.equals(PORT_CONF_AUTO)) {
            envs.put(IDF_MONITOR_BAUD, monitorBaud);
        }
        String uploadBaud = projectConfig.getUploadBaud();
        if (StringUtil.isNotEmpty(uploadBaud) && !uploadBaud.equals(PORT_CONF_AUTO)) {
            envs.put(ESP_BAUD, uploadBaud);
        }
        String port = projectConfig.getPort();
        if (StringUtil.isNotEmpty(port)) {
            envs.put(ESP_PORT, port);
        }
        return envs;
    }
}
