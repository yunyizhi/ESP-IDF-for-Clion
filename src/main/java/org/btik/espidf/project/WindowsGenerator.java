package org.btik.espidf.project;

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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.tools.ToolProcessAdapter;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.service.IdfToolConfService;
import org.btik.espidf.util.I18nMessage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.btik.espidf.adapter.Adapter.readEnvironment;

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
            return new ValidationResult(I18nMessage.$i18n("please.select.idf.tools.path.for.idf"));
        }
        if (StringUtil.isEmpty(idfId)) {
            return new ValidationResult(I18nMessage.$i18n("please.select.idf.id"));
        }
        return ValidationResult.OK;
    }

    @Override
    public void generateProject(Project project, VirtualFile baseDir, T settings, Module module) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                IdfToolConfService service = ApplicationManager.getApplication().getService(IdfToolConfService.class);
                IdfToolConf idfToolConf = service.getIdfToolConf();
                if (idfToolConf == null) {
                    idfToolConf = service.createWinToolConf(idfToolPath, idfId);
                }
                Map<String, String> readEnvironment = readEnvironment(idfToolConf);
                String toolChainName = idfToolConf.getToolchain().getName();
                generateProject(project, baseDir, readEnvironment, toolChainName);

            } catch (ExecutionException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    void generateProject(Project project, VirtualFile baseDir, Map<String, String> envs, String toolChainName) {
        Path idfGenerateTmpDir = baseDir.toNioPath().resolve(".tmp");
        GeneralCommandLine generate = new GeneralCommandLine();
        generate.setExePath("idf.py.exe");
        generate.setWorkDirectory(baseDir.getPath());
        generate.withEnvironment(envs);
        generate.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        generate.addParameters("create-project", "-p", idfGenerateTmpDir.toString(),
                baseDir.getName());

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
        Object requestor = this;
        OSProcessHandler handler;
        try {
            handler = new OSProcessHandler(generate);
            handler.addProcessListener(new ToolProcessAdapter(project, false, "Init Project"));
            handler.addProcessListener(new ProcessListener() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    ProcessListener.super.processTerminated(event);
                    if (event.getExitCode() == 0) {

                        ApplicationManager.getApplication().invokeLater(() -> {
                                VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(idfGenerateTmpDir.toFile(), true);
                                if (fileByIoFile != null) {
                                    ApplicationManager.getApplication().runWriteAction(() ->{
                                        try {
                                            for (VirtualFile child : fileByIoFile.getChildren()) {
                                                child.move(requestor, baseDir);
                                            }
                                        } catch (IOException e) {
                                            I18nMessage.NOTIFICATION_GROUP.createNotification(I18nMessage.getMsg("idf.file.init.failed"),
                                                    e.getMessage(), NotificationType.ERROR).notify(project);
                                            throw new RuntimeException(e);
                                        }

                                        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
                                        List<CMakeSettings.Profile> profiles = instance.getSettings().getProfiles();
                                        if (!profiles.isEmpty()) {
                                            CMakeSettings.Profile profile = profiles.get(0);
                                            instance.getSettings().setProfiles(List.of(profile
                                                    .withToolchainName(toolChainName)
                                                    .withName(IDF_CMAKE_PROFILE_NAME)
                                                    .withGenerationDir(new File(IDF_CMAKE_BUILD_DIR))));
                                        }
                                        instance.linkCMakeProject(VfsUtilCore.virtualToIoFile(baseDir));
                                        try {
                                            fileByIoFile.delete(requestor);
                                        } catch (IOException e) {
                                            I18nMessage.NOTIFICATION_GROUP.createNotification(I18nMessage.getMsg("idf.file.init.failed"),
                                                    e.getMessage(), NotificationType.ERROR).notify(project);
                                        }
                                    });

                                }
                        });
                    }
                }
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        handler.startNotify();
    }
}
