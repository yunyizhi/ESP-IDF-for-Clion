package org.bitk.espidf.service;

import org.bitk.espidf.conf.IdfToolConf;

/**
 * @author lustre
 * @since 2024/2/15 13:37
 */
public interface IdfToolConfService {
    IdfToolConf getIdfToolConf();

    /**
     * 仅windows
     * */
    IdfToolConf createWinToolConf(String idfToolPath, String idfId);
}
