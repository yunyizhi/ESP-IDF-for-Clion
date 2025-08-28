package org.btik.espidf.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.btik.espidf.service.IdfEnvironmentService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EspIdfProjectActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ApplicationManager.getApplication().invokeLater(() -> {
            IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
            environmentService.buildEnvironmentsCache();
            environmentService.checkEnvNeedRebuild();
        });
        return null;
    }
}
