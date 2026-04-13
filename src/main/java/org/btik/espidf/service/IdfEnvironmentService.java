package org.btik.espidf.service;


import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.jetbrains.annotations.NotNull;


import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author lustre
 * @since 2024/2/18 17:33
 */
public interface IdfEnvironmentService {
    String ENV_FILE_PREFIX = "export_";

    String IDF_TOOLCHAIN_NAME_PREFIX = "EspIdfAutoGen";

    String PATH = "PATH";

    String ESP_PORT = "ESPPORT";
    String IDF_MONITOR_BAUD = "IDF_MONITOR_BAUD";
    String MONITOR_BAUD = "MONITORBAUD";
    String ESP_BAUD = "ESPBAUD";

    String ESP_ROM_ELF_DIR = "ESP_ROM_ELF_DIR";

    String OPENOCD_COMMANDS = "OPENOCD_COMMANDS";

    String ESP_IDF_VERSION = "ESP_IDF_VERSION";

    String IDF_PATH = "IDF_PATH";

    String SRC_TOOLS_DIR = "tools"; // idf源码目录的tools

    String IDF_TOOLS_PATH = "IDF_TOOLS_PATH";

    String DEFAULT_IDF_TOOLS_PATH = System.getProperty("user.home") + File.separator + ".espressif";

    CPPToolchains.Toolchain getToolChianOfCheckedProfile();

    Map<String, String> getEnvironments();

    Map<String, String> getEnvOfToolChain(CPPToolchains.Toolchain toolchain);

    Map<String, String> setCache(@NotNull CPPToolchains.Toolchain toolchain, @NotNull Map<String, String> env);

    void putTo(Map<String, String> newEnvironments);

    CPPToolchains.Toolchain getSourceToolConf(String idfPath, String idfToolsPath);

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
