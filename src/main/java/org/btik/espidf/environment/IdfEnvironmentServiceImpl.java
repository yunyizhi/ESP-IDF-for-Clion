package org.btik.espidf.environment;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeProfileInfo;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.btik.espidf.util.EnvironmentVarUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.btik.espidf.util.OsUtil.IS_WINDOWS;
import static org.btik.espidf.util.PathTool.normalizePath;
import static org.btik.espidf.util.SysConf.$sysOs;
import static org.btik.espidf.util.ToolChainTool.*;

/**
 * @author lustre
 * @since 2024/2/18 17:34
 */
public class IdfEnvironmentServiceImpl implements IdfEnvironmentService {

    private final Map<String, Map<String, String>> envFile2Envs = new HashMap<>();

    private final Project project;

    private Consumer<Boolean> floatingToolbarVisibleHandler;

    public IdfEnvironmentServiceImpl(Project project) {
        this.project = project;
    }

    public CPPToolchains.Toolchain getToolChianOfCheckedProfile() {
        IdfProjectConfigService projectConfigService = project.getService(IdfProjectConfigService.class);
        IdfProfileInfo idfProfileInfo = projectConfigService.getSelectedIdfProfileInfo();
        if (idfProfileInfo == null || StringUtils.isEmpty(idfProfileInfo.getDisplayName())) {
            return getFirestCMakeToolchain(project);
        }

        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        CPPToolchains cppToolchains = CPPToolchains.getInstance();
        CMakeProfileInfo cMakeProfileInfoByName = instance.getCMakeProfileInfoByName(idfProfileInfo.getDisplayName());
        if (cMakeProfileInfoByName != null) {
            CMakeSettings.Profile profile = cMakeProfileInfoByName.getProfile();
            return cppToolchains.getToolchainByNameOrDefault(profile.getToolchainName());
        }
        // 强制刷新
        projectConfigService.onProfileChanged();
        // 查询第一个idf的profile的Toolchain
        List<CMakeSettings.Profile> activeProfiles = instance.getSettings().getActiveProfiles();
        for (CMakeSettings.Profile activeProfile : activeProfiles) {
            if (projectConfigService.getIdfProfileInfo(activeProfile.getName()) != null) {
                return cppToolchains.getToolchainByNameOrDefault(activeProfile.getToolchainName());
            }
        }
        return getFirestCMakeToolchain(project);

    }

    @Override
    public Map<String, String> getEnvironments() {
        CPPToolchains.Toolchain toolchain = getToolChianOfCheckedProfile();
        return getEnvOfToolChain(toolchain);
    }

    @Override
    public Map<String, String> getEnvOfToolChain(CPPToolchains.Toolchain toolchain) {
        if (toolchain == null) {
            return new HashMap<>();
        }
        String environment = toolchain.getEnvironment();
        if (StringUtil.isEmpty(environment)) {
            return new HashMap<>();
        }
        Map<String, String> env = envFile2Envs.get(environment);
        if (env != null) {
            return env;
        }
        return generateEnvironment(toolchain);
    }

    @Override
    public Map<String, String> setCache(@NotNull CPPToolchains.Toolchain toolchain, @NotNull Map<String, String> env) {
        Map<String, String> newEnv = sanitizeEnv(env);
        envFile2Envs.put(toolchain.getEnvironment(), newEnv);
        return newEnv;
    }

    @Override
    public void putTo(Map<String, String> newEnvironments) {
        if (newEnvironments == null) {
            return;
        }
        Map<String, String> environments1 = getEnvironments();
        // 用户输入的同名环境变量比idf初始化变量优先级高 故保留用户输入值
        environments1.forEach((k, v) -> newEnvironments.merge(k, v, (key, oldValue) -> oldValue));
    }

    private Map<String, String> generateEnvironment(CPPToolchains.Toolchain toolchain) {
        Map<String, String> rawEnv = toolChainEnvByProj(toolchain, project);
        rawEnv = sanitizeEnv(rawEnv);
        envFile2Envs.put(toolchain.getEnvironment(), rawEnv);
        return rawEnv;
    }

    @Override
    public CPPToolchains.Toolchain getSourceToolConf(String idfPath, String idfToolsPath) {
        String envFileName;
        if (isDefaultIdfToolsPath(idfToolsPath)) {
            envFileName = idfPath + File.separatorChar + $sysOs("idf.windows.export.bat", "idf.unix.export.script");
        } else {
            envFileName = createCustomExportScript(idfPath, idfToolsPath);
        }
        return getToolChain(envFileName);
    }

    private boolean isDefaultIdfToolsPath(String idfToolsPath) {
        if (idfToolsPath == null || idfToolsPath.isEmpty()) {
            return true;
        }

        Path inputPath = normalizePath(idfToolsPath);
        Path defaultPath = normalizePath(DEFAULT_IDF_TOOLS_PATH);

        return inputPath.equals(defaultPath);
    }

