package org.btik.espidf.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.btik.espidf.toolwindow.kconfig.EspIdfMenuConfigPanel;
import org.jetbrains.annotations.NotNull;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/18 12:57
 */
public class EspIdfToolWindowFactory implements ToolWindowFactory {


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        ContentManager contentManager = toolWindow.getContentManager();
        Content taskContent = contentFactory.createContent(new EspIdfToolWindowTaskPanel(project), $i18n("esp.idf.tool.window.tasks"), false);
        contentManager.addContent(taskContent);
        Content setttingsContent = contentFactory.createContent(new EspIdfToolWindowSettingPanel(project), $i18n("esp.idf.tool.window.settings"), false);
        contentManager.addContent(setttingsContent);
    }
}
