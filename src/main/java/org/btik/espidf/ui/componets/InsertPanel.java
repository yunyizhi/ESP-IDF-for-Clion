package org.btik.espidf.ui.componets;

import javax.swing.*;
import java.awt.*;

/**
 * @author lustre
 * @since 2025/6/26 23:59
 */
public class InsertPanel extends JPanel {

    public InsertPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    public InsertPanel(LayoutManager layout) {
        super(layout);
    }

    public InsertPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public InsertPanel() {
    }

    private Insets insets;

    public void setInsets(Insets insets) {
        this.insets = insets;
    }

    @Override
    public Insets getInsets() {
       if (insets == null) {
           return super.getInsets();
       }
       return insets;
    }
}
