package org.btik.espidf.conf;

import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;

import java.util.Objects;

import static org.btik.espidf.util.OsUtil.IS_WINDOWS;

/**
 * @author lustre
 * @since 2024/2/14 23:20
 */
public class IdfToolConf {
    private transient CPPToolchains.Toolchain toolchain;

    private transient String key;
    private String envFileName;

    /**
     * 仅windows
     */
    private String idfToolPath;

    /**
     * 仅windows
     */
    private String idfId;

    private long activeTime = 0;

    public CPPToolchains.Toolchain getToolchain() {
        return toolchain;
    }

    public void setToolchain(CPPToolchains.Toolchain toolchain) {
        this.toolchain = toolchain;
    }

    public String getEnvFileName() {
        return envFileName;
    }

    public void setEnvFileName(String envFileName) {
        this.envFileName = envFileName;
    }

    public String getIdfToolPath() {
        return idfToolPath;
    }

    public void setIdfToolPath(String idfToolPath) {
        this.idfToolPath = idfToolPath;
    }

    public String getIdfId() {
        return idfId;
    }

    public void setIdfId(String idfId) {
        this.idfId = idfId;
    }

    public long getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }

    public String getKey() {
        if (key != null) {
            return key;
        }
        if (IS_WINDOWS) {
            key = idfToolPath + idfId;
        } else {
            key = idfToolPath;
        }
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdfToolConf that)) return false;
        return getActiveTime() == that.getActiveTime() && Objects.equals(getEnvFileName(), that.getEnvFileName()) && Objects.equals(getIdfToolPath(), that.getIdfToolPath()) && Objects.equals(getIdfId(), that.getIdfId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEnvFileName(), getIdfToolPath(), getIdfId(), getActiveTime());
    }
}
