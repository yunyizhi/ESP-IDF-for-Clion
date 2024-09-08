package org.btik.espidf.run.config;

import com.google.gson.Gson;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.btik.espidf.run.config.model.DebugConfigModel;
import org.btik.espidf.service.IdfEnvironmentService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.btik.espidf.service.IdfEnvironmentService.ESP_ROM_ELF_DIR;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/9/2 21:11
 */
public class EspIdfRunConfigFactory extends ConfigurationFactory {

    private final static Logger log = Logger.getInstance(EspIdfRunConfigFactory.class);

    public EspIdfRunConfigFactory(EspIdfRunConfigType espIdfRunConfigType) {
        super(espIdfRunConfigType);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        EspIdfRunConfig espIdfRunConfig = new EspIdfRunConfig(project, this);
        espIdfRunConfig.setConfigDataModel(syncProjectDesc(project));
        return espIdfRunConfig;
    }

    @Override
    public @NotNull @NonNls String getId() {
        return $sys("esp.idf.run.config.type.factory.id");
    }

    private static DebugConfigModel parseDesc(File descFile) {
        Gson gson = new Gson();
        String json;
        try {
            json = Files.readString(descFile.toPath());
        } catch (IOException e) {
            log.error(e);
            return null;
        }
        return gson.fromJson(json, DebugConfigModel.class);
    }

    private static File getProjectDescFile(Project project, final String projectDescFile) {
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        CMakeSettings settings = instance.getSettings();
        List<CMakeSettings.Profile> profiles = settings.getProfiles();
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        Path baseDir = Path.of(basePath);

        if (profiles.isEmpty()) {
            return checkDescFile(baseDir.resolve($sys("esp.idf.build.project.build.dir")), projectDescFile);
        }
        File resolve;
        for (CMakeSettings.Profile profile : profiles) {
            File generationDir = profile.getGenerationDir();
            if (generationDir != null && (resolve = checkDescFile(baseDir.resolve(generationDir.getName()), projectDescFile)) != null) {
                return resolve;
            }
        }
        return null;
    }

    private static File checkDescFile(Path buildDir, final String projectDescFile) {
        if (!Files.exists(buildDir)) {
            return null;
        }
        File projectDesc = buildDir.resolve(projectDescFile).toFile();
        return projectDesc.exists() && projectDesc.canRead() ? projectDesc : null;

    }

    public static DebugConfigModel syncProjectDesc(Project project) {
        String projectDescFileName = $sys("esp.idf.build.project.description");
        File projectDescFile = getProjectDescFile(project, projectDescFileName);
        if (projectDescFile == null) {
            return null;
        }
        DebugConfigModel debugConfigModel = parseDesc(projectDescFile);
        if (debugConfigModel == null) {
            return null;
        }
        IdfEnvironmentService idfEnvironmentService = project.getService(IdfEnvironmentService.class);
        Map<String, String> environments = idfEnvironmentService.getEnvironments();
        String romElfDir = environments.get(ESP_ROM_ELF_DIR);
        debugConfigModel.setRomElfDir(romElfDir);
        return debugConfigModel;
    }
}
