package org.btik.espidf.environment;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.intellij.platform.ide.progress.TaskCancellation;
import com.intellij.platform.ide.progress.TasksKt;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.system.LocalHost;

import static org.btik.espidf.util.I18nMessage.$i18nF;

/**
 * @author lustre
 * @since 2024/2/15 21:09
 */
public class ToolchainEnvReader {

    /**
     * @param toolchain 必须包含环境变量文件
     *
     */
    public static Map<String, String> toolChainEnv(CPPToolchains.Toolchain toolchain) throws IOException, ExecutionException {
        String environment = toolchain.getEnvironment();
        if (StringUtil.isEmpty(environment)) {
            return new HashMap<>();
        }
        return toolchain.getToolSet().readEnvironment(toolchain.getEnvironment(), LocalHost.INSTANCE,
                new HashMap<>());
    }

    public static Map<String, String> toolChainEnvByComp(CPPToolchains.Toolchain toolchain, Component component) {
        return TasksKt.runWithModalProgressBlocking(ModalTaskOwner.component(component),
                $i18nF("esp.idf.read.envs", toolchain.getName()),
                TaskCancellation.nonCancellable(), (scope, continuation) -> {
                    try {
                        return toolChainEnv(toolchain);
                    } catch (IOException | com.intellij.execution.ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    public static Map<String, String> toolChainEnvByProj(CPPToolchains.Toolchain toolchain, Project project) {
        return TasksKt.runWithModalProgressBlocking(project, $i18nF("esp.idf.read.envs", toolchain.getName()), (scope, continuation) -> {
            try {
                return toolChainEnv(toolchain);
            } catch (IOException | com.intellij.execution.ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
