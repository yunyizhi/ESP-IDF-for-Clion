package org.btik.espidf.state;

import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.CMakeSettingsListener;
import org.btik.espidf.service.IdfProjectConfigService;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class CMakeStateListener implements CMakeSettingsListener {

    private final Project project;
    public CMakeStateListener(Project project) {
        this.project = project;
    }

    @Override
    public void profilesChanged(@NotNull List<CMakeSettings.Profile> old, @NotNull List<CMakeSettings.Profile> current) {
        CMakeSettingsListener.super.profilesChanged(old, current);
        project.getService(IdfProjectConfigService.class).onProfileChanged();
    }
}
