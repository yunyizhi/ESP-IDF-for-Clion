package org.btik.espidf.util;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.intellij.platform.ide.progress.TaskCancellation;
import com.intellij.platform.ide.progress.TasksKt;
import com.intellij.util.system.OS;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.cpp.toolchains.CPPToolSet;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.system.LocalHost;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.btik.espidf.service.IdfEnvironmentService.IDF_TOOLCHAIN_NAME_PREFIX;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;

public class ToolChainTool {

    public static CPPToolchains.Toolchain findToolchainByEnvFile(String envFileName) {
        return ApplicationManager.getApplication().runReadAction((Computable<CPPToolchains.Toolchain>)
                () -> CPPToolchains.getInstance().getToolchains().stream()
                        .filter((toolchain -> Objects.equals(toolchain.getEnvironment(), envFileName)))
                        .findFirst()
                        .orElse(null));
    }

    public static List<CPPToolchains.Toolchain> getFilteredToolchains(Predicate<CPPToolchains.Toolchain> filter) {
        return getFilteredToolchains(filter, Collectors.toList());
    }

    public static <R, A> R getFilteredToolchains(Predicate<CPPToolchains.Toolchain> filter, Collector<CPPToolchains.Toolchain, A, R> collector) {
        return ApplicationManager.getApplication().runReadAction((Computable<R>)
                () -> CPPToolchains.getInstance().getToolchains().stream()
                        .filter(filter).collect(collector)
        );
    }

    public static CPPToolchains.Toolchain getFirestCMakeToolchain(@NotNull Project project) {
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        List<CMakeSettings.Profile> activeProfiles = instance.getSettings().getActiveProfiles();
        if (activeProfiles.isEmpty()) {
            return null;
        }
        CMakeSettings.Profile currentProfile = activeProfiles.getFirst();
        return CPPToolchains.getInstance()
                .getToolchainByNameOrDefault(currentProfile.getToolchainName());
    }

    public static CPPToolchains.Toolchain newIdfToolChain(String envFile) {
        return newEnvToolChain(envFile, null);
    }

    public static CPPToolchains.Toolchain newEnvToolChain(String envFile, @Nullable String name) {
        CPPToolchains.Toolchain idfToolChain = new CPPToolchains.Toolchain(OS.CURRENT);
        idfToolChain.setToolSetKind(IS_WINDOWS ? CPPToolSet.Kind.SYSTEM_WINDOWS_TOOLSET : CPPToolSet.Kind.SYSTEM_UNIX_TOOLSET);
        if (StringUtils.isEmpty(name)) {
            name = generateToolChainName(envFile);
        }
        name = toolchainNameRename(name);
        idfToolChain.setName(name);
        ApplicationManager.getApplication().runWriteAction(() -> {
                    CPPToolchains.getInstance().beginUpdate();
                    CPPToolchains.getInstance().addToolchain(idfToolChain);
                    idfToolChain.setEnvironment(envFile);
                    CPPToolchains.getInstance().endUpdate();
                }
        );
        return idfToolChain;
    }

    public static String toolchainNameRename(String name) {
        Set<String> toolchainNames = ApplicationManager.getApplication().runReadAction((Computable<Set<String>>)
                () -> CPPToolchains.getInstance().getToolchains().stream()
                        .map(CPPToolchains.Toolchain::getName)
                        .collect(Collectors.toSet())
        );
        String newName = name;
        int index = 1;
        while (toolchainNames.contains(newName)) {
            newName = name + "(" + index++ + ")";
        }
        return newName;
    }

    public static String generateToolChainName(String envFile) {
        return IDF_TOOLCHAIN_NAME_PREFIX + Integer.toHexString(envFile.hashCode());
    }

    /**
     * * @param toolchain 必须包含环境变量文件
     */
    public static Map<String, String> toolChainEnv(CPPToolchains.Toolchain toolchain) throws IOException, ExecutionException {
        String environment = toolchain.getEnvironment();
        if (StringUtil.isEmpty(environment)) {
            return new HashMap<>();
        }
        return toolchain.getToolSet().readEnvironment(toolchain.getEnvironment(), LocalHost.INSTANCE,
                new HashMap<>());
    }

    public static Map<String, String> toolChainEnvByOwner(CPPToolchains.Toolchain toolchain, ModalTaskOwner owner) {
        return TasksKt.runWithModalProgressBlocking(owner,
                $i18nF("esp.idf.read.envs", toolchain.getName()),
                TaskCancellation.nonCancellable(), (scope, continuation) -> {
                    try {
                        return toolChainEnv(toolchain);
                    } catch (IOException | com.intellij.execution.ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static Map<String, String> toolChainEnvByComp(CPPToolchains.Toolchain toolchain, Component component) {
        return toolChainEnvByOwner(toolchain, ModalTaskOwner.component(component));
    }


    public static Map<String, String> toolChainEnvByProj(CPPToolchains.Toolchain toolchain, Project project) {
        return TasksKt.runWithModalProgressBlocking(project, $i18nF("esp.idf.read.envs", toolchain.getName()),
                (scope, continuation) -> {
                    try {
                        return toolChainEnv(toolchain);
                    } catch (IOException | com.intellij.execution.ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
