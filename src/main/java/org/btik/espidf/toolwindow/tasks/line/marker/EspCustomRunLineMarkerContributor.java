package org.btik.espidf.toolwindow.tasks.line.marker;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.tasks.TreeXmlMeta;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskConsoleCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskTreeNode;
import org.btik.espidf.toolwindow.tasks.model.LocalExecNode;
import org.btik.espidf.toolwindow.tasks.model.Profiles;
import org.btik.espidf.util.EnvironmentVarUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.btik.espidf.toolwindow.tasks.TreeXmlMeta.*;
import static org.btik.espidf.util.XmlPsiTool.*;

public class EspCustomRunLineMarkerContributor extends RunLineMarkerContributor {

    private static final Logger LOG = Logger.getInstance(EspCustomRunLineMarkerContributor.class);

    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public Info getSlowInfo(@NotNull PsiElement element) {
        if (!(element instanceof XmlTag xmlTag)) {
            return null;
        }
        PsiFile containingFile = xmlTag.getContainingFile();
        if (!TreeXmlMeta.ESP_CUSTOM_TASKS_XML.equals(containingFile.getName())) {
            return null;
        }
        // 文件变化时清理已删除/重命名任务的缓存，避免内存积压（按修改戳去重，仅扫描一次）
        if (containingFile instanceof XmlFile xmlFile) {
            xmlTag.getProject().getService(IdfProjectConfigService.class)
                    .syncRunInfoCache(containingFile.getModificationStamp(), () -> collectValidNames(xmlFile));
        }
        String tagName = xmlTag.getName();
        return switch (tagName) {
            case TreeXmlMeta.CONSOLE_COMMAND -> getConsoleInfo(xmlTag);
            case TreeXmlMeta.COMMAND_TAG -> getCommandInfo(xmlTag);
            case TreeXmlMeta.LOCAL_EXEC -> getLocalExec(xmlTag);
            default -> null;
        };
    }

    private Info getLocalExec(XmlTag xmlTag) {
        String name = getAttribute(xmlTag, TreeXmlMeta.NAME);
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        String path = getAttribute(xmlTag, EXEC_PATH);
        boolean useTerminal = getBoolAttribute(xmlTag, USE_TERMINAL);
        String argsBySubTag = getSubTagTrimmedText(xmlTag, EXEC_ARGS, useTerminal);
        String args = argsBySubTag == null ? getAttribute(xmlTag, EXEC_ARGS) : argsBySubTag;

        boolean useIdfEnv = getBoolAttribute(xmlTag, EXEC_WITH_IDF_ENV);
        String encoding = getAttribute(xmlTag, EXEC_ENCODING);
        ResolvedVars vars = resolveVars(xmlTag);
        Map<String, String> envVars = vars.env();
        Map<String, String> macros = vars.macros();
        String resolvedPath = EnvironmentVarUtil.substitutePartial(path, envVars, macros);
        String resolvedArgs = EnvironmentVarUtil.substitutePartial(args, envVars, macros);
        if (StringUtils.isEmpty(resolvedPath) && StringUtils.isEmpty(resolvedArgs)) {
            return null;
        }

        return getCacheOrNewInfo(name,
                () -> {
                    LocalExecNode localExecNode = new LocalExecNode(name, resolvedPath, resolvedArgs);
                    if (StringUtils.isNotEmpty(encoding)) {
                        localExecNode.setEncoding(encoding);
                    }
                    localExecNode.setUseTerminal(useTerminal);
                    localExecNode.setUseIdfEnv(useIdfEnv);
                    localExecNode.setEnvVars(envVars);
                    return new XmlMarkerAction(localExecNode, xmlTag.getProject());
                },
                (oldNode) -> {
                    if (oldNode instanceof LocalExecNode localExecNode) {
                        localExecNode.setPath(resolvedPath);
                        localExecNode.setArgs(resolvedArgs);
                        localExecNode.setUseTerminal(useTerminal);
                        localExecNode.setUseIdfEnv(useIdfEnv);
                        localExecNode.setEnvVars(envVars);
                        if (StringUtils.isNotEmpty(encoding)) {
                            localExecNode.setEncoding(encoding);
                        }
                    }
                }, xmlTag.getProject());
    }

    private Info getCommandInfo(@NotNull XmlTag xmlTag) {
        String value = getAttribute(xmlTag, TreeXmlMeta.VALUE);
        if (value == null) {
            return null;
        }
        String name = getAttribute(xmlTag, TreeXmlMeta.NAME);
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        boolean useMonitor = getBoolAttribute(xmlTag, USE_MONITOR);
        boolean requestPort = getBoolAttribute(xmlTag, REQUEST_PORT, useMonitor);
        boolean useFilter = getBoolAttribute(xmlTag, CONSOLE_FILTER);
        ResolvedVars vars = resolveVars(xmlTag);
        Map<String, String> envVars = vars.env();
        Map<String, String> macros = vars.macros();
        String resolvedValue = EnvironmentVarUtil.substitutePartial(value, envVars, macros);

        return getCacheOrNewInfo(name, () -> {
            EspIdfTaskCommandNode espIdfTaskTreeNode = new EspIdfTaskCommandNode(name, resolvedValue, useFilter);
            espIdfTaskTreeNode.setUseMonitor(useMonitor);
            espIdfTaskTreeNode.setRequestPort(requestPort);
            espIdfTaskTreeNode.setEnvVars(envVars);
            return new XmlMarkerAction(espIdfTaskTreeNode, xmlTag.getProject());
        }, (oldNode) -> {
            if (oldNode instanceof EspIdfTaskCommandNode espIdfTaskTreeNode) {
                espIdfTaskTreeNode.setCommand(resolvedValue);
                espIdfTaskTreeNode.setOutFilter(useFilter);
                espIdfTaskTreeNode.setUseMonitor(useMonitor);
                espIdfTaskTreeNode.setRequestPort(requestPort);
                espIdfTaskTreeNode.setEnvVars(envVars);
            }
        }, xmlTag.getProject());
    }

