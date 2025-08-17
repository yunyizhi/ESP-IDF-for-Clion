package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class KconfOptAction extends AnAction {
    private final Runnable callback;

    private boolean isRunning = false;

    public KconfOptAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon, Runnable callback) {
        super(text, description, icon);
        this.callback = callback;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!isRunning) {
            return;
        }
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(isRunning);
    }

    public void setStatus(boolean refInitOk) {
        this.isRunning = refInitOk;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }


}
