package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.jetbrains.annotations.NotNull;


public class JumpToSearchResultAction extends AnAction {
    private ConfModel confModel;

    public JumpToSearchResultAction(ConfModel confModel) {
        super(confModel.toString(), confModel.getId(), null);
        this.confModel = confModel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

    }
}
