package org.btik.espidf.util;

import com.intellij.uiDesigner.core.GridConstraints;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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

    public static @NotNull JLabel i18nLabel(String i18nKey) {
       return new JLabel($i18n(i18nKey));
    }
}
