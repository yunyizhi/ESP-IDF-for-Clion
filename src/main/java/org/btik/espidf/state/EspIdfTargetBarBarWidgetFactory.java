package org.btik.espidf.state;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lustre
 * @since 2025/7/22 0:21
 */
public class EspIdfTargetBarBarWidgetFactory implements StatusBarWidgetFactory {

    private final ConcurrentHashMap<String,Boolean> availableMap = new ConcurrentHashMap<>();

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
        return new EspIdfTargetBarWidget(project, this);
    }

    public void setAvailable(String id, boolean isEspIdfProject) {
        this.availableMap.put(id, isEspIdfProject);
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return false;
        }
        Boolean b = availableMap.get(basePath);
        if (b == null) {
            availableMap.put(basePath, true);
            return true;
        }
        return b;
    }
}
