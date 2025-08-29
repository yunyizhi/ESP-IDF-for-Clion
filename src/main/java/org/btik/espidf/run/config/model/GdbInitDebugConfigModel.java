package org.btik.espidf.run.config.model;


import com.intellij.execution.configuration.EnvironmentVariablesData;

public class GdbInitDebugConfigModel {
    @Serial
    private String target;

    @Serial
    private String openOcdArguments;

    @Serial
    private String path;

    @Serial
    private String gdbExe;

    @Serial
    private EnvironmentVariablesData envData = EnvironmentVariablesData.DEFAULT;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getOpenOcdArguments() {
        return openOcdArguments;
    }

    public void setOpenOcdArguments(String openOcdArguments) {
        this.openOcdArguments = openOcdArguments;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getGdbExe() {
        return gdbExe;
    }

    public void setGdbExe(String gdbExe) {
        this.gdbExe = gdbExe;
    }

    public EnvironmentVariablesData getEnvData() {
        return envData;
    }

    public void setEnvData(EnvironmentVariablesData envData) {
        this.envData = envData;
    }
}
