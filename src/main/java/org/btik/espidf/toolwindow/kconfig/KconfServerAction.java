package org.btik.espidf.toolwindow.kconfig;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2025/7/7 23:24
 */
public class KconfServerAction extends AnAction {

    private Runnable callback;

    private boolean isRunning = false;

    public KconfServerAction() {
        super($i18n("esp.idf.kconfig.start.tip"), $i18n("esp.idf.kconfig.start.tip"), AllIcons.Actions.Execute);
    }


    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Presentation presentation = e.getPresentation();
        if (isRunning) {
            presentation.setIcon(AllIcons.Run.Stop);
            presentation.setText($i18n("esp.idf.kconfig.stop.tip"));
        } else {
            presentation.setIcon(AllIcons.Actions.Execute);
            presentation.setText($i18n("esp.idf.kconfig.start.tip"));
        }
    }

    public void setStatus(boolean refInitOk) {
        this.isRunning = refInitOk;
    }
}
