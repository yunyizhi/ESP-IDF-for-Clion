package org.btik.espidf.toolwindow.tasks;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.sh.run.ShConfigurationType;
import com.intellij.sh.run.ShRunConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.command.ProcessEventAdaptor;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskActionNode;
import org.btik.espidf.toolwindow.tasks.web.SizeAnalysisFileType;
import org.btik.espidf.toolwindow.tasks.web.SizeAnalysisVirtualFile;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.EnvironmentVarUtil;
import org.btik.espidf.util.I18nMessage;
import org.jetbrains.ide.BuiltInServerManager;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/5/2 12:33
 */
public class EspIdfActionMap {
    private static final HashMap<String, BiConsumer<EspIdfTaskActionNode, Project>> actionMap;

    static {
        actionMap = new HashMap<>();
        actionMap.put("idf.export.console", EspIdfActionMap::exportConsole);
        actionMap.put("open.component.registry", EspIdfActionMap::openComponentRegistry);
        actionMap.put("idf.rebuild.all.env.cache", EspIdfActionMap::reBuildAllIdfEnvCache);
        actionMap.put("idf.size.analysis", EspIdfActionMap::sizeAnalysis);
    }

    private static void reBuildAllIdfEnvCache(EspIdfTaskActionNode espIdfTaskActionNode, Project project) {
        IdfEnvironmentService service = project.getService(IdfEnvironmentService.class);
        service.buildEnvironmentsCache();
        // 如果缓存建立完成，检查是否需要关闭悬浮工具栏
        service.checkEnvNeedRebuild();
        I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("esp.idf.rebuild.ok"),
                $i18n("esp.idf.rebuild.envs.ok"),
                NotificationType.INFORMATION).notify(project);
    }

    private static void openComponentRegistry(EspIdfTaskActionNode espIdfTaskActionNode, Project project) {
        BrowserUtil.browse($sys("esp.component.registry.url"));
    }

    private static void exportConsole(EspIdfTaskActionNode actionNode, Project project) {
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        CPPToolchains.Toolchain toolchain = environmentService.getToolChianOfCheckedProfile();
        if (toolchain == null) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    $i18n("action.exec.toolchain.notfound"),
                    NotificationType.ERROR).notify(project);
            return;
        }
        String envFile = toolchain.getEnvironment();
        if (StringUtil.isEmpty(envFile)) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    "Toolchain environment file is not set",
                    NotificationType.ERROR).notify(project);
            return;
        }
        Path envFilePath = Path.of(envFile);
        if (!Files.exists(envFilePath)) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    "Environment file not found: " + envFile,
                    NotificationType.ERROR).notify(project);
            return;
        }


        String displayName = actionNode.getDisplayName();
        try {
            if (IS_WINDOWS) {
                checkWindowsScript(envFile);
            }
            String basePath = project.getBasePath();
            RunnerAndConfigurationSettings settings = RunManager.getInstance(project)
                    .createConfiguration(displayName, ShConfigurationType.class);
            ShRunConfiguration runConfiguration = (ShRunConfiguration) settings.getConfiguration();
            runConfiguration.setExecuteInTerminal(true);
            runConfiguration.setExecuteScriptFile(false);
            runConfiguration.setScriptText(". \"" + envFile + '"');
            if (basePath != null) {
                runConfiguration.setScriptWorkingDirectory(basePath);
            }

            ExecutionEnvironmentBuilder builder =
                    ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), runConfiguration);
            if (builder != null) {
                ExecutionManager.getInstance(project).restartRunProfile(builder.build());
            }

        } catch (Exception e) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    e.getMessage(),
                    NotificationType.ERROR).notify(project);
        }
    }

    private static void checkWindowsScript(String envFile) {
        String fileNameLower = envFile.toLowerCase();
        if (fileNameLower.endsWith(".bat")) {
            throw new IllegalArgumentException("BAT scripts cannot be executed directly in PowerShell. " +
                    "Please use 'IDF Console' action to open a CMD terminal first, " +
                    "then manually run: " + envFile);
        }
        if ((!fileNameLower.endsWith(".ps1") && (!fileNameLower.endsWith(".powershell")))) {
            throw new IllegalArgumentException("Unsupported script type: " + envFile);
        }
    }


    private static void sizeAnalysis(EspIdfTaskActionNode actionNode, Project project) {
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        IdfProjectConfigService projectConfigService = project.getService(IdfProjectConfigService.class);
        String cmakeBuildDir = projectConfigService.getCmakeBuildDir();
        Map<String, String> environments = environmentService.getEnvironments();
        String idfFullPath = EnvironmentVarUtil.findIdfFullPath(environments);
        if (EnvironmentVarUtil.checkIdfPyNotFound(idfFullPath, project)) {
            return;
        }

        String basePath = project.getBasePath();
        String outputFile = Paths.get(basePath, cmakeBuildDir, "size_out.json").toString();

        var commandLine = new PtyCommandLine()
                .withEnvironment(environments)
                .withExePath(idfFullPath)
                .withWorkDirectory(basePath)
                .withCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")))
                .withParameters("-B", cmakeBuildDir, "size", "--format", "raw", "--output-file", outputFile);

        IdfConsoleRunProfile runProfile = new IdfConsoleRunProfile(
                actionNode.getDisplayName(), EspIdfIcon.IDF_16_16, commandLine);

        try {
            CmdTaskExecutor.execute(project, runProfile,
                    new ProcessEventAdaptor().withProcessTerminatedCb((event) -> {
                        Path outputPath = Path.of(outputFile);
                        if (event.getExitCode() == 0 && Files.exists(outputPath)) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                I18nMessage.NOTIFICATION_GROUP.createNotification(
                                        $i18n("esp.idf.size.analysis"),
                                        $i18n("esp.idf.size.analysis.complete"),
                                        NotificationType.INFORMATION).notify(project);

                                int port = BuiltInServerManager.getInstance().getPort();
                                String url = "http://localhost:" + port + "/esp-idf-size-analysis/?path=" +
                                        URLEncoder.encode(outputFile, StandardCharsets.UTF_8);
                                openPreviewInEditor(project, url, cmakeBuildDir);
                            });
                        }
                    }));
        } catch (ExecutionException e) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    e.getMessage(),
                    NotificationType.ERROR).notify(project);
        }
    }

    private static void openPreviewInEditor(Project project, String url, String cmakeBuildDir) {
        String fileName = "Size-Analysis [" + cmakeBuildDir + "]." + SizeAnalysisFileType.DEFAULT_EXTENSION;
        SizeAnalysisVirtualFile virtualFile = new SizeAnalysisVirtualFile(fileName, url, project.getBasePath());
        FileEditorManager.getInstance(project).openFile(virtualFile, true);
    }

    public static void exec(EspIdfTaskActionNode actionNode, Project project) {
        BiConsumer<EspIdfTaskActionNode, Project> action = actionMap.get(actionNode.getId());
        if (action == null) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    $i18n("action.exec.action.notfound"),
                    NotificationType.ERROR).notify(project);
            return;
        }
        action.accept(actionNode, project);
    }

}
