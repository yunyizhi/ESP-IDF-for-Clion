package org.btik.espidf.run.config;

import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.btik.espidf.run.config.build.EspIdfExecTarget;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EspIdfExecutionTargetProvider extends ExecutionTargetProvider {
    @Override
    public List<ExecutionTarget> getTargets(@NotNull Project project, @NotNull RunConfiguration configuration) {
        if (!(configuration instanceof EspIdfDebugRunConfig<?>)) {
            return Collections.singletonList(DefaultExecutionTarget.INSTANCE);
        }
        IdfProjectConfigService idfProjectConfigService = project.getService(IdfProjectConfigService.class);
        List<IdfProfileInfo> idfProfileInfoList = idfProjectConfigService.getIdfProfileInfoList();
        if (idfProfileInfoList.isEmpty()) {
            idfProjectConfigService.onProfileChanged();
            idfProfileInfoList = idfProjectConfigService.getIdfProfileInfoList();
        }
        List<ExecutionTarget> targets = new ArrayList<>();
        for (IdfProfileInfo idfProfileInfo : idfProfileInfoList) {
            var espIdfExecTarget = new EspIdfExecTarget(idfProfileInfo.getDisplayName());
            targets.add(espIdfExecTarget);
        }

        return targets;
    }
}
