package org.bitk.espidf.conf;

import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;

/**
 * @author lustre
 * @since 2024/2/14 23:20
 */
public class IdfToolConf {
   private transient CPPToolchains.Toolchain toolchain;
   private String envFileName;

   /**
    * 仅windows
    * */
   private String idfToolPath;

    /**
     * 仅windows
     * */
   private String idfId;

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
}
