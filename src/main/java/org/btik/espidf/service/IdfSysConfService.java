package org.btik.espidf.service;

import com.intellij.openapi.project.Project;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.toolwindow.settings.model.CdcAcmVendorInfo;
import org.btik.espidf.util.ClassMetaUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * @author lustre
 * @since 2024/2/15 13:37
 */
public interface IdfSysConfService {
    String WINDOWS = "windows";
    String UNIX_LIKE = "unixLike";
    String DEFAULT_GDB = "default";

    String MONITOR_COMMAND = "monitor";

    IdfToolConf getLastActivedIdfToolConf();

    void store(IdfToolConf newIdfToolConf);

    Path getIdfConfFolder();

    IdfToolConf getToolConfByKey(String key);

    IdfToolConf getIdfConfByProject(Project project);

    String getGdbExecutable(String target);

    List<ClassMetaUtils.PropOptMeta> getPropOptMetas(Class<?> clazz);

    CdcAcmVendorInfo getCdcAcmVendorInfo(int vendorId, int productId);
}
