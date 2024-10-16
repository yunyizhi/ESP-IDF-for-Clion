package org.btik.espidf.environment;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.cpp.toolchains.CPPToolSet;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.toolchains.OSType;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.run.config.build.EspIdfBuildTarget;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfSysConfService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.btik.espidf.adapter.Adapter.readEnvironment;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/18 17:34
 */
public class IdfEnvironmentServiceImpl implements IdfEnvironmentService {
    private Map<String, String> environments;

    private String environmentFile;
    private final Project project;

    private IdfToolConf idfToolConf;

    private final EspIdfBuildTarget espIdfBuildTarget;

    public IdfEnvironmentServiceImpl(Project project) {
        this.project = project;
        this.espIdfBuildTarget = new EspIdfBuildTarget(project.getName());
    }

    @Override
    public Map<String, String> getEnvironments() {
        CPPToolchains.Toolchain toolchain = getCMakeToolchain();
        if (toolchain == null) {
            return new HashMap<>();
        }
        if (!Objects.equals(environmentFile, toolchain.getEnvironment())) {
            generateEnvironment();
        }
        if (environments == null) {
            return new HashMap<>();
        }
        return environments;
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

    @Override
    public String getEnvironmentFile() {
        CPPToolchains.Toolchain toolchain = getCMakeToolchain();
        if (toolchain == null) {
            return environmentFile;
        }
        if (!Objects.equals(environmentFile, toolchain.getEnvironment())) {
            generateEnvironment();
        }
        if (environmentFile == null) {
            return "";
        }
        return environmentFile;
    }

    private CPPToolchains.Toolchain getCMakeToolchain() {
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        List<CMakeSettings.Profile> activeProfiles = instance.getSettings().getActiveProfiles();
        if (activeProfiles.isEmpty()) {
            return null;
        }
        CMakeSettings.Profile currentProfile = activeProfiles.get(0);
        return CPPToolchains.getInstance()
                .getToolchainByNameOrDefault(currentProfile.getToolchainName());
    }

    private void generateEnvironment() {
        CPPToolchains.Toolchain toolchain = getCMakeToolchain();
        if (toolchain == null) {
            return;
        }
        String environment = toolchain.getEnvironment();
        if (StringUtil.isEmpty(environment)) {
            return;
        }
        try {
            environments = ApplicationManager.getApplication()
                    .executeOnPooledThread(() -> readEnvironment(toolchain, environment)).get();
            environmentFile = environment;

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public List<EspIdfBuildTarget> getBuildTargets() {
        return List.of(espIdfBuildTarget);
    }

    @Override
    public CPPToolchains.Toolchain getCurrentToolchain() {
        String envFileName = environmentFile;
        return ApplicationManager.getApplication().runReadAction((Computable<CPPToolchains.Toolchain>) () -> {
            List<CPPToolchains.Toolchain> toolchains = CPPToolchains.getInstance().getToolchains();
            if (toolchains.isEmpty()) {
                return null;
            }
            if (StringUtil.isEmpty(envFileName)) {
                return toolchains.get(0);
            }
            for (CPPToolchains.Toolchain toolchain : toolchains) {
                if (Objects.equals(toolchain.getEnvironment(), envFileName)) {
                    return toolchain;
                }
            }
            return toolchains.get(0);
        });
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

}
