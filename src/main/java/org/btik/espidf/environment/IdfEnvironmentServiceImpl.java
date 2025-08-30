package org.btik.espidf.environment;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.ide.progress.TasksKt;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeProfileInfo;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.cpp.toolchains.CPPToolSet;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.toolchains.OSType;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.btik.espidf.adapter.Adapter.readEnvironment;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/18 17:34
 */
public class IdfEnvironmentServiceImpl implements IdfEnvironmentService {

    private final Map<String, Map<String, String>> envFile2Envs = new HashMap<>();

    private final Project project;

    private IdfToolConf idfToolConf;

    private Consumer<Boolean> floatingToolbarVisibleHandler;

    public IdfEnvironmentServiceImpl(Project project) {
        this.project = project;
    }

    private CPPToolchains.Toolchain getToolChianOfCheckedProfile() {
        IdfProjectConfigService projectConfigService = project.getService(IdfProjectConfigService.class);
        IdfProfileInfo idfProfileInfo = projectConfigService.getSelectedIdfProfileInfo();
        if (idfProfileInfo == null || StringUtils.isEmpty(idfProfileInfo.getDisplayName())) {
            return getFirestCMakeToolchain();
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
        return getFirestCMakeToolchain();

    }

    @Override
    public Map<String, String> getEnvironments() {
        CPPToolchains.Toolchain toolchain = getToolChianOfCheckedProfile();
        return getEnvOfToolChain(toolchain);
    }

    private Map<String, String> getEnvOfToolChain(CPPToolchains.Toolchain toolchain) {
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
    public void putTo(Map<String, String> newEnvironments) {
        if (newEnvironments == null) {
            return;
        }
        Map<String, String> environments1 = getEnvironments();
        // 用户输入的同名环境变量比idf初始化变量优先级高 故保留用户输入值
        environments1.forEach((k, v) -> newEnvironments.merge(k, v, (key, oldValue) -> oldValue));
    }

    private CPPToolchains.Toolchain getFirestCMakeToolchain() {
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        List<CMakeSettings.Profile> activeProfiles = instance.getSettings().getActiveProfiles();
        if (activeProfiles.isEmpty()) {
            return null;
        }
        CMakeSettings.Profile currentProfile = activeProfiles.get(0);
        return CPPToolchains.getInstance()
                .getToolchainByNameOrDefault(currentProfile.getToolchainName());
    }

    private Map<String, String> generateEnvironment(CPPToolchains.Toolchain toolchain) {
        String environment = toolchain.getEnvironment();
        if (StringUtil.isEmpty(environment)) {
            return new HashMap<>();
        }
        Map<String, String> rawEnv = TasksKt.runWithModalProgressBlocking(project, $i18nF("esp.idf.read.envs", toolchain.getName()), (scope, continuation) -> {
            try {
                return readEnvironment(toolchain, environment);
            } catch (IOException | com.intellij.execution.ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        rawEnv = sanitizeEnv(rawEnv);
        envFile2Envs.put(environment, rawEnv);
        return rawEnv;
    }

    @Override
    public IdfToolConf getWinToolConf(String idfToolPath, String idfId) {
        IdfSysConfService service = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        IdfToolConf toolConfByKey = service.getToolConfByKey(idfToolPath + idfId);
        if (toolConfByKey != null && toolConfByKey.getToolchain() != null) {
            return toolConfByKey;
        }
        Path idfConfFolder = service.getIdfConfFolder();
        String exportEnvCmd = idfToolPath + File.separatorChar +
                $sys("idf.windows.command.init.bat") + " " + idfId;
        Path idfExportPs1 = idfConfFolder.resolve(ENV_FILE_PREFIX + Integer.toHexString(exportEnvCmd.hashCode()) + ".bat");
        try {
            Files.writeString(idfExportPs1, exportEnvCmd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String exportBatPath = idfExportPs1.toString();
        IdfToolConf newIdfToolConf = new IdfToolConf();
        newIdfToolConf.setIdfId(idfId);
        newIdfToolConf.setEnvFileName(exportBatPath);
        newIdfToolConf.setIdfToolPath(idfToolPath);

        CPPToolchains.Toolchain toolchain = getToolChain(exportBatPath);
        newIdfToolConf.setToolchain(toolchain);

        service.store(newIdfToolConf);
        this.idfToolConf = newIdfToolConf;
        return this.idfToolConf;
    }

    @Override
    public IdfToolConf getSourceToolConf(String idfFrameworkPath) {
        IdfSysConfService service = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        IdfToolConf toolConfByKey = service.getToolConfByKey(idfFrameworkPath);
        if (toolConfByKey != null && toolConfByKey.getToolchain() != null) {
            return toolConfByKey;
        }
        IdfToolConf newIdfToolConf = new IdfToolConf();
        String envFileName = idfFrameworkPath + File.separatorChar + $sys(IS_WINDOWS ? "idf.windows.export.bat" : "idf.unix.export.script");
        newIdfToolConf.setEnvFileName(envFileName);
        newIdfToolConf.setIdfToolPath(idfFrameworkPath);
        CPPToolchains.Toolchain toolchain = getToolChain(envFileName);
        newIdfToolConf.setToolchain(toolchain);
        service.store(newIdfToolConf);
        this.idfToolConf = newIdfToolConf;
        return newIdfToolConf;
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
            floatingToolbarVisibleHandler.accept(needReload.get());
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
        CPPToolchains.Toolchain existsToolChain = ApplicationManager.getApplication().runReadAction((Computable<CPPToolchains.Toolchain>) () -> {
            List<CPPToolchains.Toolchain> toolchains = CPPToolchains.getInstance().getToolchains();
            for (CPPToolchains.Toolchain toolchain : toolchains) {
                if (Objects.equals(toolchain.getEnvironment(), envFileName)) {
                    return toolchain;
                }
            }
            return null;
        });
        if (existsToolChain != null) {
            return existsToolChain;
        }

        CPPToolchains.Toolchain idfToolChain = new CPPToolchains.Toolchain(OSType.getCurrent());
        idfToolChain.setToolSetKind(IS_WINDOWS ? CPPToolSet.Kind.SYSTEM_WINDOWS_TOOLSET : CPPToolSet.Kind.SYSTEM_UNIX_TOOLSET);
        idfToolChain.setName(IDF_TOOLCHAIN_NAME_PREFIX + Integer.toHexString(envFileName.hashCode()));
        ApplicationManager.getApplication().invokeLater(() ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                            CPPToolchains.getInstance().beginUpdate();
                            CPPToolchains.getInstance().addToolchain(idfToolChain);
                            idfToolChain.setEnvironment(envFileName);
                            CPPToolchains.getInstance().endUpdate();
                        }
                ));
        return idfToolChain;
    }

    private Map<String, String> sanitizeEnv(Map<String, String> env) {
        Map<String, String> cleanEnv = new HashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.equalsIgnoreCase("PATH")) {
                // making sure to remove extra quotes and semicolon-prefixes
                value = value.replace("\"", "").replaceAll("^;+|;+$", "");
            }
            cleanEnv.put(key, value);
        }
        return cleanEnv;
    }

}
