package org.btik.espidf.service;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.util.Consumer;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

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

    IdfProfileInfo getFirstIdfProfileInfo();

    IdfProfileInfo getSelectedIdfProfileInfo();

    List<IdfProfileInfo> getIdfProfileInfoList();

    void updateProfile(String displayName);

    RunLineMarkerContributor.Info getRunInfo(@NotNull String name);

    void putRunInfo(@NotNull String name, RunLineMarkerContributor.Info info);

    /**
     * 依据自定义任务文件的修改戳同步 line marker 缓存：
     * 当文件发生变化时，清除缓存中已不存在于文件的任务（避免任务被删除/重命名后缓存只增不减导致内存积压）。
     * 仅当 {@code modificationStamp} 与上次不同才会触发扫描，避免逐元素重复全量扫描。
     *
     * @param modificationStamp   自定义任务文件的当前修改戳
     * @param validNamesSupplier  惰性提供文件中当前仍然存在的任务名集合（仅在需要时调用）
     */
    void syncRunInfoCache(long modificationStamp, @NotNull Supplier<Set<String>> validNamesSupplier);
}
