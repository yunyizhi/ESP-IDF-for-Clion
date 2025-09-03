package org.btik.espidf.run.config.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.cidr.ArchitectureType;
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.command.ProcessEventAdaptor;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.run.config.EspIdfDebugRunConfig;
import org.btik.espidf.run.config.model.CustomDebugConfigModel;
import org.btik.espidf.run.config.model.GdbInitDebugConfigModel;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.btik.espidf.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.service.IdfEnvironmentService.ESP_ROM_ELF_DIR;
import static org.btik.espidf.service.IdfEnvironmentService.OPENOCD_COMMANDS;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;

public class IdfOpenOcdGDBDriverConfig<T> extends CLionGDBDriverConfiguration {
    private final EspIdfDebugRunConfig<T> debugRunConfig;

    private final Project project;

    private final PtyCommandLine openOcdCli = new PtyCommandLine();

    private final IdfOpenOcdProcessListener openOcdProcessListener = new IdfOpenOcdProcessListener();

    public IdfOpenOcdGDBDriverConfig(@NotNull Project project, @Nullable CPPToolchains.Toolchain toolchain, EspIdfDebugRunConfig<T> debugRunConfig) {
        super(project, toolchain);
        this.project = project;
        this.debugRunConfig = debugRunConfig;
    }

    @NotNull
    @Override
    public BaseProcessHandler<?> createDebugProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
        var idfOpenOcd = new IdfConsoleRunProfile($i18n("esp.idf.debug.openocd.run.title"), EspIdfIcon.IDF_16_16, openOcdCli);
        idfOpenOcd.addProcessListener(openOcdProcessListener);
        var environment = ExecutionEnvironmentBuilder.create(project, DefaultRunExecutor.getRunExecutorInstance(), idfOpenOcd).build();
        environment.setExecutionId(ExecutionEnvironment.getNextUnusedExecutionId());
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                environment.getRunner().execute(environment);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        KillableProcessHandler processHandler = new KillableProcessHandler(commandLine);
        processHandler.addProcessListener(new ProcessEventAdaptor().withProcessTerminatedCb((event) -> openOcdProcessListener.destroy()));
        return processHandler;
    }

    @Override
    public @NotNull GeneralCommandLine createDriverCommandLine(@NotNull DebuggerDriver driver, @NotNull ArchitectureType architectureType) {
        T configDataModel = debugRunConfig.getConfigDataModel();
        if (configDataModel instanceof CustomDebugConfigModel configModel) {
            return createCustomDriverCommandLine(driver, architectureType, configModel);
        }
        if (configDataModel instanceof GdbInitDebugConfigModel configModel) {
            return createGdbInitRunConfig(driver, architectureType, configModel);
        }

        throw new RuntimeException("not supported debugConfig[" + debugRunConfig + "]");
    }

    private void setOpenOcdProcessListener(String openOcdArguments, String buildDir, Map<String, String> envs) {
        String idfFullPath = EnvironmentVarUtil.findIdfFullPath(envs);
        if (EnvironmentVarUtil.checkIdfPyNotFound(idfFullPath, project)) {
            return;
        }
        if (OsUtil.IS_WINDOWS) {
            openOcdCli.withInitialColumns(SysConf.getInt("esp.idf.pyt.cmd.cols", 255));
        }
        openOcdCli.setExePath(idfFullPath);
        openOcdCli.withConsoleMode(true);
        openOcdCli.setWorkDirectory(project.getBasePath());

        openOcdCli.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        if (StringUtils.isEmpty(buildDir)) {
            buildDir = project.getService(IdfProjectConfigService.class).getCmakeBuildDir();
        }
        openOcdCli.withParameters("-B", buildDir);
        openOcdCli.addParameters("openocd");
        if (StringUtil.isNotEmpty(openOcdArguments)) {
            envs.put(OPENOCD_COMMANDS, openOcdArguments);
        }
        openOcdCli.withEnvironment(envs);
    }

    private @NotNull GeneralCommandLine createGdbInitRunConfig(@NotNull DebuggerDriver driver, @NotNull ArchitectureType architectureType, GdbInitDebugConfigModel configModel) {
        if (StringUtils.isEmpty(configModel.getGdbExe())) {
            throw new RuntimeException($i18n("esp.idf.debugging.gdb.not.selected"));
        }
        if (StringUtil.isEmpty(configModel.getPath())) {
            throw new RuntimeException($i18n("esp.idf.debugging.symbols.not.selected"));
        }
        Map<String, String> envs = new HashMap<>(configModel.getEnvData().getEnvs());
        IdfEnvironmentService idfEnvironmentService = project.getService(IdfEnvironmentService.class);
        idfEnvironmentService.putTo(envs);
        setOpenOcdProcessListener(configModel.getOpenOcdArguments(), configModel.getBuildDir(), envs);
        GeneralCommandLine generalCommandLine = new GeneralCommandLine()
                .withExePath(configModel.getGdbExe())
                .withWorkDirectory(project.getBasePath())
                .withCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")))
                .withEnvironment(envs)
                .withRedirectErrorStream(true)
                .withParameters("--interpreter=mi2",
                        "-iex", "set mi-async",
                        "-x", configModel.getPath());
        String[] connect = {
                "set remotetimeout 10",
                "target remote :3333",
                "monitor reset halt",
                "maintenance flush register-cache",
                "thbreak app_main"
        };
        for (String gdbCmd : connect) {
            generalCommandLine.addParameters("-ex", gdbCmd);
        }
        return generalCommandLine;
    }

    private @NotNull GeneralCommandLine createCustomDriverCommandLine(@NotNull DebuggerDriver driver, @NotNull ArchitectureType architectureType, CustomDebugConfigModel configDataModel) {
        IdfProjectConfigService projectConfigService = project.getService(IdfProjectConfigService.class);
        IdfProfileInfo selectedIdfProfileInfo = projectConfigService.getSelectedIdfProfileInfo();
        if ((selectedIdfProfileInfo != null) && (!Objects.equals(configDataModel.getTarget(), selectedIdfProfileInfo.getTarget()))) {
            ApplicationManager.getApplication().invokeLater(
                    () -> I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("esp.idf.debug.target.miss.match"),
                            $i18nF("esp.idf.debug.target.miss.match.info", configDataModel.getTarget(), selectedIdfProfileInfo.getTarget()),
                            NotificationType.WARNING).notify(project));
        }
        if (StringUtils.isEmpty(configDataModel.getGdbExe())) {
            throw new RuntimeException($i18n("esp.idf.debugging.gdb.not.selected"));
        }
        if (StringUtils.isEmpty(configDataModel.getAppElf())) {
            throw new RuntimeException($i18n("esp.idf.debugging.app.elf.not.selected"));
        }
        Map<String, String> envs = new HashMap<>(configDataModel.getEnvData().getEnvs());
        IdfEnvironmentService idfEnvironmentService = project.getService(IdfEnvironmentService.class);
        idfEnvironmentService.putTo(envs);
        setOpenOcdProcessListener(configDataModel.getOpenOcdArguments(), null, envs);
        GeneralCommandLine commandLine = new GeneralCommandLine()
                .withExePath(configDataModel.getGdbExe())
                .withWorkDirectory(project.getBasePath())
                .withCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")))
                .withEnvironment(envs)
                .withRedirectErrorStream(true)
                .withParameters("--interpreter=mi2",
                        "-iex", "set mi-async",
                        "-iex", "set confirm off");

        String bootloaderElf = configDataModel.getBootloaderElf();
        if (checkElf(bootloaderElf)) {
            commandLine.addParameters("-iex", "add-symbol-file " + gdbConsolePath(bootloaderElf));
        }
        // 添加rom的 elf这里 可能在 ESP_ROM_ELF_DIR对应目录下 也可能 是一个其他全路径
        String romElf = configDataModel.getRomElf();
        String romElfDir = idfEnvironmentService.getEnvironments().get(ESP_ROM_ELF_DIR);
        if (checkElf(romElf)) {
            commandLine.addParameters("-iex", "add-symbol-file " + gdbConsolePath(romElf));
        } else if (checkElf(Path.of(romElfDir, configDataModel.getRomElf()).toString())) {
            commandLine.addParameters("-iex", "add-symbol-file " + gdbConsolePath(Path.of(romElfDir, configDataModel.getRomElf()).toString()));
        }

        String appElf = configDataModel.getAppElf();
        if (checkElf(appElf)) {
            commandLine.addParameters("-iex", "file " + gdbConsolePath(appElf));
        } else {
            File appElfInBuild = EspIdfProjectUtil.getFileInCurrentBuildDir(project, gdbConsolePath(appElf));
            if (appElfInBuild != null) {
                commandLine.addParameters("-iex", "file " + gdbConsolePath(appElfInBuild.getPath()));
            }
        }
        String[] connect = {
                "set confirm on",
                "set remotetimeout 10",
                "target remote :3333",
                "monitor reset halt",
                "maintenance flush register-cache",
                "thbreak app_main"
        };
        for (String gdbCmd : connect) {
            commandLine.addParameters("-ex", gdbCmd);
        }
        return commandLine;
    }

    private boolean checkElf(String elfPath) {
        return elfPath != null && Files.exists(Path.of(elfPath));
    }

    private String gdbConsolePath(String path) {
        return path.replace('\\', '/');
    }
}
