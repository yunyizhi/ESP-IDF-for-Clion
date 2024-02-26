package org.btik.espidf.environment.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.btik.espidf.environment.IdfEnvironmentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.btik.espidf.adapter.Adapter.readEnvironment;

/**
 * @author lustre
 * @since 2024/2/18 17:34
 */
public class IdfEnvironmentServiceImpl implements IdfEnvironmentService {
    private Map<String, String> environments;

    private String environmentFile;
    private final Project project;

    public IdfEnvironmentServiceImpl(Project project) {
        this.project = project;
    }

    @Override
    public Map<String, String> getEnvironments() {
        if (environments == null) {
            generateEnvironment();
        }
        if (environments == null) {
            return Map.of();
        }
        return environments;
    }

    @Override
    public String getEnvironmentFile() {
        if (environmentFile == null) {
            generateEnvironment();
        }
        if (environmentFile == null) {
            return "";
        }
        return environmentFile;
    }

    private void generateEnvironment() {
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        List<CMakeSettings.Profile> activeProfiles = instance.getSettings().getActiveProfiles();
        if (activeProfiles.isEmpty()) {
            return;
        }
        CMakeSettings.Profile currentProfile = activeProfiles.get(0);
        CPPToolchains.Toolchain toolchain = CPPToolchains.getInstance()
                .getToolchainByNameOrDefault(currentProfile.getToolchainName());
        if (toolchain == null) {
            return;
        }
        String environment = toolchain.getEnvironment();
        if (StringUtil.isEmpty(environment)) {
            return;
        }
        try {
            environments = ApplicationManager.getApplication()
                    .executeOnPooledThread(() -> readEnvironment(toolchain, environment)).get();
            if(environment.endsWith(".bat")){
                environmentFile = environment.substring(0, environment.length() - 4) + ".ps1";
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }


    }
}
