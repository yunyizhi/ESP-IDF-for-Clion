package org.btik.espidf.run.config.components;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author lustre
 * @since 2024/9/10 1:42
 */
public class TextFieldFileChooser extends TextFieldWithBrowseButton {
    private FileChooserDescriptor descriptor;

    public void addActionListener(@NotNull Project project, FileChooserDescriptor descriptor, String title, String tip) {
        super.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>(
                title, tip, this, project, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
        this.descriptor = descriptor;
    }

    public void setRootDir(File rootDir) {
        if (rootDir == null || descriptor == null) {
            return;
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(rootDir);
        if (virtualFile != null) {
            descriptor.setRoots(virtualFile);
        }
    }
}
