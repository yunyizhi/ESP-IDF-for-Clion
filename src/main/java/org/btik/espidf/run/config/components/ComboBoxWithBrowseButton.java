package org.btik.espidf.run.config.components;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TextAccessor;

/**
 * @author lustre
 * @since 2024/9/8 10:42
 */
public class ComboBoxWithBrowseButton extends ComboBox<String> implements TextAccessor {
    private final TextFieldWithBrowseButtonEditor editor1;

    public ComboBoxWithBrowseButton(FileChooserDescriptor descriptor, Project project) {
        editor1 = new TextFieldWithBrowseButtonEditor(descriptor, project, null, null);
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
}
