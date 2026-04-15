package org.btik.espidf.util;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.awt.RelativePoint;
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

    public static void showPop(JComponent component, String title, ActionGroup actionGroup){
        DataContext dataContext = DataManager.getInstance().getDataContext(component);
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(title,
                actionGroup, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
        RelativePoint pos = JBPopupFactory.getInstance().guessBestPopupLocation(component);
        popup.showInScreenCoordinates(component, pos.getScreenPoint());
    }
}
