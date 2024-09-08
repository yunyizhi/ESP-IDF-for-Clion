package org.btik.espidf.run.config.components;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.ui.TextAccessor;
import org.jetbrains.annotations.NotNull;

/**
 * @author lustre
 * @since 2024/9/8 10:42
 */
public class ComboBoxWithBrowseButton extends ComboBox<String> implements TextAccessor {
    private TextFieldWithBrowseButtonEditor editor1;

    public ComboBoxWithBrowseButton(FileChooserDescriptor descriptor) {
        editor1 = new TextFieldWithBrowseButtonEditor(descriptor, null, null);
        setEditor(editor1);
        setEditable(true);
        descriptor.withShowHiddenFiles(true);
    }

    @Override
    public String getText() {
        return editor1.getText();
    }

    @Override
    public void setText(String text) {
        editor1.setText(text != null ? text : "");
    }
}
