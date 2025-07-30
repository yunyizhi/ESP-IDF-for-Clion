package org.btik.espidf.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeProfileInfo;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.run.config.model.DebugConfigModel;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.service.IdfEnvironmentService.ESP_ROM_ELF_DIR;
import static org.btik.espidf.util.SysConf.$sys;

public class EspIdfProjectUtil {
    private final static Logger log = Logger.getInstance(EspIdfProjectUtil.class);
    private static final String PROJECT_DESC_FILE_NAME = $sys("esp.idf.build.project.description");
    private static final String IDF_PATH = $sys("esp.idf.build.idf.path");
    private static final String DEFAULT_CMAKE_BUILD_DIR_PREFIX = "cmake-build-";

    public static boolean isEspIdfProject(Project project) {
        boolean createByEspIdf = project.getService(IdfProjectConfigService.class).isCreateByEspIdf();
        if (createByEspIdf) {
            return true;
        }
        String basePath = project.getBasePath();

        if (basePath == null) {
            return false;
        }
        Path baseDir = Path.of(basePath);
        List<CMakeSettings.Profile> activeProfiles = CMakeSettings.getInstance(project).getActiveProfiles();
        Gson gson = new Gson();
        for (CMakeSettings.Profile activeProfile : activeProfiles) {
            String buildOutDir = EspIdfProjectUtil.getBuildOutDir(project.getBasePath(), activeProfile);
            if (StringUtils.isEmpty(buildOutDir)) {
                continue;
            }
            File descFile = checkDescFile(baseDir.resolve(buildOutDir), PROJECT_DESC_FILE_NAME);
            if (descFile == null) {
                continue;
            }
            try (FileReader fileReader = new FileReader(descFile);
                 JsonReader jsonReader = new JsonReader(fileReader)) {
                JsonElement jsonElement = gson.fromJson(jsonReader, JsonElement.class);
                return jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has(IDF_PATH);
            } catch (IOException ignored) {
            }
        }
        return false;
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

    public static String getProfileDefaultBuildOutDir(String basePath, CMakeSettings.Profile profile) {
        if (basePath == null) {
            return null;
        }
        Path baseDir = Path.of(basePath);
        String name = profile.getName().toLowerCase();
        String tryWithProfileName = DEFAULT_CMAKE_BUILD_DIR_PREFIX + name;
        File file = baseDir.resolve(tryWithProfileName).toFile();
        if (file.exists() && file.isDirectory()) {
            return tryWithProfileName;
        }
        return null;
    }

    public static String getBuildOutDir(String basePath, CMakeSettings.Profile profile) {
        if (basePath == null) {
            return null;
        }
        File generationDir = profile.getGenerationDir();
        if (generationDir != null) {
            return generationDir.getName();
        }
        String profileDefaultBuildOutDir = getProfileDefaultBuildOutDir(basePath, profile);
        if (StringUtil.isNotEmpty(profileDefaultBuildOutDir)) {
            return profileDefaultBuildOutDir;
        }
        return null;
    }

    public static File getFileInCmakeBuildDir(Project project, final String fileName) {
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        String basePath = project.getBasePath();

        if (basePath == null) {
            return null;
        }
        Path baseDir = Path.of(basePath);

        ExecutionTarget activeTarget = ExecutionTargetManager.getActiveTarget(project);
        String displayName = activeTarget.getDisplayName();
        CMakeProfileInfo cMakeProfileInfoByName = instance.getCMakeProfileInfoByName(displayName);
        File resolve;
        if (cMakeProfileInfoByName != null) {
            CMakeSettings.Profile profile = cMakeProfileInfoByName.getProfile();
            String buildOutDir = EspIdfProjectUtil.getBuildOutDir(project.getBasePath(), profile);
            if (StringUtils.isNotEmpty(buildOutDir) && (resolve = checkDescFile(baseDir.resolve(buildOutDir), fileName)) != null) {
                return resolve;
            }
        }

        CMakeSettings settings = instance.getSettings();
        List<CMakeSettings.Profile> profiles = settings.getProfiles();
        if (profiles.isEmpty()) {
            return checkDescFile(baseDir.resolve($sys("esp.idf.build.project.build.dir")), fileName);
        }

        for (CMakeSettings.Profile profile : profiles) {
            String buildOutDir = EspIdfProjectUtil.getBuildOutDir(project.getBasePath(), profile);
            if (StringUtils.isNotEmpty(buildOutDir) && (resolve = checkDescFile(baseDir.resolve(buildOutDir), fileName)) != null) {
                return resolve;
            }
        }
        return null;
    }

    private static File checkDescFile(Path buildDir, final String fileName) {
        if (!Files.exists(buildDir)) {
            return null;
        }
        if (Objects.equals("/", fileName)) {
            return buildDir.toFile();
        }
        File projectDesc = buildDir.resolve(fileName).toFile();
        return projectDesc.exists() && projectDesc.canRead() ? projectDesc : null;

    }

    public static DebugConfigModel syncProjectDesc(Project project) {
        String projectDescFileName = $sys("esp.idf.build.project.description");
        File projectDescFile = getFileInCmakeBuildDir(project, projectDescFileName);
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
