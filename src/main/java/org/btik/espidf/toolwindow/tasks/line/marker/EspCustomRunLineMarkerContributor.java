package org.btik.espidf.toolwindow.tasks.line.marker;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.toolwindow.tasks.TreeXmlMeta;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskConsoleCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskTreeNode;
import org.btik.espidf.toolwindow.tasks.model.LocalExecNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.btik.espidf.toolwindow.tasks.TreeXmlMeta.*;
import static org.btik.espidf.util.XmlPsiTool.*;

public class EspCustomRunLineMarkerContributor extends RunLineMarkerContributor {
    private final HashMap<String, Info> runInfoCache = new HashMap<>();

    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public Info getSlowInfo(@NotNull PsiElement element) {
        if (!(element instanceof XmlTag xmlTag)) {
            return null;
        }
        String fileName = xmlTag.getContainingFile().getName();
        if (!TreeXmlMeta.ESP_CUSTOM_TASKS_XML.equals(fileName)) {
            return null;
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
        String argsBySubTag = getSubTagTrimmedText(xmlTag, EXEC_ARGS, true);
        String args = argsBySubTag == null ? getAttribute(xmlTag, EXEC_ARGS) : argsBySubTag;
        if (StringUtils.isEmpty(path) && StringUtils.isEmpty(args) ) {
            return null;
        }

        boolean useTerminal = getBoolAttribute(xmlTag, USE_TERMINAL);
        boolean useIdfEnv = getBoolAttribute(xmlTag, EXEC_WITH_IDF_ENV);
        return getCacheOrNewInfo(name,
                () -> {
                    LocalExecNode localExecNode = new LocalExecNode(name, path, args);
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
                    }
                });
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
        });
    }

    private Info getCacheOrNewInfo(@NotNull String name, Supplier<XmlMarkerAction> actionSupplier, Consumer<EspIdfTaskTreeNode> updater) {
        Info info = runInfoCache.get(name);
        if (info == null || info.actions.length < 1) {
            info = new Info(actionSupplier.get());
            runInfoCache.put(name, info);
            return info;
        }
        if (info.actions[0] instanceof XmlMarkerAction xmlMarkerAction) {
            updater.accept(xmlMarkerAction.getNode());
        }
        return info;
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
                });
    }

}
