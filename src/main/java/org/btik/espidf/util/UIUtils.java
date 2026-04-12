package org.btik.espidf.util;

import com.intellij.uiDesigner.core.GridConstraints;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import java.awt.*;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/9/2 22:51
 */
public class UIUtils {

    public static @NotNull GridConstraints createConstraints(int row, int column) {
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(row);
        constraints.setColumn(column);
        constraints.setAnchor(GridConstraints.ANCHOR_WEST);
        return constraints;
    }

    public static @NotNull GridConstraints createHCrowConstraints(int row, int column){
        GridConstraints constraints = createConstraints(row, column);
        constraints.setFill(GridConstraints.FILL_HORIZONTAL);
        constraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        return constraints;
    }

    public static @NotNull JLabel i18nLabel(String i18nKey) {
        return new JLabel($i18n(i18nKey));
    }

    public static void setWidth(Component component, int width) {
        Dimension preferredSize = component.getPreferredSize();
        preferredSize.width = width;
        component.setPreferredSize(preferredSize);
    }

    public static void setHeight(Component component, int height) {
        component.setSize(component.getPreferredSize().width, height);
    }
}
