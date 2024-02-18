package org.btik.espidf.toolwindow.tree;

import javax.swing.*;

/**
 * @author lustre
 * @since 2023/2/19 23:25
 */
public class IconTextItem<T extends JComponent>  {

    private final T t;

    public IconTextItem(T t) {
        this.t = t;
    }

    public void setText(String text) {
        if (t instanceof JLabel label) {
            label.setText(text);
        } else if (t instanceof JCheckBox checkBox) {
            checkBox.setText(text);
        }
    }

    public void setIcon(Icon defaultIcon) {
        if (t instanceof JLabel label) {
            label.setIcon(defaultIcon);
        } else if (t instanceof JCheckBox checkBox) {
            checkBox.setIcon(defaultIcon);
        }
    }

    public JComponent getComponent() {
        return t;
    }
}
