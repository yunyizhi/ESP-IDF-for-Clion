package org.btik.espidf.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;

import java.util.List;
import java.util.Objects;

public class ToolChainTool {

    public static CPPToolchains.Toolchain findToolchainByEnvFile(String envFileName) {
        return ApplicationManager.getApplication().runReadAction((Computable<CPPToolchains.Toolchain>) () -> {
            List<CPPToolchains.Toolchain> toolchains = CPPToolchains.getInstance().getToolchains();
            for (CPPToolchains.Toolchain toolchain : toolchains) {
                if (Objects.equals(toolchain.getEnvironment(), envFileName)) {
                    return toolchain;
                }
            }
            return null;
        });
    }
}
