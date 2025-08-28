package org.btik.espidf.state.env;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.btik.espidf.service.IdfEnvironmentService;
import org.jetbrains.annotations.NotNull;

import static org.btik.espidf.util.I18nMessage.$i18n;

public class LoadEspIdfEnvsCancelAction extends AnAction {

    public LoadEspIdfEnvsCancelAction() {
        super($i18n("esp.idf.load.envs.cancel"), $i18n("esp.idf.load.envs.cancel"), AllIcons.Actions.Cancel);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        IdfEnvironmentService service = project.getService(IdfEnvironmentService.class);
        service.setFloatingToolbarVisible(false);
    }
}
