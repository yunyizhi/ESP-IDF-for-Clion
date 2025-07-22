package org.btik.espidf.run.config;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import org.btik.espidf.toolwindow.tasks.TreeNodeCmdExecutor;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskCommandNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2023/5/9 15:38
 */
public class CheckBuildTypeAction extends AnAction implements DumbAware {

    private final String chipTarget;

    public CheckBuildTypeAction(@Nullable @NlsActions.ActionText String text,@NotNull String chipTarget) {
        super(text);
        this.chipTarget = chipTarget;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        Project project = e.getProject();
        if (project == null) {
            return;
        }
        TreeNodeCmdExecutor.execute(new EspIdfTaskCommandNode($i18n("idf.set.project.target"),
                "set-target " + chipTarget, true
        ), project);
    }
}
