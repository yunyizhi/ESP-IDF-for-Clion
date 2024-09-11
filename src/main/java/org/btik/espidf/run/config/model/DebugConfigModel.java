package org.btik.espidf.run.config.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.execution.configuration.EnvironmentVariablesData;

/**
 * @author lustre
 * @since 2024/9/7 11:37
 */
public class DebugConfigModel {

    @Serial
    @SerializedName("target")
    private String target;

    @Serial
    @SerializedName("bootloader_elf")
    private String bootloaderElf;

    @Serial
    @SerializedName("app_elf")
    private String appElf;

    @Expose(serialize = false, deserialize = false)
    @Serial
    private String openOcdArguments;

    @Expose(serialize = false, deserialize = false)
    @Serial
    private String gdbExe;

    @Expose(serialize = false, deserialize = false)
    private String romElfDir;

    @Expose(serialize = false, deserialize = false)
    @Serial
    private String romElf;

    @Serial
    private EnvironmentVariablesData envData = EnvironmentVariablesData.DEFAULT;

    private boolean createByFactory = false;

    public String getOpenOcdArguments() {
        return openOcdArguments;
    }

    public void setOpenOcdArguments(String openOcdArguments) {
        this.openOcdArguments = openOcdArguments;
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

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getBootloaderElf() {
        return bootloaderElf;
    }

    public void setBootloaderElf(String bootloaderElf) {
        this.bootloaderElf = bootloaderElf;
    }

    public String getAppElf() {
        return appElf;
    }

    public void setAppElf(String appElf) {
        this.appElf = appElf;
    }

    public String getRomElfDir() {
        return romElfDir;
    }

    public void setRomElfDir(String romElfDir) {
        this.romElfDir = romElfDir;
    }

    public String getRomElf() {
        return romElf;
    }

    public void setRomElf(String romElf) {
        this.romElf = romElf;
    }

    public boolean isCreateByFactory() {
        return createByFactory;
    }

    public void setCreateByFactory(boolean createByFactory) {
        this.createByFactory = createByFactory;
    }
}
