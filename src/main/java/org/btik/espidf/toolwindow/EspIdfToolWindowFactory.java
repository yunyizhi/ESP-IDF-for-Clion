package org.btik.espidf.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author lustre
 * @since 2024/2/18 12:57
 */
public class EspIdfToolWindowFactory implements ToolWindowFactory {

    String ESP_IDF_TASK_TREE = "Tasks";

    String PIO_HOME_SETTINGS_CONTENT_ID = "Settings";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory =  ApplicationManager.getApplication().getService(ContentFactory.class);
        Content taskContent = contentFactory.createContent(new EspIdfToolWindowTaskPanel(project), ESP_IDF_TASK_TREE, false);
        toolWindow.getContentManager().addContent(taskContent);

        Content setttingsContent = contentFactory.createContent(new EspIdfToolWindowSettingPanel(project), PIO_HOME_SETTINGS_CONTENT_ID, false);
        toolWindow.getContentManager().addContent(setttingsContent);
    }
}
