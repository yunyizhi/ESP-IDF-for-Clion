package org.btik.espidf.state;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.platform.ide.progress.TasksKt;
import com.intellij.ui.ClickListener;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.run.config.model.DebugConfigModel;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.EnvironmentVarUtil;
import org.btik.espidf.util.EspIdfProjectUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Map;

import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2025/7/22
 */
public class EspIdfTargetBarWidget extends EditorBasedWidget implements StatusBarWidget.Multiframe, CustomStatusBarWidget {

    private final TextPanel.WithIconAndArrows myComponent;

    private final Project project;

    public EspIdfTargetBarWidget(Project project) {
        super(project);
        this.project = project;
        myComponent = new TextPanel.WithIconAndArrows();
        myComponent.setBorder(JBUI.CurrentTheme.StatusBar.Widget.border());
        myComponent.setIcon(EspIdfIcon.IDF_16_16);
        IdfProjectConfigService service = project.getService(IdfProjectConfigService.class);
        service.addProfileChangeListener(this::update);
    }

    @Override
    public @NotNull @NonNls String ID() {
        return "esp_idf_cmake_swicth";
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        super.install(statusBar);

        myComponent.setToolTipText("ESP-IDF Target");
        new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent event, int clickCount) {
                showSelectBuildTypePopup();
                return true;
            }
        }.installOn(myComponent, true);
        ApplicationManager.getApplication().invokeLater(this::update);
    }


    private void showSelectBuildTypePopup() {
        GeneralCommandLine listTarget = new GeneralCommandLine();
        IdfEnvironmentService idfEnvironmentService = project.getService(IdfEnvironmentService.class);
        Map<String, String> environments = idfEnvironmentService.getEnvironments();
        listTarget.withEnvironment(environments);
        listTarget.setExePath(EnvironmentVarUtil.findIdfFullPath(environments));
        listTarget.addParameters("--list-targets", "--preview");
        String targets = TasksKt.runWithModalProgressBlocking(project, "Loading Targets", (scope, continuation) -> CmdTaskExecutor.exeGetStdOut(listTarget, 60 * 1000));
        String[] targetsArray;
        if (StringUtils.isEmpty(targets)) {
            targetsArray = $sys("idf.targets.last").split(",");
        } else {
            targetsArray = targets.split("\n");
        }
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (String target : targetsArray) {
            actionGroup.add(new CheckBuildTypeAction(target, target, this::update));
        }
        JComponent component = getComponent();
        DataContext dataContext = DataManager.getInstance().getDataContext(component);
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup("Select Target",
                actionGroup, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
        RelativePoint pos = JBPopupFactory.getInstance().guessBestPopupLocation(component);
        popup.showInScreenCoordinates(component, pos.getScreenPoint());
    }


    @NotNull
    @Override
    public StatusBarWidget copy() {
        return new EspIdfTargetBarWidget(getProject());
    }

    @Override
    public JComponent getComponent() {
        return myComponent;
    }


    public void update() {
        DebugConfigModel debugConfigModel = EspIdfProjectUtil.syncProjectDesc(project);
        if (debugConfigModel == null) {
            return;
        }
        myComponent.setText(debugConfigModel.getTarget());
    }
}
