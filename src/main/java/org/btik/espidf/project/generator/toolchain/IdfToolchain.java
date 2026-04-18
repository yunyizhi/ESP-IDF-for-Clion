package org.btik.espidf.project.generator.toolchain;

import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class IdfToolchain {
    private final CPPToolchains.Toolchain toolchain;
    private final String name;
    private final String idfVersion;
    private final String idfPath;
    private final String idfToolsPath;
    private final Map<String, String> env;
    private String adfPath;

    public IdfToolchain(CPPToolchains.Toolchain toolchain, String idfVersion, String idfPath, String idfToolsPath, Map<String, String> env) {
        this.toolchain = toolchain;
        this.name = toolchain.getName();
        this.idfVersion = idfVersion;
        this.idfPath = idfPath;
        this.idfToolsPath = idfToolsPath;
        this.env = env;
    }

    public String getName() {
        return name;
    }

    public String getIdfVersion() {
        return idfVersion;
    }

    public String getIdfPath() {
        return idfPath;
    }

    public String getIdfToolsPath() {
        return idfToolsPath;
    }

    @Override
    public @NotNull String toString() {
        return name;
    }

    public CPPToolchains.Toolchain getToolchain() {
        return toolchain;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public String getAdfPath() {
        return adfPath;
    }

    public void setAdfPath(String adfPath) {
        this.adfPath = adfPath;
    }
}
