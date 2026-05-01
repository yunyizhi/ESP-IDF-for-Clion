package org.btik.espidf.ui.componets;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
 * disable属性为false但editable 为false不可编辑但不会变灰
 *
 */
public class DisabledEditor extends BasicComboBoxEditor {
    @Override
    protected JTextField createEditorComponent() {
        JTextField textField = super.createEditorComponent();
        textField.setEditable(false);
        return textField;
    }
}
