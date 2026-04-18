package org.btik.espidf.ui.componets;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.btik.espidf.util.UIUtils;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;

import static org.btik.espidf.util.UIUtils.i18nLabel;

public class GridPanel extends JPanel {
    private int rowIndex = 0;
    private int columnIndex = 0;

    public GridPanel(int rowCount, int columnCount) {
        super(new GridLayoutManager(rowCount, columnCount));
    }

    public GridPanel(GridLayoutManager gridLayoutManager) {
        super(gridLayoutManager);
    }


    public void add(@NonNull JComponent component, Object constraints) {
        super.add(component, constraints);
        columnIndex++;
    }

    public void add(JComponent component, int colSpan) {
        GridConstraints constraints = UIUtils.createConstraints(rowIndex, columnIndex);
        constraints.setColSpan(colSpan);
        add(component, constraints);
    }

    public void add(JComponent component) {
        add(component, UIUtils.createConstraints(rowIndex, columnIndex));
    }

    public void addCol(JComponent component, int column) {
        add(component, UIUtils.createConstraints(rowIndex, column));
    }

    public void addGrow(JComponent component) {
        add(component, UIUtils.createHCrowConstraints(rowIndex, columnIndex));
    }

    public void addNewFormRow(String i8nLabelKey, JComponent component, boolean valueIsGrow) {
        addNewFormRow(i18nLabel(i8nLabelKey), component, valueIsGrow);
    }

    public void addNewFormRow(JLabel label, JComponent component, boolean valueIsGrow) {
        add(label);
        if (valueIsGrow) {
            addGrow(component);
        } else {
            add(component);
        }
        newRow();
    }

    public void newRow() {
        rowIndex++;
        columnIndex = 0;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public int getColumnIndex() {
        return columnIndex;
    }
}
