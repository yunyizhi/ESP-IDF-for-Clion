package org.btik.espidf.state;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeProfileInfo;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.btik.espidf.util.CollectionUtils;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.btik.espidf.util.EspIdfProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author lustre
 * @since 2024/8/25 17:38
 */
@State(
        name = "ESP-IDF_SETTINGS"
        , storages = {@Storage("espidf_settings.xml")}
)
public class IdfProjectConfigComponent implements PersistentStateComponent<IdfProjectConfig>, IdfProjectConfigService {
    private final IdfProjectConfig idfProjectConfig = new IdfProjectConfig();
    private final Project project;

    private final HashMap<String, Consumer<ProfileChangeType>> profileChangeListeners = new HashMap<>();

    private final HashMap<String, IdfProfileInfo> idfProfileInfoMap = new HashMap<>();
    private List<IdfProfileInfo> currentInfo;

    private final Map<String, RunLineMarkerContributor.Info> lineMarkerRunInfoCache = new ConcurrentHashMap<>();

    /** 上次同步 line marker 缓存时自定义任务文件的修改戳，用于避免逐元素重复全量扫描。 */
    private volatile long lineMarkerRunInfoCacheStamp = -1;

    public IdfProjectConfigComponent(Project project) {
        this.project = project;

    }

    @Override
    public @Nullable IdfProjectConfig getState() {
        return idfProjectConfig;
    }

    @Override
    public void loadState(@NotNull IdfProjectConfig idfProjectConfig) {
        XmlSerializerUtil.copyBean(idfProjectConfig, this.idfProjectConfig);
    }

    @Override
    public void updateProjectConfig(IdfProjectConfig idfProjectConfig) {
        if (!Objects.equals(idfProjectConfig.getCmakeProfile(), this.idfProjectConfig.getCmakeProfile())) {
            onProfileSelectChanged();
        }
        XmlSerializerUtil.copyBean(idfProjectConfig, this.idfProjectConfig);
    }

    @Override
    public boolean hasValueChange(IdfProjectConfig viewObj) {
        return !Objects.equals(viewObj, idfProjectConfig);
    }

    @Override
    public IdfProjectConfig getProjectConfig() {
        return XmlSerializerUtil.createCopy(idfProjectConfig);
    }

    private String getFirstBuildDir(CMakeWorkspace workspace) {
        List<CMakeSettings.Profile> activeProfiles = workspace.getSettings().getActiveProfiles();
        if (activeProfiles.isEmpty()) {
            return "build";
        }
        CMakeSettings.Profile profile = activeProfiles.get(0);
        String buildOutDir = EspIdfProjectUtil.getBuildOutDir(project, profile);
        if (StringUtil.isNotEmpty(buildOutDir)) {
            return buildOutDir;
        }
        return "build";
    }

    @Override
    public String getCmakeBuildDir() {
        String cmakeProfile = idfProjectConfig.getCmakeProfile();
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        if (StringUtil.isEmpty(cmakeProfile)) {
            return getFirstBuildDir(instance);
        }
        CMakeProfileInfo cMakeProfileInfoByName = instance.getCMakeProfileInfoByName(cmakeProfile);
        if (cMakeProfileInfoByName == null) {
            return getFirstBuildDir(instance);
        }
        File generationDir = cMakeProfileInfoByName.getGenerationDir();
        return generationDir.getName();
    }

    @Override
    public void addProfileChangeListener(String id,Consumer<ProfileChangeType> callback) {
        profileChangeListeners.put(id, callback);
    }

    @Override
    public void onProfileSelectChanged() {
        profileChangeListeners.forEach((id, callback) ->
                ApplicationManager.getApplication().invokeLater(() -> callback.accept(ProfileChangeType.PROFILE_SELECT_CHANGE))
        );
    }

    @Override
    public void onProfileChanged() {
        updateIdfProfiles();
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        environmentService.checkEnvNeedRebuild();
        profileChangeListeners.forEach( (id, callback) ->
                ApplicationManager.getApplication().invokeLater(() -> callback.accept(ProfileChangeType.PROFILE_CHANGE))
        );
    }

    @Override
    public IdfProfileInfo getIdfProfileInfo(String profileName) {
        return idfProfileInfoMap.get(profileName);
    }

    @Override
    public IdfProfileInfo getFirstIdfProfileInfo() {
        if (CollectionUtils.isEmpty(currentInfo)) {
            return null;
        }
        return currentInfo.get(0);
    }

    @Override
    public IdfProfileInfo getSelectedIdfProfileInfo() {
        String cmakeProfile = idfProjectConfig.getCmakeProfile();
        if (StringUtil.isEmpty(cmakeProfile)) {
            return getFirstIdfProfileInfo();
        }
        return idfProfileInfoMap.get(cmakeProfile);
    }


    @Override
    public List<IdfProfileInfo> getIdfProfileInfoList() {
        return currentInfo == null ? List.of() : currentInfo;
    }

    private void updateIdfProfiles() {
        currentInfo = EspIdfProjectUtil.getIdfProfiles(project, this);
        idfProfileInfoMap.clear();
        if (CollectionUtils.isEmpty(currentInfo)) {
            return;
        }
        for (IdfProfileInfo idfProfileInfo : currentInfo) {
            idfProfileInfoMap.put(idfProfileInfo.getDisplayName(), idfProfileInfo);
        }
    }

    @Override
    public void updateProfile(String displayName) {
        if (!Objects.equals(displayName, idfProjectConfig.getCmakeProfile())) {
            onProfileSelectChanged();
        }
        idfProjectConfig.setCmakeProfile(displayName);
    }

    @Override
    public RunLineMarkerContributor.Info getRunInfo(@NotNull String name) {
        return lineMarkerRunInfoCache.get(name);
    }

    @Override
    public void putRunInfo(@NotNull String name, RunLineMarkerContributor.Info info) {
        lineMarkerRunInfoCache.put(name, info);
    }

    @Override
    public void syncRunInfoCache(long modificationStamp, @NotNull Supplier<Set<String>> validNamesSupplier) {
        // 文件未变化则无需重新扫描（getSlowInfo 会对每个元素调用，靠修改戳去重）
        if (modificationStamp == lineMarkerRunInfoCacheStamp) {
            return;
        }
        lineMarkerRunInfoCacheStamp = modificationStamp;
        Set<String> validNames = validNamesSupplier.get();
        // 清除文件中已不存在的任务缓存，防止删除/重命名任务后缓存只增不减
        lineMarkerRunInfoCache.keySet().removeIf(name -> !validNames.contains(name));
    }

}
