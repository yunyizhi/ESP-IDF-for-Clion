package org.btik.espidf.toolwindow.kconfig;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.NlsContexts;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

public class KconfSearchItemListPopupStep extends BaseListPopupStep<ConfModel> {

    public KconfSearchItemListPopupStep(@NlsContexts.PopupTitle @Nullable String title, Collection<ConfModel> result) {
        super(title, result.stream().toList());
    }

    @Override
    public Icon getIconFor(ConfModel value) {
        if (value.isAsMenuPanelItem()) {
            return AllIcons.FileTypes.Config;
        }
        return AllIcons.Nodes.Folder;
    }


}
