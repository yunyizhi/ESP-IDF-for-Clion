package org.btik.espidf.run.config.build;

import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.execution.CidrBuildConfigurationHelper;
import org.btik.espidf.service.IdfEnvironmentService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author lustre
 * @since 2024/9/3 0:28
 */
public class EspIdfBuildConfHelper extends CidrBuildConfigurationHelper<EspIdfBuildConf, EspIdfBuildTarget> {

    private final Project project;

    public EspIdfBuildConfHelper(Project project) {
        this.project = project;
    }

    @Override
    public @NotNull List<EspIdfBuildTarget> getTargets() {
        return project.getService(IdfEnvironmentService.class).getBuildTargets();
    }
}
