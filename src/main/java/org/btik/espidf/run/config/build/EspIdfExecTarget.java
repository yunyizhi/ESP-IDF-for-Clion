package org.btik.espidf.run.config.build;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.configurations.RunConfiguration;
import org.btik.espidf.run.config.EspIdfDebugRunConfig;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EspIdfExecTarget extends ExecutionTarget {
    private final String profileName;
    public EspIdfExecTarget(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "org.bitk.EspIdfExecTarget" + profileName;
    }

    @Override
    public @NotNull @Nls String getDisplayName() {
        return profileName;
    }

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }

    @Override
    public boolean canRun(@NotNull RunConfiguration configuration) {
        return configuration instanceof EspIdfDebugRunConfig<?>;
    }
}
