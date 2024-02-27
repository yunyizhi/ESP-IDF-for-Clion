package org.btik.espidf.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.I18nMessage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;

import static org.btik.espidf.adapter.Adapter.readEnvironment;
import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/11 17:00
 */
public class UnixLikeGenerator<T> implements SubGenerator<T> {

    private String idfFrameworkPath;

    public void setIdfFrameworkPath(String idfFrameworkPath) {
        this.idfFrameworkPath = idfFrameworkPath;
    }

    @Override
    public ValidationResult validate() {
        if (StringUtil.isEmpty(idfFrameworkPath)) {
            return new ValidationResult($i18n("please.select.idf.path"));
        }
        File file = new File(idfFrameworkPath);
        if (!file.exists()) {
            return new ValidationResult($i18n("please.select.idf.path.not.exist"));
        }
        return ValidationResult.OK;
    }

    @Override
    public void generateProject(Project project, VirtualFile baseDir, T settings, Module module) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
                IdfToolConf idfToolConf = environmentService.getUnixToolConf(idfFrameworkPath);
                final IdfToolConf idfToolConf1 = idfToolConf;
                Map<String, String> readEnvironment = ApplicationManager.getApplication()
                        .executeOnPooledThread(() -> readEnvironment(idfToolConf1)).get();
                String toolChainName = idfToolConf.getToolchain().getName();
                generateProject(project, baseDir, readEnvironment, toolChainName);
            } catch (java.util.concurrent.ExecutionException | InterruptedException | ExecutionException e) {
                I18nMessage.NOTIFICATION_GROUP.createNotification(I18nMessage.getMsg("idf.cmd.init.project.failed"),
                        e.getMessage(), NotificationType.ERROR).notify(project);
                throw new RuntimeException(e);
            }
        });
    }

    void generateProject(Project project, VirtualFile baseDir, Map<String, String> envs, String toolChainName) throws ExecutionException {
        Path idfGenerateTmpDir = baseDir.toNioPath().resolve(".tmp");
        GeneralCommandLine generate = new GeneralCommandLine();
        generate.setExePath("idf.py");
        generate.setWorkDirectory(baseDir.getPath());
        generate.withEnvironment(envs);
        generate.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        generate.addParameters("create-project", "-p", idfGenerateTmpDir.toString(),
                baseDir.getName());
        Object requestor = this;
        CmdTaskExecutor.execute(project, new IdfConsoleRunProfile($i18n("idf.create.project"),
                EspIdfIcon.IDF_16_16, generate), new ProcessListener() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                ProcessListener.super.processTerminated(event);
                if (event.getExitCode() != 0) {
                    I18nMessage.NOTIFICATION_GROUP.createNotification(I18nMessage.getMsg("idf.cmd.init.project.failed"),
                            event.getText(), NotificationType.ERROR).notify(project);
                }
                VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(idfGenerateTmpDir.toFile(), true);
                if (fileByIoFile == null) {
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            for (VirtualFile child : fileByIoFile.getChildren()) {
                                child.move(requestor, baseDir);
                            }
                            fileByIoFile.delete(requestor);
                        } catch (IOException e) {
                            I18nMessage.NOTIFICATION_GROUP.createNotification(I18nMessage.getMsg("idf.file.init.failed"),
                                    e.getMessage(), NotificationType.ERROR).notify(project);
                            throw new RuntimeException(e);
                        }
                        loadCMakeProject(project, VfsUtilCore.virtualToIoFile(baseDir), toolChainName);
                    });
                });

            }
        });

    }
}