    private String createCustomExportScript(String idfPath, String idfToolsPath) {
        Path idfPathObj = Path.of(idfPath);
        String originalExportName = $sysOs("idf.windows.export.ps1","idf.unix.export.script");
        Path originalExport = idfPathObj.resolve(originalExportName);

        if (!Files.exists(originalExport)) {
            throw new RuntimeException("Export script not found: " + originalExport);
        }

        String customExportName = $sysOs("idf.windows.export.clion.ps1", "idf.unix.export.clion.sh");
        Path customExport = idfPathObj.resolve(customExportName);

        try {
            List<String> originalLines = Files.readAllLines(originalExport);

            StringBuilder content = new StringBuilder();
            if (IS_WINDOWS) {
                content.append("$env:" + IDF_TOOLS_PATH + "=\"").append(idfToolsPath).append("\"\n");
            } else {
                content.append("export" + IDF_TOOLS_PATH + "=\"").append(idfToolsPath).append("\"\n");
            }

            for (String line : originalLines) {
                content.append(line).append("\n");
            }

            Files.writeString(customExport, content.toString());

            if (!IS_WINDOWS) {
                Files.setPosixFilePermissions(customExport,
                        java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
            }

            return customExport.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create custom export script: " + e.getMessage(), e);
        }
    }

    private void eachIdfToolChain(Function<CPPToolchains.Toolchain, @NotNull Boolean> callback) {
        IdfProjectConfigService projectConfigService = project.getService(IdfProjectConfigService.class);
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        CPPToolchains cppToolchains = CPPToolchains.getInstance();

        List<IdfProfileInfo> idfProfileInfoList = projectConfigService.getIdfProfileInfoList();
        for (IdfProfileInfo idfProfileInfo : idfProfileInfoList) {
            CMakeProfileInfo cMakeProfileInfoByName = instance.getCMakeProfileInfoByName(idfProfileInfo.getDisplayName());
            if (cMakeProfileInfoByName == null) {
                continue;
            }
            CMakeSettings.Profile profile = cMakeProfileInfoByName.getProfile();
            CPPToolchains.Toolchain toolChian = cppToolchains.getToolchainByNameOrDefault(profile.getToolchainName());
            if (toolChian == null) {
                continue;
            }
            boolean next = callback.apply(toolChian);
            if (!next) {
                break;
            }
        }
    }

    @Override
    public void buildEnvironmentsCache() {
        project.getService(IdfProjectConfigService.class).onProfileChanged();
        eachIdfToolChain((toolchain -> {
            generateEnvironment(toolchain);
            return true;
        }));
    }

    @Override
    public void fixEnvironmentsCache() {
        eachIdfToolChain((toolchain -> {
            String environment = toolchain.getEnvironment();
            if (!envFile2Envs.containsKey(environment)) {
                generateEnvironment(toolchain);
            }
            return true;
        }));
    }

    @Override
    public void checkEnvNeedRebuild() {
        AtomicBoolean needReload = new AtomicBoolean(false);
        eachIdfToolChain((toolchain -> {
            String environment = toolchain.getEnvironment();
            if (!envFile2Envs.containsKey(environment)) {
                needReload.set(true);
                return false;
            }
            return true;
        }));
        if (floatingToolbarVisibleHandler != null) {
            ApplicationManager.getApplication().invokeLater(() -> floatingToolbarVisibleHandler.accept(needReload.get()));
        }
    }

    @Override
    public void register(Consumer<Boolean> floatingToolbarVisibleHandler) {
        this.floatingToolbarVisibleHandler = floatingToolbarVisibleHandler;
    }

    @Override
    public void setFloatingToolbarVisible(boolean visible) {
        if (floatingToolbarVisibleHandler != null) {
            floatingToolbarVisibleHandler.accept(visible);
        }
    }

    private CPPToolchains.Toolchain getToolChain(String envFileName) {
        CPPToolchains.Toolchain existsToolChain = findToolchainByEnvFile(envFileName);
        if (existsToolChain != null) {
            return existsToolChain;
        }
        return newIdfToolChain(envFileName);
    }

    private String pathEnvProcess(Map<String, String> env) {
        String path = env.get(PATH);
        if (path == null) {
            path = env.get("Path");
        }
        if (StringUtils.isEmpty(path)) {
            return "";
        }
        path = path.replace("\"", "").replaceAll("^;+|;+$", "");
        String idfFullPath = EnvironmentVarUtil.findIdfFullPath(path);
        if (StringUtils.isNotEmpty(idfFullPath)) {
            return path;
        }
        String idfPath = env.get(IDF_PATH);
        String separator = IS_WINDOWS ? ";" : ":";
        return path + separator + idfPath + File.separatorChar + SRC_TOOLS_DIR;
    }

    private Map<String, String> sanitizeEnv(Map<String, String> env) {
        Map<String, String> cleanEnv = new HashMap<>(env);
        cleanEnv.put(PATH, pathEnvProcess(env));
        return cleanEnv;
    }

}
