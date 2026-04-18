package org.btik.espidf.ui.componets;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author lustre
 * @since 2024/9/10 1:42
 */
public class TextFieldFileChooser extends TextFieldWithBrowseButton {
    private FileChooserDescriptor descriptor;

    public void addActionListener(@Nullable Project project, FileChooserDescriptor descriptor) {
        super.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>(this, project,
                descriptor, TextComponentAccessor.TEXT_FIELD_SELECTED_TEXT));
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
