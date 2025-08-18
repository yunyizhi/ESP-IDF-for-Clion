package org.btik.espidf.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeProfileInfo;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.util.EspIdfProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

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

    private final Set<Runnable> profileChangeListeners = new HashSet<>();
    /*
     * 是否由ESPIDF插件创建，不需要持久化
     * */
    private boolean createByEspIdf = false;

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
    public void addProfileChangeListener(Runnable callback) {
        profileChangeListeners.add(callback);
    }

    @Override
    public void onProfileChanged() {
        for (Runnable profileChangeListener : profileChangeListeners) {
            ApplicationManager.getApplication().invokeLater(profileChangeListener);
        }
    }

    @Override
    public boolean isCreateByEspIdf() {
        return createByEspIdf;
    }

    @Override
    public void setCreateByEspIdf(boolean createByEspIdf) {
        this.createByEspIdf = createByEspIdf;
    }

    @Override
    public void updateProfile(String displayName) {
        idfProjectConfig.setCmakeProfile(displayName);
    }
}
