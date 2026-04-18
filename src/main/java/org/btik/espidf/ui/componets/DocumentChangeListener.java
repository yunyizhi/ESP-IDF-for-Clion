package org.btik.espidf.ui.componets;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.util.function.Consumer;

public class DocumentChangeListener implements DocumentListener {
    private final Consumer<DocumentEvent> changeCb;

    public DocumentChangeListener(@NotNull Consumer<DocumentEvent> changeCb) {
        this.changeCb = changeCb;
    }

    public static @NotNull DocumentChangeListener onDocumentChange(@NotNull Consumer<DocumentEvent> changeCb) {
        return new DocumentChangeListener(changeCb);
    }

    public static void bindDocChange(@NotNull JTextComponent textComponent, @NotNull Consumer<DocumentEvent> changeCb) {
        textComponent.getDocument().addDocumentListener(onDocumentChange(changeCb));
    }

    public static void bindDocChange(@NotNull TextFieldWithBrowseButton textFieldWithBrowseButton, @NotNull Consumer<DocumentEvent> changeCbb) {
        bindDocChange(textFieldWithBrowseButton.getTextField(), changeCbb);
    }

    private void handleChange(DocumentEvent e) {
        changeCb.accept(e);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        handleChange(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        handleChange(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        handleChange(e);
    }
}
