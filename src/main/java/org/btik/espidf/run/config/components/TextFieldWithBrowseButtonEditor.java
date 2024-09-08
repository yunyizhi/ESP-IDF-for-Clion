package org.btik.espidf.run.config.components;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @author lustre
 * @since 2024/9/8 11:10
 */
public class TextFieldWithBrowseButtonEditor implements ComboBoxEditor {
    private final TextFieldWithBrowseButton idfToolPathBrowserButton;

    public TextFieldWithBrowseButtonEditor(FileChooserDescriptor descriptor, @Nullable @NlsContexts.DialogTitle String title, @Nullable @NlsContexts.Label String description) {
        this.idfToolPathBrowserButton = new TextFieldWithBrowseButton();
        idfToolPathBrowserButton.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>(
                title, description,
                idfToolPathBrowserButton, null, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
        idfToolPathBrowserButton.getTextField().setBorder(null);

    }

    @Override
    public Component getEditorComponent() {
        return idfToolPathBrowserButton;
    }

    @Override
    public void setItem(Object anObject) {
        String text;

        if (anObject != null) {
            text = anObject.toString();
            if (text == null) {
                text = "";
            }
        } else {
            text = "";
        }
        if (!text.equals(idfToolPathBrowserButton.getText())) {
            idfToolPathBrowserButton.setText(text);
        }
    }

    @Override
    public Object getItem() {

        return idfToolPathBrowserButton.getText();
    }

    @Override
    public void selectAll() {
        JTextField textField = idfToolPathBrowserButton.getTextField();
        textField.selectAll();
        textField.requestFocus();
    }

    @Override
    public void addActionListener(ActionListener l) {
        idfToolPathBrowserButton.addActionListener(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
        idfToolPathBrowserButton.removeActionListener(l);
    }

    public String getText() {
        return idfToolPathBrowserButton.getText();
    }

    public void setText(@NotNull String text) {
        idfToolPathBrowserButton.setText(text);
    }
}
