package org.btik.espidf.run.config;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.BaseProcessHandler;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import com.jetbrains.cidr.ArchitectureType;
import com.jetbrains.cidr.cpp.execution.CLionLauncher;
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration;
import com.jetbrains.cidr.cpp.execution.debugger.peripheralview.SvdPanel;
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.CidrCoroutineHelper;
import com.jetbrains.cidr.execution.CidrPathConsoleFilter;
import com.jetbrains.cidr.execution.TrivialRunParameters;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import kotlin.Pair;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.util.OsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.SystemIndependent;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * @author lustre
 * @since 2024/9/2 23:05
 */
public class EspIdfLauncher extends CLionLauncher {
    private final EspIdfRunConfig espIdfRunConfig;

    public EspIdfLauncher(@NotNull ExecutionEnvironment executionEnvironment, @NotNull EspIdfRunConfig configuration) {
        super(executionEnvironment, configuration);
        this.espIdfRunConfig = configuration;
    }

    @Override
    public @NotNull Pair<File, CPPEnvironment> getRunFileAndEnvironment() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean emulateTerminal(@NotNull CPPEnvironment environment, boolean isDebugMode) {
        return false;
    }

    @Override
    public @NotNull ProcessHandler createProcess(@NotNull CommandLineState state) throws ExecutionException {
        Project project = getProject();
        GeneralCommandLine showVersion = new GeneralCommandLine();
        showVersion.setExePath(OsUtil.getIdfExe());
        showVersion.setWorkDirectory(project.getBasePath());
        showVersion.withEnvironment(project.getService(IdfEnvironmentService.class).getEnvironments());
        showVersion.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        showVersion.addParameters("--help");
        return new KillableColoredProcessHandler(showVersion);
    }

    @Override
    public @NotNull XDebugProcess createDebugProcess(@NotNull CommandLineState state, @NotNull XDebugSession session) throws ExecutionException {
        Project project = getProject();
        @SystemIndependent final String projectPath = project.getBasePath();
        IdfEnvironmentService idfEnvironmentService = project.getService(IdfEnvironmentService.class);
        DebuggerDriverConfiguration debuggerDriverConfiguration = getDebuggerDriverConfiguration(idfEnvironmentService, project);

        GeneralCommandLine commandLine = new GeneralCommandLine("").withWorkDirectory(project.getBasePath());
        TrivialRunParameters parameters = new TrivialRunParameters(debuggerDriverConfiguration, commandLine, ArchitectureType.UNKNOWN);
        final ConsoleFilterProvider consoleCopyFilter = project1 -> new Filter[]{(s, i) -> {
            session.getConsoleView().print(s, ConsoleViewContentType.NORMAL_OUTPUT);
            return null;
        }};
        return CidrCoroutineHelper.runOnEDT(
                () -> new CidrDebugProcess(parameters, session, state.getConsoleBuilder(),
                        consoleCopyFilter) {

                    @Override
                    public @NotNull
                    XDebugTabLayouter createTabLayouter() {
                        CidrDebugProcess gdbDebugProcess = this;
                        XDebugTabLayouter innerLayouter = super.createTabLayouter();
                        return new XDebugTabLayouter() {
                            @NotNull
                            @Override
                            public Content registerConsoleContent(@NotNull RunnerLayoutUi ui, @NotNull ExecutionConsole console) {
                                return innerLayouter.registerConsoleContent(ui, console);
                            }

                            @Override
                            public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
                                innerLayouter.registerAdditionalContent(ui);
                                SvdPanel.registerPeripheralTab(gdbDebugProcess, ui, null);
                            }
                        };
                    }

                    @Override
                    public @NotNull ConsoleView createConsole() {
                        ConsoleView console = super.createConsole();
                        console.addMessageFilter(new CidrPathConsoleFilter(getProject(), null, Path.of(projectPath)));
                        return console;
                    }

                    @Override
                    protected @NotNull
                    DebuggerDriver.Inferior doLoadTarget(@NotNull DebuggerDriver driver) throws ExecutionException {

                        DebuggerDriver.Inferior tempInferior = driver.loadForRemote("", null, null, Collections.emptyList());
                        return driver.new Inferior(tempInferior.getId()) {
                            @SuppressWarnings("RedundantThrows")
                            @Override
                            protected long startImpl() throws ExecutionException {
                                return 0;
                            }

                            @Override
                            protected void detachImpl() throws ExecutionException {
                                tempInferior.detach();
                            }

                            @Override
                            protected boolean destroyImpl() throws ExecutionException {
                                return tempInferior.destroy();
                            }
                        };
                    }
                });
    }

    private @NotNull DebuggerDriverConfiguration getDebuggerDriverConfiguration(IdfEnvironmentService idfEnvironmentService, Project project) {
        CPPToolchains.Toolchain currentToolchain = idfEnvironmentService.getCurrentToolchain();
        return new CLionGDBDriverConfiguration(project, currentToolchain) {
            @NotNull
            @Override
            public BaseProcessHandler<?> createDebugProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
                return new KillableColoredProcessHandler(commandLine);
            }

            @Override
            public @NotNull GeneralCommandLine createDriverCommandLine(@NotNull DebuggerDriver driver, @NotNull ArchitectureType architectureType) throws ExecutionException {
                GeneralCommandLine commandLine = new GeneralCommandLine();
                commandLine.setExePath(idfEnvironmentService.getGdbExe());
                commandLine.setWorkDirectory(project.getBasePath());
                Map<String, String> envs = espIdfRunConfig.getEnvData().getEnvs();
                Map<String, String> environments = idfEnvironmentService.getEnvironments();
                environments.putAll(envs);
                commandLine.withEnvironment(environments);
                commandLine.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
                commandLine.addParameters("--interpreter=mi2", "-iex", "set mi-async", "-ix", "build/gdbinit/gdbinit");
                return commandLine;
            }
        };
    }

}
