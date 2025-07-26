package org.btik.espidf.state;

import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspaceListener;
import org.btik.espidf.service.IdfProjectConfigService;


public class EspIdfCMakeStatusListener implements CMakeWorkspaceListener {
    private final Project project;

    public EspIdfCMakeStatusListener(Project project) {
        this.project = project;
    }

    @Override
    public void reloadingFinished(boolean canceled) {
        CMakeWorkspaceListener.super.reloadingFinished(canceled);
        project.getService(IdfProjectConfigService.class).onProfileChanged();
    }

    @Override
    public void reloadingScheduled() {
        CMakeWorkspaceListener.super.reloadingScheduled();
        project.getService(IdfProjectConfigService.class).onProfileChanged();
    }


}
