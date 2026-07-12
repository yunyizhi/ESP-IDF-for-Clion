package org.btik.espidf.project.generator.toolchain;

import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.btik.espidf.conf.IdfToolchainCacheEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class IdfToolchain extends IdfToolchainCacheEntry {
    private final CPPToolchains.Toolchain toolchain;

    public IdfToolchain(CPPToolchains.Toolchain toolchain, String idfVersion, String idfPath, String idfToolsPath, Map<String, String> env) {
        super(toolchain.getName(), idfVersion, idfPath, idfToolsPath);
        this.toolchain = toolchain;
        setEnv(env);
    }

    @Override
    public @NotNull String toString() {
        return getName();
    }

    public CPPToolchains.Toolchain getToolchain() {
        return toolchain;
    }
}
