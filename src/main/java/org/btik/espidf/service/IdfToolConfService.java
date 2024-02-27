package org.btik.espidf.service;

import org.btik.espidf.conf.IdfToolConf;

import java.nio.file.Path;

/**
 * @author lustre
 * @since 2024/2/15 13:37
 */
public interface IdfToolConfService {

    IdfToolConf getLastActivedIdfToolConf();


    void store(IdfToolConf newIdfToolConf);

    Path getIdfConfFolder();

    IdfToolConf getToolConfByKey(String key);
}
