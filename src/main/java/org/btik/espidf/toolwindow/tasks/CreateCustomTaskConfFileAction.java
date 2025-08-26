package org.btik.espidf.toolwindow.tasks;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;


public class CreateCustomTaskConfFileAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(CreateCustomTaskConfFileAction.class);
    private final Project project;
    private final String templateFileName;
    private final Runnable refreshTree;

    public CreateCustomTaskConfFileAction(@NotNull @NlsActions.ActionText String text,
                                          @NotNull final String templateFileName, @NotNull Project project,
                                          @NotNull Runnable refreshTree) {
        super(text);
        this.templateFileName = templateFileName;
        this.project = project;
        this.refreshTree = refreshTree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try (InputStream resourceAsStream = getClass().getResourceAsStream("/org-btik-esp-idf/conf/" + templateFileName)) {
            if (resourceAsStream != null) {
                Path baseDir = Path.of(Objects.requireNonNull(project.getBasePath()));
                File taskXml = baseDir.resolve(TreeXmlMeta.ESP_CUSTOM_TASKS_XML).toFile();
                Files.copy(resourceAsStream, taskXml.toPath(), StandardCopyOption.REPLACE_EXISTING);
                VirtualFile xmlVirtual = VfsUtil.findFileByIoFile(taskXml, true);
                if (xmlVirtual == null) {
                    return;
                }
                refreshTree.run();
                ApplicationManager.getApplication().invokeLater(() -> {
                    FileEditorManager instance = FileEditorManager.getInstance(project);
                    instance.openFile(xmlVirtual, true);
                    xmlVirtual.refresh(true, true);

                });

            }
        } catch (IOException ioe) {
            LOG.error(ioe);
        }
    }
}
