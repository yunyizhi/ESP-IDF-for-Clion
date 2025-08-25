package org.btik.espidf.toolwindow.kconfig;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class SearchResultCellRenderer extends ColoredListCellRenderer<ConfModel> {

    public static final SimpleTextAttributes GRAY_ITALIC_SMALL_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER | SimpleTextAttributes.STYLE_ITALIC, JBColor.GRAY);


    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends ConfModel> list, ConfModel value, int index, boolean selected, boolean hasFocus) {

        boolean asMenuPanelItem = value.isAsMenuPanelItem();
        KconfigType redefinedType = value.getRedefinedType();
        boolean isConfig = asMenuPanelItem || redefinedType == KconfigType.CHOICE_ITEM;
        setIcon(isConfig ? AllIcons.FileTypes.Config : AllIcons.Nodes.Folder);
        append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        if (!isConfig || redefinedType == KconfigType.ENABLE_SWITCH || redefinedType == KconfigType.CHOICE) {
            return;
        }
        append(" ");
        append("(" + value.getId() + ")", GRAY_ITALIC_SMALL_ATTRIBUTES);

    }
}
