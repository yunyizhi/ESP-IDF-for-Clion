package org.bitk.espidf.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.tools.ToolProcessAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.bitk.espidf.util.I18nMessage.*;
import static org.bitk.espidf.util.EnvironmentVarUtil.parseEnv;

/**
 * @author lustre
 * @since 2024/2/11 16:57
 */
public class WindowsGenerator<T> implements SubGenerator<T> {
    private String idfToolPath;

    private String idfId;

    public void setIdfToolsPath(String text) {
        idfToolPath = text;
    }


    public void setIdfId(String idfId) {
        this.idfId = idfId;
    }

    @Override
    public ValidationResult validate() {
        if (StringUtil.isEmpty(idfToolPath)) {
            return new ValidationResult($i18n("please.select.idf.tools.path.for.idf"));
        }
        if (StringUtil.isEmpty(idfId)) {
            return new ValidationResult($i18n("please.select.idf.id"));
        }
        return ValidationResult.OK;
    }

    @Override
    public void generateProject(Project project, VirtualFile baseDir, T settings, Module module) {
        new Task.Backgroundable(null, $i18n("init.idf.env")) {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                GeneralCommandLine commandLine = new GeneralCommandLine()
                        .withExePath("cmd")
                        .withCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")))
                        .withWorkDirectory(idfToolPath)
                        .withParameters(
                                "/c", idfToolPath + File.separatorChar + "idf_cmd_init.bat " + idfId +
                                        " 1>&2 && SET");
                try {
                    ProcessOutput output = new CapturingProcessRunner(new CapturingProcessHandler(commandLine))
                            .runProcess(60000);
                    if (output.isTimeout()) {
                        NOTIFICATION_GROUP.createNotification(getMsg("idf.cmd.init.failed"),
                                getMsg("idf.cmd.init.failed.timeout"), NotificationType.ERROR).notify(null);

                    } else if (output.getExitCode() != 0) {
                        NOTIFICATION_GROUP.createNotification(getMsg("idf.cmd.init.failed"),
                                output.getStderr(), NotificationType.ERROR).notify(null);
                    } else {
                        ApplicationManager.getApplication().invokeLater(() ->{
                            generateProject(project, baseDir, parseEnv(output.getStdout()));
                        });
                    }
                } catch (ExecutionException e) {
                    NOTIFICATION_GROUP.createNotification(getMsg("idf.cmd.init.failed"),
                            getMsgF("idf.cmd.init.failed.with", e.getMessage()), NotificationType.ERROR).notify(null);
                }
            }
        }.queue();


    }

    void generateProject(Project project, VirtualFile baseDir, Map<String, String> envs) {
        GeneralCommandLine generate = new GeneralCommandLine();
        generate.setExePath("idf.py.exe");
        generate.setWorkDirectory(idfToolPath);
        generate.withEnvironment(envs);
        generate.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        generate.addParameters("create-project", "-C", baseDir.getParent().getPath(), baseDir.getName());

        ExecutionEnvironment environment;
        try {
            environment = ExecutionEnvironmentBuilder.create(project, DefaultRunExecutor.getRunExecutorInstance(),
                    new IdfConsoleRunProfile("init project",
                            IconLoader.getIcon("/org-btik-esp-idf/idf16_16.svg",
                                    getClass()), generate)).build();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        environment.setExecutionId(ExecutionEnvironment.getNextUnusedExecutionId());
        try {
            environment.getRunner().execute(environment);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        OSProcessHandler handler;
        try {
            handler = new OSProcessHandler(generate);
            handler.addProcessListener(new ToolProcessAdapter(project, false, "Init Project"));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        handler.startNotify();
    }
}
