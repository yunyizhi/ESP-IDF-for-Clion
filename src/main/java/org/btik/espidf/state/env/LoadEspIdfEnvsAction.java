package org.btik.espidf.state.env;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.service.IdfEnvironmentService;
import org.jetbrains.annotations.NotNull;

import static org.btik.espidf.util.I18nMessage.$i18n;

public class LoadEspIdfEnvsAction extends AnAction {
    public LoadEspIdfEnvsAction() {
        super($i18n("esp.idf.load.envs"), $i18n("esp.idf.load.envs.desc"), EspIdfIcon.IDF_RELOAD);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        IdfEnvironmentService service = project.getService(IdfEnvironmentService.class);
        service.fixEnvironmentsCache();
        service.checkEnvNeedRebuild();
    }
}
