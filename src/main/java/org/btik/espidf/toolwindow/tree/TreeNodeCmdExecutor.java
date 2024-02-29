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
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskTerminalCommandNode;
import org.btik.espidf.toolwindow.tree.model.RawCommandNode;
import org.btik.espidf.util.CmdTaskExecutor;

import java.nio.charset.Charset;
import java.util.Map;

import static org.btik.espidf.util.EnvironmentVarUtil.diffWithSystem;
import static org.btik.espidf.util.OsUtil.*;
import static org.btik.espidf.util.OsUtil.Const.POWER_SHELL_ENV_PREFIX;

/**
 * @author lustre
 * @since 2024/2/18 18:51
 */
public class TreeNodeCmdExecutor {
    public static void execute(EspIdfTaskCommandNode commandNode, Project project, String generatorValue) {
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(getCmdEnv());
        commandLine.setWorkDirectory(project.getBasePath());
        commandLine.withEnvironment(environmentService.getEnvironments());
        commandLine.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        if (IS_WINDOWS) {
            commandLine.addParameters(getCmdArg(), getIdfExe());
            if (!StringUtil.isEmpty(generatorValue)) {
                commandLine.addParameters("-G", generatorValue);
            }
            commandLine.addParameters(
                    commandNode.getCommand());
        } else {
            commandLine.addParameters(getCmdArg());
            String generatorArg = StringUtil.isEmpty(generatorValue) ? "" : " -G " + generatorValue;
            commandLine.addParameters(getIdfExe() + generatorArg + " " + commandNode.getCommand());
        }


        try {
            CmdTaskExecutor.execute(project, new IdfConsoleRunProfile(commandNode.getDisplayName(),
                    EspIdfIcon.IDF_16_16, commandLine), null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void execute(EspIdfTaskTerminalCommandNode commandNode, Project project, String generatorValue) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project)
                .createConfiguration(commandNode.getDisplayName(), ShConfigurationType.class);
        ShRunConfiguration runConfiguration = (ShRunConfiguration) settings.getConfiguration();
        runConfiguration.setExecuteInTerminal(true);
        runConfiguration.setExecuteScriptFile(false);
        runConfiguration.setInterpreterPath(getCmdEnv());
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        Map<String, String> environments = environmentService.getEnvironments();
        String command = commandNode.getCommand();
        String generatorArg = StringUtil.isEmpty(generatorValue) ? "" : " -G " + generatorValue;
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
                    envPrefix : envPrefix + Const.WIN_IDF_EXE + generatorArg + " " + command);
        } else {
            runConfiguration.setEnvData(EnvironmentVariablesData.create(environments, false));
            runConfiguration.setScriptText(StringUtil.isEmpty(command) ? "" : Const.UNIX_IDF_EXE +
                    generatorArg + " " + command);
        }
        runConfiguration.setScriptWorkingDirectory(basePath);

        ExecutionEnvironmentBuilder builder =
                ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), runConfiguration);
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build());
        }
    }

    public static void execute(RawCommandNode commandNode, Project project) {
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(getCmdEnv());
        commandLine.setWorkDirectory(project.getBasePath());
        commandLine.withEnvironment(environmentService.getEnvironments());
        commandLine.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        commandLine.addParameters(getCmdArg(), commandNode.getCommand());
        try {
            CmdTaskExecutor.execute(project, new IdfConsoleRunProfile(commandNode.getDisplayName(),
                    EspIdfIcon.IDF_16_16, commandLine), null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
