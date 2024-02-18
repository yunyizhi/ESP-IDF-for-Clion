package org.btik.espidf.project;


import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;

import java.io.File;
import java.util.List;

/**
 * @author lustre
 * @since 2024/2/11 17:18
 */
public interface SubGenerator<T> {
    String IDF_CMAKE_PROFILE_NAME = "idf";

    String IDF_CMAKE_BUILD_DIR = "build";
    ValidationResult validate();

    void generateProject(Project project, VirtualFile baseDir, T settings, Module module);

    default void loadCMakeProject(Project project,File cmakeProject, String toolChainName) {
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        List<CMakeSettings.Profile> profiles = instance.getSettings().getProfiles();
        if (!profiles.isEmpty()) {
            CMakeSettings.Profile profile = profiles.get(0);
            instance.getSettings().setProfiles(List.of(profile
                    .withToolchainName(toolChainName)
                    .withName(IDF_CMAKE_PROFILE_NAME)
                    .withGenerationDir(new File(IDF_CMAKE_BUILD_DIR))));
        }
        instance.linkCMakeProject(cmakeProject);
    }
}
