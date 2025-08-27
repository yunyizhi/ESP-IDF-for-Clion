package org.btik.espidf.service;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.util.Consumer;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author lustre
 * @since 2024/8/25 17:41
 */
public interface IdfProjectConfigService {
    enum ProfileChangeType{
        PROFILE_CHANGE,
        PROFILE_SELECT_CHANGE
    }
    String PORT_CONF_AUTO = "Auto";

    void updateProjectConfig(IdfProjectConfig idfToolConf);

    boolean hasValueChange(IdfProjectConfig viewObj);

    IdfProjectConfig getProjectConfig();

    String getCmakeBuildDir();


    void addProfileChangeListener(String id, Consumer<ProfileChangeType> callback);

    void onProfileSelectChanged();

    void onProfileChanged();

    IdfProfileInfo getIdfProfileInfo(String profileName);

    IdfProfileInfo getFirstIdfProjectConfig();

    List<IdfProfileInfo> getCurrentIdfProjectConfig();

    void updateProfile(String displayName);

    RunLineMarkerContributor.Info getRunInfo(@NotNull String name);

    void putRunInfo(@NotNull String name, RunLineMarkerContributor.Info info);
}
