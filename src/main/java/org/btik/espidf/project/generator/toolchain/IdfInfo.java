package org.btik.espidf.project.generator.toolchain;

import com.google.gson.annotations.SerializedName;

public class IdfInfo {
    private String name;
    private String idfVersion;
    @SerializedName("path")
    private String idfPath;
    @SerializedName("idfToolsPath")
    private String idfToolsPath;
    private String adfPath;
    @SerializedName("activationScript")
    private String envFile;

    public IdfInfo() {
    }

    public IdfInfo(String name, String idfVersion, String idfPath, String idfToolsPath) {
        this.name = name;
        this.idfVersion = idfVersion;
        this.idfPath = idfPath;
        this.idfToolsPath = idfToolsPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdfVersion() {
        return idfVersion;
    }

    public void setIdfVersion(String idfVersion) {
        this.idfVersion = idfVersion;
    }

    public String getIdfPath() {
        return idfPath;
    }

    public void setIdfPath(String idfPath) {
        this.idfPath = idfPath;
    }

    public String getIdfToolsPath() {
        return idfToolsPath;
    }

    public void setIdfToolsPath(String idfToolsPath) {
        this.idfToolsPath = idfToolsPath;
    }

    public String getAdfPath() {
        return adfPath;
    }

    public void setAdfPath(String adfPath) {
        this.adfPath = adfPath;
    }

    public String getEnvFile() {
        return envFile;
    }

    public void setEnvFile(String envFile) {
        this.envFile = envFile;
    }
}