    private Info getCacheOrNewInfo(@NotNull String name, Supplier<XmlMarkerAction> actionSupplier, Consumer<EspIdfTaskTreeNode> updater, @NotNull
    Project project) {
        IdfProjectConfigService idfProjectConfigService = project.getService(IdfProjectConfigService.class);
        Info info = idfProjectConfigService.getRunInfo(name);
        if (info == null || info.actions.length < 1) {
            info = new Info(actionSupplier.get());
            idfProjectConfigService.putRunInfo(name, info);
            return info;
        }
        if (info.actions[0] instanceof XmlMarkerAction xmlMarkerAction) {
            updater.accept(xmlMarkerAction.getNode());
        }
        return info;
    }

    /**
     * 收集自定义任务文件中当前仍然存在的任务名（command / console-command / exec）。
     * 用于清理缓存中已被删除或重命名的任务条目。
     */
    private static Set<String> collectValidNames(@NotNull XmlFile xmlFile) {
        Set<String> names = new HashSet<>();
        XmlTag root = xmlFile.getRootTag();
        if (root == null) {
            return names;
        }
        for (XmlTag tag : root.getSubTags()) {
            String tagName = tag.getName();
            if (COMMAND_TAG.equals(tagName) || CONSOLE_COMMAND.equals(tagName) || LOCAL_EXEC.equals(tagName)) {
                String name = getAttribute(tag, NAME);
                if (StringUtils.isNotEmpty(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private Info getConsoleInfo(XmlTag xmlTag) {
        String value = getAttribute(xmlTag, TreeXmlMeta.VALUE);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String name = getAttribute(xmlTag, TreeXmlMeta.NAME);
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        ResolvedVars vars = resolveVars(xmlTag);
        Map<String, String> envVars = vars.env();
        Map<String, String> macros = vars.macros();
        String resolvedValue = EnvironmentVarUtil.substitutePartial(value, envVars, macros);
        return getCacheOrNewInfo(name,
                () -> {
                    EspIdfTaskConsoleCommandNode commandNode = new EspIdfTaskConsoleCommandNode(name, resolvedValue, true);
                    commandNode.setEnvVars(envVars);
                    return new XmlMarkerAction(commandNode, xmlTag.getProject());
                },
                (oldNode) -> {
                    if (oldNode instanceof EspIdfTaskConsoleCommandNode commandNode) {
                        commandNode.setCommand(resolvedValue);
                        commandNode.setEnvVars(envVars);
                    }
                }, xmlTag.getProject());
    }

    /** 单个任务解析后的合并结果：环境变量（注入进程）与宏（用于文本替换）。 */
    private record ResolvedVars(Map<String, String> env, Map<String, String> macros) {
    }

    /**
     * 解析任务的内联 env 与引用的 profile，合并出最终环境变量与宏。
     * 优先级：内联 &gt; 命名 profile（按 profile 属性书写顺序）&gt; 匿名 profile。
     */
    private static ResolvedVars resolveVars(@NotNull XmlTag xmlTag) {
        Profiles profiles = collectProfiles(xmlTag);
        String profileAttr = getAttribute(xmlTag, PROFILE);
        return new ResolvedVars(profiles.resolveEnv(profileAttr, parseEnv(xmlTag)),
                profiles.resolveMacros(profileAttr));
    }

    /**
     * 从自定义任务根标签下收集变量组 profile：{@code <envs>} / {@code <macros>} 为多行 key=value 列表。
     * 无 {@code name} 的 profile 视为匿名（全局导入）。
     */
    private static Profiles collectProfiles(@NotNull XmlTag tag) {
        Profiles profiles = new Profiles();
        PsiFile file = tag.getContainingFile();
        if (!(file instanceof XmlFile xmlFile)) {
            return profiles;
        }
        XmlTag root = xmlFile.getRootTag();
        if (root == null) {
            return profiles;
        }
        for (XmlTag child : root.getSubTags()) {
            if (!PROFILE.equals(child.getName())) {
                continue;
            }
            Map<String, String> env = new HashMap<>();
            putEnv(getSubTagTrimmedText(child, ENVS, false), env);
            Map<String, String> macros = new HashMap<>();
            putEnv(getSubTagTrimmedText(child, MACROS, false), macros);
            profiles.addNamed(getAttribute(child, NAME), env, macros);
        }
        return profiles;
    }

    /**
     * 解析拓展环境变量：行内属性 {@code env} 与子标签 {@code <env>} 均支持，
     * 变量列表每行只写一个 {@code key=value}，子标签会在同名时覆盖属性。
     */
    private static Map<String, String> parseEnv(@NotNull XmlTag xmlTag) {
        Map<String, String> envVars = new HashMap<>();
        putEnv(getAttribute(xmlTag, ENV), envVars);
        putEnv(getSubTagTrimmedText(xmlTag, ENV, false), envVars);
        return envVars;
    }

    private static void putEnv(String text, Map<String, String> envVars) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        try {
            envVars.putAll(EnvironmentVarUtil.parseEnv(text));
        } catch (RuntimeException e) {
            // 格式非法时忽略该来源，避免阻断运行图标生成，格式问题由 xsd 在编辑器中提示
            LOG.warn("Malformed extended environment variables: " + text, e);
        }
    }

}
