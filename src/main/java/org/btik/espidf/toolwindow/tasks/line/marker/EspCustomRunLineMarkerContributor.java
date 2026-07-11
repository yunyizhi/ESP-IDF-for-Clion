package org.btik.espidf.toolwindow.tasks.line.marker;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.btik.espidf.toolwindow.tasks.TreeXmlMeta.*;
import static org.btik.espidf.util.XmlPsiTool.*;

public class EspCustomRunLineMarkerContributor extends RunLineMarkerContributor {

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
        if (StringUtils.isEmpty(path) && StringUtils.isEmpty(args)) {
            return null;
        }

        boolean useIdfEnv = getBoolAttribute(xmlTag, EXEC_WITH_IDF_ENV);
        String encoding = getAttribute(xmlTag, EXEC_ENCODING);
        return getCacheOrNewInfo(name,
                () -> {
                    LocalExecNode localExecNode = new LocalExecNode(name, path, args);
                    if (StringUtils.isNotEmpty(encoding)) {
                        localExecNode.setEncoding(encoding);
                    }
                    localExecNode.setUseTerminal(useTerminal);
                    localExecNode.setUseIdfEnv(useIdfEnv);
                    return new XmlMarkerAction(localExecNode, xmlTag.getProject());
                },
                (oldNode) -> {
                    if (oldNode instanceof LocalExecNode localExecNode) {
                        localExecNode.setPath(path);
                        localExecNode.setArgs(args);
                        localExecNode.setUseTerminal(useTerminal);
                        localExecNode.setUseIdfEnv(useIdfEnv);
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

        return getCacheOrNewInfo(name, () -> {
            EspIdfTaskCommandNode espIdfTaskTreeNode = new EspIdfTaskCommandNode(name, value, useFilter);
            espIdfTaskTreeNode.setUseMonitor(useMonitor);
            espIdfTaskTreeNode.setRequestPort(requestPort);
            return new XmlMarkerAction(espIdfTaskTreeNode, xmlTag.getProject());
        }, (oldNode) -> {
            if (oldNode instanceof EspIdfTaskCommandNode espIdfTaskTreeNode) {
                espIdfTaskTreeNode.setCommand(value);
                espIdfTaskTreeNode.setOutFilter(useFilter);
                espIdfTaskTreeNode.setUseMonitor(useMonitor);
                espIdfTaskTreeNode.setRequestPort(requestPort);
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

        return getCacheOrNewInfo(name,
                () -> new XmlMarkerAction(new EspIdfTaskConsoleCommandNode(name, value, true), xmlTag.getProject()),
                (oldNode) -> {
                    if (oldNode instanceof EspIdfTaskConsoleCommandNode commandNode) {
                        commandNode.setCommand(value);
                    }
                }, xmlTag.getProject());
    }

}
