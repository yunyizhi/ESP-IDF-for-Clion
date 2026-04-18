package org.btik.espidf.project.generator.toolchain;

import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class IdfToolchain extends IdfInfo {
    private final CPPToolchains.Toolchain toolchain;
    private final Map<String, String> env;

    public IdfToolchain(CPPToolchains.Toolchain toolchain, String idfVersion, String idfPath, String idfToolsPath, Map<String, String> env) {
        super(toolchain.getName(), idfVersion, idfPath, idfToolsPath);
        this.toolchain = toolchain;
        this.env = env;
    }

    @Override
    public @NotNull String toString() {
        return getName();
    }

    public CPPToolchains.Toolchain getToolchain() {
        return toolchain;
    }

    public Map<String, String> getEnv() {
        return env;
    }
}
