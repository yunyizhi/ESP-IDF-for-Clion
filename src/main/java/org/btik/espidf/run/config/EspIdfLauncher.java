package org.btik.espidf.run.config;

import com.intellij.execution.runners.ExecutionEnvironment;
import com.jetbrains.cidr.cpp.execution.CLionLauncher;
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author lustre
 * @since 2024/9/2 23:05
 */
public class EspIdfLauncher extends CLionLauncher {
    public EspIdfLauncher(@NotNull ExecutionEnvironment executionEnvironment, @NotNull EspIdfRunConfig configuration) {
        super(executionEnvironment, configuration);
    }

    @Override
    public @NotNull Pair<File, CPPEnvironment> getRunFileAndEnvironment() {
        throw new UnsupportedOperationException();
    }

}
