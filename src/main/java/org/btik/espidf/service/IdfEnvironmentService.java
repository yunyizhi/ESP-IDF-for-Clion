package org.btik.espidf.service;

import org.btik.espidf.conf.IdfToolConf;

import java.util.Map;

/**
 * @author lustre
 * @since 2024/2/18 17:33
 */
public interface IdfEnvironmentService {
    String ENV_FILE_PREFIX = "export_";

    String IDF_TOOLCHAIN_NAME_PREFIX = "EspIdfAutoGen";

    Map<String, String> getEnvironments();

    String getEnvironmentFile();

    /**
     * 仅windows
     * */
    IdfToolConf getWinToolConf(String idfToolPath, String idfId);

    IdfToolConf getUnixToolConf(String idfFrameworkPath);
}
