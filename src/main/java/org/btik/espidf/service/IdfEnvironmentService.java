package org.btik.espidf.service;

import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.run.config.build.EspIdfBuildTarget;

import java.util.List;
import java.util.Map;

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

    Map<String, String> getEnvironments();

    String getEnvironmentFile();

    /**
     * 仅windows
     * */
    IdfToolConf getWinToolConf(String idfToolPath, String idfId);

    IdfToolConf getSourceToolConf(String idfFrameworkPath);

    List<EspIdfBuildTarget> getBuildTargets();

    CPPToolchains.Toolchain getCurrentToolchain();
}
