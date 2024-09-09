package org.btik.espidf.run.config.components;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.TextAccessor;

import java.io.File;

/**
 * @author lustre
 * @since 2024/9/8 10:42
 */
public class ComboBoxWithBrowseButton extends ComboBox<String> implements TextAccessor {
    private final TextFieldWithBrowseButtonEditor editor1;
    private final FileChooserDescriptor fileChooserDescriptor;

    public ComboBoxWithBrowseButton(FileChooserDescriptor descriptor, Project project, String title, String description) {
        editor1 = new TextFieldWithBrowseButtonEditor(descriptor, project, title, description);
        this.fileChooserDescriptor = descriptor;
        setEditor(editor1);
        setEditable(true);
    }

    @Override
    public String getText() {
        return editor1.getText();
    }

    @Override
    public void setText(String text) {
        editor1.setText(text != null ? text : "");
        setSelectedItem(text);
    }

    public void setRootDir(File rootDir) {
        if (rootDir == null || fileChooserDescriptor == null) {
            return;
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(rootDir);
        if (virtualFile != null) {
            fileChooserDescriptor.setRoots(virtualFile);
        }
    }
}
