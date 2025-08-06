package org.btik.espidf.toolwindow.kconfig;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.command.KconfProcessHandler;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.kconfig.model.KconfigStatus;
import org.btik.espidf.util.EnvironmentVarUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2025/6/17 23:38
 */
public class KConfServer implements ProcessListener {
    private static final Logger LOG = Logger.getInstance(KConfServer.class);
    private final Project project;

    private final Consumer<KconfigStatus> onMsg;
    private KconfProcessHandler processHandler;
    private IdfConsoleRunProfile runProfile;
    private Runnable onStopCallback;

    private final StringBuilder builder = new StringBuilder();

    public KConfServer(Project project, Consumer<KconfigStatus> onMsg) {
        this.project = project;
        this.onMsg = onMsg;
    }

    public void start() {
        IdfProjectConfigService projectConfigService = project.getService(IdfProjectConfigService.class);
        String cmakeBuildDir = projectConfigService.getCmakeBuildDir();
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        Map<String, String> environments = environmentService.getEnvironments();
        GeneralCommandLine commandLine = new GeneralCommandLine()
                .withEnvironment(environments)
                .withExePath(EnvironmentVarUtil.findIdfFullPath(environments))
                .withWorkDirectory(project.getBasePath())
                .withCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")))
                .withParameters("-B", cmakeBuildDir, "confserver");
        runProfile = new IdfConsoleRunProfile($i18n("esp.idf.config.server.name"), EspIdfIcon.IDF_16_16, commandLine, true);
        try {
            processHandler = new KconfProcessHandler(commandLine);
            runProfile.setProcessHandler(processHandler);
            runProfile.addProcessListener(this);
            ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(project, DefaultRunExecutor.getRunExecutorInstance(), runProfile).build();
            environment.setExecutionId(ExecutionEnvironment.getNextUnusedExecutionId());
            final ExecutionEnvironment finalEnvironment = environment;
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    ProgramRunner<?> runner = finalEnvironment.getRunner();
                    runner.execute(finalEnvironment);
                } catch (ExecutionException e) {
                    LOG.error("start KConfServer failed", e);
                }
            });
        } catch (ExecutionException e) {
            LOG.error("start KConfServer failed", e);
        }
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        if (outputType == ProcessOutputType.STDERR){
            return;
        }
        String text = event.getText();
        builder.append(text);
        processBufferForJsonObjects();
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        if (onStopCallback != null) {
            onStopCallback.run();
        }
    }

    public void setOnStopCallback(Runnable onStopCallback) {
        this.onStopCallback = onStopCallback;
    }

    private void processBufferForJsonObjects() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        int startIndex = 0;
        while (startIndex < builder.length()) {
            int jsonStart = builder.indexOf("{", startIndex);
            if (jsonStart == -1) break;

            boolean inQuotes = false;
            boolean escapeNext = false;
            int depth = 1;
            int currentIndex = jsonStart + 1;

            while (currentIndex < builder.length() && depth > 0) {
                char c = builder.charAt(currentIndex);
                if (escapeNext) {
                    escapeNext = false;
                } else if (c == '\\') {
                    if (inQuotes) escapeNext = true;
                } else if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (!inQuotes) {
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                }
                currentIndex++;
            }

            if (depth == 0) {
                String jsonString = builder.substring(jsonStart, currentIndex);
                try {
                    JsonNode node = mapper.readTree(jsonString);
                    KconfigStatus status = mapper.treeToValue(node, KconfigStatus.class);
                    onMsg.accept(status);
                    builder.delete(0, currentIndex);
                    startIndex = 0;
                } catch (IOException e) {
                    LOG.error("Parse failed" + jsonString, e);
                    builder.delete(jsonStart, currentIndex);
                    startIndex = jsonStart;
                }
            } else {
                break; // 等待更多数据
            }
        }
    }

    public void sendCommand(String command) {
        OutputStream processStdIn = processHandler.getProcessInput();
        try {
            processStdIn.write(command.getBytes(StandardCharsets.UTF_8));
            processStdIn.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
            processStdIn.flush();
            runProfile.println(command, ConsoleViewContentType.USER_INPUT);
            LOG.info(command);
        } catch (IOException e) {
            LOG.error("sendCommand failed", e);
        }
    }

    public void stop() {
        if (processHandler == null) {
            return;
        }
        processHandler.destroyProcess();
        runProfile.println("Conf Server stoped", ConsoleViewContentType.NORMAL_OUTPUT);
    }
}
