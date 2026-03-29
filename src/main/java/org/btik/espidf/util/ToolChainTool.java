package org.btik.espidf.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ToolChainTool {

    public static CPPToolchains.Toolchain findToolchainByEnvFile(String envFileName) {
        return ApplicationManager.getApplication().runReadAction((Computable<CPPToolchains.Toolchain>)
                () -> CPPToolchains.getInstance().getToolchains().stream()
                        .filter((toolchain -> Objects.equals(toolchain.getEnvironment(), envFileName)))
                        .findFirst()
                        .orElse(null));
    }

    public static List<CPPToolchains.Toolchain> getFilteredToolchains(Predicate<CPPToolchains.Toolchain> filter) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<CPPToolchains.Toolchain>>)
                () -> CPPToolchains.getInstance().getToolchains().stream()
                        .filter(filter).collect(Collectors.toList())
        );
    }
}
