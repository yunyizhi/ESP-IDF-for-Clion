package org.btik.espidf.state;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import org.btik.espidf.service.IdfProjectConfigService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author lustre
 * @since 2025/7/22 0:21
 */
public class EspIdfTargetBarBarWidgetFactory implements StatusBarWidgetFactory {

    private boolean available = false;

    private boolean widgetHasCreated = false;

    @Override
    public @NotNull @NonNls String getId() {
        return "esp_idf_target";
    }

    @Override
    public @NotNull @NlsContexts.ConfigurableName String getDisplayName() {
        return "Switch ESP-IDF Target";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        this.widgetHasCreated = true;
        return new EspIdfTargetBarWidget(project);
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        project.getService(IdfProjectConfigService.class).setStatusBarRefreshHook((isEspIdfProject)->{
            this.available = isEspIdfProject;
            if ((!widgetHasCreated && isEspIdfProject) || (widgetHasCreated && !isEspIdfProject)) {
                project.getService(StatusBarWidgetsManager.class).updateWidget(this);
            }
        });
        return available;
    }
}
