package org.btik.espidf.service;


import org.btik.espidf.conf.IdfToolConf;


import java.util.Map;
import java.util.function.Consumer;

/**
 * @author lustre
 * @since 2024/2/18 17:33
 */
public interface IdfEnvironmentService {
    String ENV_FILE_PREFIX = "export_";

    String IDF_TOOLCHAIN_NAME_PREFIX = "EspIdfAutoGen";

    String ESP_PORT = "ESPPORT";
    String IDF_MONITOR_BAUD = "IDF_MONITOR_BAUD";
    String MONITOR_BAUD = "MONITORBAUD";
    String ESP_BAUD = "ESPBAUD";

    String ESP_ROM_ELF_DIR = "ESP_ROM_ELF_DIR";

    String OPENOCD_COMMANDS = "OPENOCD_COMMANDS";

    Map<String, String> getEnvironments();

    void putTo(Map<String, String> newEnvironments);

    /**
     * 仅windows
     */
    IdfToolConf getWinToolConf(String idfToolPath, String idfId);

    IdfToolConf getSourceToolConf(String idfFrameworkPath);

    /**
     * 构建环境遍历缓存，在初始化或者Toolchain发生变化后调用，会覆盖已有缓存
     *
     */
    void buildEnvironmentsCache();

    /**
     * 补全未被建缓存的Toolchain环境变量缓存，不修改已有缓存
     *
     */
    void fixEnvironmentsCache();

    void checkEnvNeedRebuild();

    void register(Consumer<Boolean> floatingToolbarVisibleHandler);

    void setFloatingToolbarVisible(boolean visible);
}
