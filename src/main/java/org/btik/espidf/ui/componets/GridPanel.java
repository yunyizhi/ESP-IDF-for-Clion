package org.btik.espidf.ui.componets;

import org.btik.espidf.util.UIUtils;

import javax.swing.*;
import java.awt.*;

public class GridPanel extends JPanel {
    private int rowIndex = 0;
    private int columnIndex = 0;

    public GridPanel(int rowCount, int columnCount) {
        super(new GridLayout(rowCount, columnCount));
    }

    public void add(JComponent component) {
        add(component, UIUtils.createConstraints(rowIndex, columnIndex));
        columnIndex++;
    }

    public void addGrow(JComponent component) {
        add(component, UIUtils.createHCrowConstraints(rowIndex, columnIndex));
        columnIndex++;
    }

    public void newRow() {
        rowIndex++;
        columnIndex = 0;
    }
}
