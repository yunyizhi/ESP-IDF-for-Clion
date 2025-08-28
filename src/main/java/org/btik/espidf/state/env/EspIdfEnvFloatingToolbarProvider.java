package org.btik.espidf.state.env;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent;
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarProvider;
import com.intellij.openapi.project.Project;
import org.btik.espidf.service.IdfEnvironmentService;
import org.jetbrains.annotations.NotNull;

public class EspIdfEnvFloatingToolbarProvider implements FloatingToolbarProvider {
    private static final String GROUP_ID = "reload_idf_env";

    @Override
    public @NotNull ActionGroup getActionGroup() {
        AnAction action = ActionManager.getInstance().getAction(GROUP_ID);
        return (ActionGroup) action;
    }

    @Override
    public boolean getAutoHideable() {
        return false;
    }

    @Override
    public void register(@NotNull DataContext dataContext, @NotNull FloatingToolbarComponent component, @NotNull Disposable parentDisposable) {
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        IdfEnvironmentService service = project.getService(IdfEnvironmentService.class);
        service.register((visible) ->{
            if (visible) {
                component.scheduleShow();
            }else {
                component.scheduleHide();
            }
        });
    }
}
