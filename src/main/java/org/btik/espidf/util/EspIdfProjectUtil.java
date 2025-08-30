package org.btik.espidf.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeProfileInfo;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.CMakeBuildProfileExecutionTarget;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.run.config.EspIdfDebugRunConfig;
import org.btik.espidf.run.config.build.EspIdfExecTarget;
import org.btik.espidf.run.config.model.CustomDebugConfigModel;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static org.btik.espidf.service.IdfEnvironmentService.ESP_ROM_ELF_DIR;
import static org.btik.espidf.util.SysConf.$sys;

public class EspIdfProjectUtil {
    private final static Logger log = Logger.getInstance(EspIdfProjectUtil.class);
    private static final String PROJECT_DESC_FILE_NAME = $sys("esp.idf.build.project.description");
    private static final String IDF_PATH = "idf_path";
    private static final String TARGET = "target";

    public static void switchProfileTarget(@NotNull Project project, @NotNull String profileName) {
        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings selectedConfiguration = runManager.getSelectedConfiguration();
        if (selectedConfiguration == null) {
            return;
        }

        RunConfiguration configuration = selectedConfiguration.getConfiguration();

        if (configuration instanceof EspIdfDebugRunConfig<?>) {
            activeTarget(project, configuration,
                    (executionTarget ->
                            executionTarget instanceof EspIdfExecTarget espIdfExecTarget
                                    && Objects.equals(espIdfExecTarget.getDisplayName(), profileName)));
        } else if (configuration instanceof CMakeAppRunConfiguration) {
            activeTarget(project, configuration,
                    (executionTarget ->
                            executionTarget instanceof CMakeBuildProfileExecutionTarget profileExecutionTarget
                                    && Objects.equals(profileExecutionTarget.getProfileName(), profileName)));
        }

    }

    private static void activeTarget(@NotNull Project project, RunConfiguration configuration, Predicate<ExecutionTarget> filter) {
        ExecutionTargetManager executionTargetManager = ExecutionTargetManager.getInstance(project);
        List<ExecutionTarget> targetsFor = executionTargetManager.getTargetsFor(configuration);
        for (ExecutionTarget executionTarget : targetsFor) {
            if (filter.test(executionTarget)) {
                executionTargetManager.setActiveTarget(executionTarget);
                break;
            }
        }
    }

    private static CustomDebugConfigModel parseDesc(File descFile) {
        Gson gson = new Gson();
        String json;
        try {
            json = Files.readString(descFile.toPath());
        } catch (IOException e) {
            log.error(e);
            return null;
        }
        return gson.fromJson(json, CustomDebugConfigModel.class);
    }

    public static String getBuildOutDir(Project project, CMakeSettings.Profile profile) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        File generationDir = profile.getGenerationDir();
        if (generationDir != null) {
            return generationDir.getName();
        }
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        CMakeProfileInfo cMakeProfileInfoByName = instance.getCMakeProfileInfoByName(profile.getName());
        if (cMakeProfileInfoByName != null) {
            return cMakeProfileInfoByName.getGenerationDir().getName();
        }
        return null;
    }

    public static File getFileInCurrentBuildDir(Project project, final String fileName) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        Path baseDir = Path.of(basePath);
        IdfProjectConfigService projectConfigService = project.getService(IdfProjectConfigService.class);
        File resolve;
        String cmakeBuildDir = projectConfigService.getCmakeBuildDir();
        if (StringUtils.isNotEmpty(cmakeBuildDir) && (resolve = checkFileInBuildDir(baseDir.resolve(cmakeBuildDir), fileName)) != null) {
            return resolve;
        }
        return null;
    }

    private static File checkFileInBuildDir(Path buildDir, final String fileName) {
        if (!Files.exists(buildDir)) {
            return null;
        }
        if (Objects.equals("/", fileName)) {
            return buildDir.toFile();
        }
        File file = buildDir.resolve(fileName).toFile();
        return file.exists() && file.canRead() ? file : null;

    }

    public static CustomDebugConfigModel syncProjectDesc(Project project) {
        String projectDescFileName = $sys("esp.idf.build.project.description");
        File projectDescFile = getFileInCurrentBuildDir(project, projectDescFileName);
        if (projectDescFile == null) {
            return null;
        }
        CustomDebugConfigModel customDebugConfigModel = parseDesc(projectDescFile);
        if (customDebugConfigModel == null) {
            return null;
        }
        IdfEnvironmentService idfEnvironmentService = project.getService(IdfEnvironmentService.class);
        Map<String, String> environments = idfEnvironmentService.getEnvironments();
        String romElfDir = environments.get(ESP_ROM_ELF_DIR);
        customDebugConfigModel.setRomElfDir(romElfDir);
        return customDebugConfigModel;
    }

    public static List<IdfProfileInfo> getIdfProfiles(Project project, IdfProjectConfigService projectConfigService) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        Path baseDir = Path.of(basePath);
        List<CMakeSettings.Profile> activeProfiles = CMakeSettings.getInstance(project).getActiveProfiles();
        List<IdfProfileInfo> result = new ArrayList<>();
        Gson gson = new Gson();
        for (CMakeSettings.Profile activeProfile : activeProfiles) {
            String buildOutDir = EspIdfProjectUtil.getBuildOutDir(project, activeProfile);
            if (StringUtils.isEmpty(buildOutDir)) {
                continue;
            }
            File descFile = checkFileInBuildDir(baseDir.resolve(buildOutDir), PROJECT_DESC_FILE_NAME);
            if (descFile == null) {
                continue;
            }
            String name = activeProfile.getName();
            IdfProfileInfo idfProfileInfo = projectConfigService.getIdfProfileInfo(name);
            boolean oldIdfProfileIsNull = idfProfileInfo == null;
            boolean needParseFile = oldIdfProfileIsNull || idfProfileInfo.fileHasUpdate(descFile);
            if (needParseFile) {
                try (FileReader fileReader = new FileReader(descFile);
                     JsonReader jsonReader = new JsonReader(fileReader)) {
                    JsonElement jsonElement = gson.fromJson(jsonReader, JsonElement.class);
                    if (jsonElement == null || !jsonElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject asJsonObject = jsonElement.getAsJsonObject();
                    if (!asJsonObject.has(IDF_PATH)) {
                        continue;
                    }
                    String target = asJsonObject.get(TARGET).getAsString();
                    if (oldIdfProfileIsNull) {
                        idfProfileInfo = new IdfProfileInfo();
                    }
                    idfProfileInfo.setBuildDir(buildOutDir);
                    idfProfileInfo.setTarget(target);
                    idfProfileInfo.setDescFileChangeTime(descFile.lastModified());
                    idfProfileInfo.setDescFileSize(descFile.length());
                    idfProfileInfo.setDisplayName(name);
                    result.add(idfProfileInfo);
                } catch (IOException ioException) {
                    log.warn(ioException);
                }
            } else {
                result.add(idfProfileInfo);
            }

        }
        return result;
    }
}
