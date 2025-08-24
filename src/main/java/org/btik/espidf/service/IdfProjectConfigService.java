package org.btik.espidf.service;

import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.state.model.IdfProfileInfo;

import java.util.List;

/**
 * @author lustre
 * @since 2024/8/25 17:41
 */
public interface IdfProjectConfigService {
    String PORT_CONF_AUTO = "Auto";

    void updateProjectConfig(IdfProjectConfig idfToolConf);

    boolean hasValueChange(IdfProjectConfig viewObj);

    IdfProjectConfig getProjectConfig();

    String getCmakeBuildDir();

    void addProfileChangeListener(Runnable callback);

    void onProfileChanged();

    IdfProfileInfo getIdfProfileInfo(String profileName);

    IdfProfileInfo getFirstIdfProjectConfig();

    List<IdfProfileInfo> getCurrentIdfProjectConfig();

    void updateProfile(String displayName);
}
