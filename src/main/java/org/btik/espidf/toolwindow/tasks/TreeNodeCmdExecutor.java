package org.btik.espidf.toolwindow.tasks;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.sh.run.ShConfigurationType;
import com.intellij.sh.run.ShRunConfiguration;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.command.MonitorProcessHandler;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskActionNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskConsoleCommandNode;
import org.btik.espidf.toolwindow.tasks.model.RawCommandNode;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.EnvironmentVarUtil;
import org.btik.espidf.util.I18nMessage;
import org.btik.espidf.util.SysConf;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.btik.espidf.service.IdfEnvironmentService.*;
import static org.btik.espidf.service.IdfProjectConfigService.PORT_CONF_AUTO;
import static org.btik.espidf.util.EnvironmentVarUtil.diffWithSystem;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.util.OsUtil.*;
import static org.btik.espidf.util.OsUtil.Const.POWER_SHELL_ENV_PREFIX;

/**
 * @author lustre
 * @since 2024/2/18 18:51
 */
public class TreeNodeCmdExecutor {
    private static final HashMap<String, MonitorProcessHandler> monitorProcessHandlers = new HashMap<>();

    public static void execute(EspIdfTaskCommandNode commandNode, @NotNull Project project) {
        Map<String, String> envsWithProjectSettings = getEnvsWithProjectSettings(project);
        String port = envsWithProjectSettings.get(ESP_PORT);
        if (port == null) {
            port = PORT_CONF_AUTO;
        }
        // 关停带monitor的同端口任务
        if (commandNode.isRequestPort()) {
            MonitorProcessHandler aliveHandler = monitorProcessHandlers.get(port);
            if (aliveHandler != null) {
                aliveHandler.destroyProcess();
                I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("esp.idf.monitor.task.auto.stop.title"),
                        $i18nF("esp.idf.monitor.task.auto.stop.msg", aliveHandler.getTaskRawName(), commandNode.getDisplayName()),
                        NotificationType.INFORMATION).notify(project);
            }
        }


        PtyCommandLine commandLine = new PtyCommandLine();
        IdfEnvironmentService idfEnvironmentService = project.getService(IdfEnvironmentService.class);
        commandLine.setExePath(EnvironmentVarUtil.findIdfFullPath(idfEnvironmentService.getEnvironments()));
        commandLine.setWorkDirectory(project.getBasePath());
        commandLine.withEnvironment(envsWithProjectSettings);
        commandLine.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        commandLine.addParameters("-B", project.getService(IdfProjectConfigService.class).getCmakeBuildDir());
        commandLine.addParameters(commandNode.getCommand().split(" "));
        if (IS_WINDOWS) {
            commandLine.withInitialColumns(SysConf.getInt("esp.idf.pyt.cmd.cols", 255));
        }
        try {
            IdfConsoleRunProfile idfConsoleRunProfile = new IdfConsoleRunProfile(commandNode.getDisplayName(),
                    EspIdfIcon.IDF_16_16, commandLine);
            if (commandNode.isUseMonitor()) {
                registerMonitorHandler(port, idfConsoleRunProfile);
            }
            idfConsoleRunProfile.setUseOutFilter(commandNode.isOutFilter());
            CmdTaskExecutor.execute(project, idfConsoleRunProfile, null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMonitorHandler(final String port, IdfConsoleRunProfile idfConsoleRunProfile) throws ExecutionException {
        MonitorProcessHandler monitorProcessHandler = new MonitorProcessHandler(idfConsoleRunProfile.getCommandLine());
        monitorProcessHandler.setTaskRawName(idfConsoleRunProfile.getName());
        idfConsoleRunProfile.setProcessHandler(monitorProcessHandler);
        monitorProcessHandlers.put(port, monitorProcessHandler);
        idfConsoleRunProfile.addProcessListener(new ProcessListener() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                ProcessListener.super.processTerminated(event);
                monitorProcessHandlers.remove(port);
            }
        });
    }

    public static void execute(EspIdfTaskConsoleCommandNode commandNode, @NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        String cmakeBuildDir = project.getService(IdfProjectConfigService.class).getCmakeBuildDir();
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
                    envPrefix : envPrefix + Const.IDF_EXE + " -B " + cmakeBuildDir + " " + command);
        } else {
            runConfiguration.setEnvData(EnvironmentVariablesData.create(environments, false));
            runConfiguration.setScriptText(StringUtil.isEmpty(command) ? "" : Const.IDF_EXE + " -B " + cmakeBuildDir + " " + command);
        }
        runConfiguration.setScriptWorkingDirectory(basePath);

        ExecutionEnvironmentBuilder builder =
                ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), runConfiguration);
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build());
        }
    }

    public static void execute(RawCommandNode commandNode, @NotNull Project project) {
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

    public static Map<String, String> getEnvsWithProjectSettings(@NotNull Project project) {
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        IdfProjectConfig projectConfig = project.getService(IdfProjectConfigService.class).getProjectConfig();
        Map<String, String> projectEnvs = buildProjectSettingToEnvs(projectConfig);
        environmentService.putTo(projectEnvs);
        return projectEnvs;
    }

    public static Map<String, String> buildProjectSettingToEnvs(@NotNull IdfProjectConfig projectConfig) {
        if (projectConfig.isEmpty()) {
            return new HashMap<>();
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
