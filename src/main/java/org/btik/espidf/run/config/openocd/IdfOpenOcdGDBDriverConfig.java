package org.btik.espidf.run.config.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.BaseProcessHandler;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.cidr.ArchitectureType;
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.run.config.EspIdfRunConfig;
import org.btik.espidf.run.config.EspIdfRunConfigFactory;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.util.OsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.btik.espidf.service.IdfEnvironmentService.ESP_ROM_ELF_DIR;
import static org.btik.espidf.util.I18nMessage.$i18n;

public class IdfOpenOcdGDBDriverConfig extends CLionGDBDriverConfiguration {
    private final EspIdfRunConfig espIdfRunConfig;

    private final Project project;

    private final GeneralCommandLine openOcdCli = new GeneralCommandLine();

    private final IdfOpenOcdProcessListener openOcdProcessListener = new IdfOpenOcdProcessListener();

    public IdfOpenOcdGDBDriverConfig(@NotNull Project project, @Nullable CPPToolchains.Toolchain toolchain, EspIdfRunConfig espIdfRunConfig) {
        super(project, toolchain);
        this.project = project;
        this.espIdfRunConfig = espIdfRunConfig;
    }

    @NotNull
    @Override
    public BaseProcessHandler<?> createDebugProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
        if (!openOcdProcessListener.isAlive()) {
            var idfOpenOcd = new IdfConsoleRunProfile($i18n("esp.idf.debug.openocd.run.title"),
                    EspIdfIcon.IDF_16_16, openOcdCli);
            var environment = ExecutionEnvironmentBuilder.create(project, DefaultRunExecutor.getRunExecutorInstance(), idfOpenOcd)
                    .build(runContentDescriptor -> {
                        ProcessHandler processHandler = runContentDescriptor.getProcessHandler();
                        if (processHandler != null) {
                            processHandler.addProcessListener(openOcdProcessListener);
                        }
                    });
            environment.setExecutionId(ExecutionEnvironment.getNextUnusedExecutionId());
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    environment.getRunner().execute(environment);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

        }
        return new KillableColoredProcessHandler(commandLine);
    }

    @Override
    public @NotNull GeneralCommandLine createDriverCommandLine(@NotNull DebuggerDriver driver, @NotNull ArchitectureType architectureType) {
        var configDataModel = espIdfRunConfig.getConfigDataModel();
        Map<String, String> envs = new HashMap<>(configDataModel.getEnvData().getEnvs());
        IdfEnvironmentService idfEnvironmentService = project.getService(IdfEnvironmentService.class);
        idfEnvironmentService.putTo(envs);

        openOcdCli.setExePath(OsUtil.getIdfExe());
        openOcdCli.setWorkDirectory(project.getBasePath());
        openOcdCli.withEnvironment(envs);
        openOcdCli.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        openOcdCli.addParameters("openocd");
        String openOcdArguments = configDataModel.getOpenOcdArguments();
        if (StringUtil.isNotEmpty(openOcdArguments)) {
            openOcdCli.addParameters("--openocd_commands", openOcdArguments);
        }

        GeneralCommandLine commandLine = new GeneralCommandLine()
                .withWorkDirectory(configDataModel.getGdbExe())
                .withWorkDirectory(project.getBasePath())
                .withCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")))
                .withEnvironment(envs)
                .withParameters("--interpreter=mi2", "-iex", "set mi-async");

        String bootloaderElf = configDataModel.getBootloaderElf();
        if (checkElf(bootloaderElf)) {
            commandLine.addParameters("-iex", "add-symbol-file " + bootloaderElf);
        }
        // 添加rom的 elf这里 可能在 ESP_ROM_ELF_DIR对应目录下 也可能 是一个其他全路径
        String romElf = configDataModel.getRomElf();
        String romElfDir = idfEnvironmentService.getEnvironments().get(ESP_ROM_ELF_DIR);
        if (checkElf(romElf)) {
            commandLine.addParameters("-iex", "add-symbol-file " + romElf);
        } else if (checkElf(Path.of(romElfDir, configDataModel.getRomElf()).toString())) {
            commandLine.addParameters("-iex", "add-symbol-file " + Path.of(romElfDir, configDataModel.getRomElf()));
        }

        String appElf = configDataModel.getAppElf();
        if (checkElf(appElf)) {
            commandLine.addParameters("-iex", "file " + appElf);
        } else {
            File appElfInBuild = EspIdfRunConfigFactory.getFileInCmakeBuildDir(project, appElf);
            if (appElfInBuild != null) {
                commandLine.addParameters("-iex", "file " + appElfInBuild);
            }
        }
        commandLine.addParameters("-ex",
                """
                        set remotetimeout 10
                        target remote :3333
                        monitor reset halt
                        maintenance flush register-cache
                        thbreak app_main
                        continue
                        """);
        return commandLine;
    }

    private boolean checkElf(String elfPath) {
        return elfPath != null && Files.exists(Path.of(elfPath));
    }
}
