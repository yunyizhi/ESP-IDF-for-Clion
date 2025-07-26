package org.btik.espidf.run.config;

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

    @Override
    public @NotNull @NonNls String getId() {
        return "espidf_cmake";
    }

    @Override
    public @NotNull @NlsContexts.ConfigurableName String getDisplayName() {
        return "";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new EspIdfTargetBarWidget(project);
    }
}
