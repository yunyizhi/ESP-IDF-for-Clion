package org.btik.espidf.state;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.btik.espidf.service.IdfProjectConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EspIdfProjectManagerListener implements ProjectActivity {

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        project.getService(IdfProjectConfigService.class).onProfileChanged();
        return null;
    }
}
