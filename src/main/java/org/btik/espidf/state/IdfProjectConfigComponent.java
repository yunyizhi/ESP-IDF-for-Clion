package org.btik.espidf.state;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfProjectConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;

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

    @Override
    public String getCmakeBuildDir() {
        if (!StringUtil.isEmpty(idfProjectConfig.getBuildDir())){
            return idfProjectConfig.getBuildDir();
        }
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        List<CMakeSettings.Profile> activeProfiles = instance.getSettings().getActiveProfiles();
        if (activeProfiles.isEmpty()){
            return "build";
        }
        File generationDir = activeProfiles.get(0).getGenerationDir();
        if (generationDir == null){
            return "build";
        }
        return generationDir.getName();
    }

    @Override
    public List<String> listCmakeBuildDir() {
        return List.of();
    }
}
