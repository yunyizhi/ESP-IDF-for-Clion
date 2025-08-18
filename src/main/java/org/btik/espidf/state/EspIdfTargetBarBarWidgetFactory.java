package org.btik.espidf.state;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author lustre
 * @since 2025/7/22 0:21
 */
public class EspIdfTargetBarBarWidgetFactory implements StatusBarWidgetFactory {

    private boolean available = true;

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
        return new EspIdfTargetBarWidget(project, (isEspIdfProject) -> this.available = isEspIdfProject, this);
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return available;
    }
}
