package org.btik.espidf.project;


import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.conf.IdfToolConfManager;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.I18nMessage;
import org.btik.espidf.util.OsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/11 17:18
 */
public abstract class SubGenerator<T> {
    protected final Logger LOG = Logger.getInstance(SubGenerator.class);

    protected static final String IDF_CMAKE_PROFILE_NAME = "idf";

    protected static final String IDF_CMAKE_BUILD_DIR = "build";

    protected VirtualFile baseDir;

    protected Project project;

    public abstract ValidationResult validate();

    public abstract void generateProject();

    public void generateProject(Project project, VirtualFile baseDir, T settings, Module module) {
        this.baseDir = baseDir;
        this.project = project;
        this.generateProject();
    }

    protected void generateProject(Map<String, String> envs, String toolChainName) throws ExecutionException {
        Path idfGenerateTmpDir = baseDir.toNioPath().resolve(".tmp");
        GeneralCommandLine generate = new GeneralCommandLine();
        generate.setExePath(OsUtil.getIdfExe());
        generate.setWorkDirectory(baseDir.getPath());
        generate.withEnvironment(envs);
        generate.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        generate.addParameters("create-project", "-p", idfGenerateTmpDir.toString(),
                baseDir.getName());
        IdfConsoleRunProfile createProjectProfile = new IdfConsoleRunProfile($i18n("idf.create.project"),
                EspIdfIcon.IDF_16_16, generate);
        Runnable nextTaskChain =
                () -> moveTmpDir(idfGenerateTmpDir,
                        () -> loadCMakeProject(toolChainName));
        CmdTaskExecutor.execute(project, createProjectProfile,
                nextTaskChain, $i18n("idf.cmd.init.project.failed"), true
        );
    }


    protected void loadCMakeProject(String toolChainName) {
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
    }

    protected void moveTmpDir(Path idfGenerateTmpDir, Runnable nextTask) {
        VirtualFile tmpDir = VfsUtil.findFileByIoFile(idfGenerateTmpDir.toFile(), true);
        Object requestor = project;
        if (tmpDir == null) {
            LOG.error("tmpDir not found, can't move");
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    for (VirtualFile child : tmpDir.getChildren()) {
                        child.move(requestor, baseDir);
                    }
                    tmpDir.delete(requestor);
                } catch (IOException e) {
                    I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("idf.file.init.failed"),
                            e.getMessage(), NotificationType.ERROR).notify(project);
                    LOG.error(e);
                }
                nextTask.run();
                I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("idf.tmp.folder.title"),
                        $i18n("idf.tmp.folder.may.not.deleted"), NotificationType.INFORMATION).notify(project);
            });
        });
    }

}
