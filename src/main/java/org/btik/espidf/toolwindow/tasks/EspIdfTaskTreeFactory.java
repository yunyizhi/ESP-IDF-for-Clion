package org.btik.espidf.toolwindow.tasks;

import com.intellij.notification.NotificationType;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.toolwindow.common.NodeModel;
import org.btik.espidf.toolwindow.tasks.model.*;
import org.btik.espidf.util.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static org.btik.espidf.toolwindow.tasks.TreeXmlMeta.*;
import static org.btik.espidf.util.DomUtil.eachChildrenElement;
import static org.btik.espidf.util.DomUtil.getFirstElementByName;
import static org.btik.espidf.util.I18nMessage.*;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;

/**
 * @author lustre
 * @since 2024/2/18 15:16
 */
public class EspIdfTaskTreeFactory {
    private static final HashMap<String, Function<Element, NodeModel<Element>>> factories = new HashMap<>();

    static {
        factories.put(FOLDER_TAG, EspIdfTaskTreeFactory::newFolder);
        factories.put(COMMAND_TAG, EspIdfTaskTreeFactory::newCmd);
        factories.put(CONSOLE_COMMAND, EspIdfTaskTreeFactory::newConsoleCmd);
        factories.put(RAW_COMMAND, EspIdfTaskTreeFactory::newRawCmd);
        factories.put(ACTION, EspIdfTaskTreeFactory::newAction);
        factories.put(LOCAL_EXEC, EspIdfTaskTreeFactory::newLocalExec);
    }

    public static DefaultMutableTreeNode load() {
        Element documentElement;
        try {
            Document treeConf = DomUtil.parse(EspIdfTaskTreeFactory.class
                    .getResourceAsStream("/org-btik-esp-idf/conf/defaultTree.xml"));
            documentElement = treeConf.getDocumentElement();

        } catch (Exception e) {
            NOTIFICATION_GROUP.createNotification($i18n("notification.group.idf"),
                    e.getMessage(), NotificationType.ERROR).notify(null);
            return null;
        }
        Element treeRoot = getFirstElementByName(documentElement, TREE_ROOT);
        if (null == treeRoot) {
            NOTIFICATION_GROUP.createNotification($i18n("notification.group.idf"),
                    $i18nF("idf.xml.load.failed", TREE_ROOT), NotificationType.ERROR).notify(null);
            return null;
        }

        return build(treeRoot);
    }

    private static DefaultMutableTreeNode build(Element treeRoot) {
        NodeModel<Element> rootNode = newFolder(treeRoot);
        LinkedList<NodeModel<Element>> treeNodeQueue = new LinkedList<>();
        treeNodeQueue.add(rootNode);
        while (!treeNodeQueue.isEmpty()) {
            NodeModel<Element> xmlNode = treeNodeQueue.removeFirst();
            eachChildrenElement(xmlNode.getModel(), (child) -> {
                String type = child.getTagName();
                NodeModel<Element> childXmlNode = factories.get(type).apply(child);
                xmlNode.getNode().add(childXmlNode.getNode());
                treeNodeQueue.add(childXmlNode);
            });
        }

        return rootNode.getNode();
    }

    private static NodeModel<Element> newFolder(Element element) {
        String name = element.getAttribute(NAME);
        return buildNode(element, new EspIdfTaskFolderNode(name));
    }

    private static NodeModel<Element> newCmd(Element element) {
        String name = element.getAttribute(NAME);
        String command = element.getAttribute(VALUE);
        String useFilterStr = element.getAttribute(CONSOLE_FILTER);
        String useMonitorStr = element.getAttribute(USE_MONITOR);
        String requestPortStr = element.getAttribute(REQUEST_PORT);
        boolean useFilter = Boolean.parseBoolean(useFilterStr);
        EspIdfTaskCommandNode taskTreeNode = new EspIdfTaskCommandNode(name, command, useFilter);

        boolean useMonitor = Boolean.parseBoolean(useMonitorStr);
        // 使用监视器必然需要请求串口
        boolean requestPort = useMonitor || Boolean.parseBoolean(requestPortStr);
        taskTreeNode.setUseMonitor(useMonitor);
        taskTreeNode.setRequestPort(requestPort);
        return buildNode(element, taskTreeNode);
    }

    private static NodeModel<Element> newAction(Element element) {
        String name = element.getAttribute(NAME);
        EspIdfTaskActionNode espIdfTaskActionNode = new EspIdfTaskActionNode(name);
        return buildNode(element, espIdfTaskActionNode);
    }

    private static NodeModel<Element> newConsoleCmd(Element element) {
        String name = element.getAttribute(NAME);
        String command = element.getAttribute(VALUE);
        String useTerminalStr = element.getAttribute(USE_TERMINAL);
        boolean useTerminal = useTerminalStr.isEmpty() || Boolean.parseBoolean(useTerminalStr);
        EspIdfTaskConsoleCommandNode taskTreeNode = new EspIdfTaskConsoleCommandNode(name, command, useTerminal);
        return buildNode(element, taskTreeNode);
    }

    private static NodeModel<Element> newRawCmd(Element element) {
        String name = element.getAttribute(NAME);
        String command = element.getAttribute(IS_WINDOWS ? WIN_VALUE : UNIX_VALUE);
        String commandDefault = element.getAttribute(VALUE);
        if (command.trim().isEmpty()) {
            command = commandDefault;
        }
        return buildNode(element, new RawCommandNode(name, command));
    }

    private static NodeModel<Element> newLocalExec(Element element) {
        String name = element.getAttribute(NAME);
        String path = element.getAttribute(EXEC_PATH).trim();
        String encoding = element.getAttribute(EXEC_ENCODING).trim();
        Element argTag = getFirstElementByName(element, EXEC_ARGS);
        String args = null == argTag ? element.getAttribute(EXEC_ARGS).trim() : argTag.getTextContent();
        LocalExecNode localExecNode = new LocalExecNode(name, path, args);
        if (StringUtils.isNotEmpty(encoding)) {
            localExecNode.setEncoding(encoding);
        }
        localExecNode.setUseTerminal(Boolean.parseBoolean(element.getAttribute(USE_TERMINAL)));
        localExecNode.setUseIdfEnv(Boolean.parseBoolean(element.getAttribute(EXEC_WITH_IDF_ENV)));
        return buildNode(element, localExecNode);
    }

    private static NodeModel<Element> buildNode(Element element, EspIdfTaskTreeNode taskTreeNode) {
        String toolTip = element.getAttribute(TOOL_TIP);
        taskTreeNode.setToolTip(getI18n(toolTip));
        taskTreeNode.setId(element.getAttribute(ID));
        taskTreeNode.setIcon(element.getAttribute(ICON));
        // mcp 属性缺省视为 true（暴露给 MCP）；仅 mcp="false" 才屏蔽
        taskTreeNode.setMcp(!"false".equalsIgnoreCase(element.getAttribute(MCP)));
        return new NodeModel<>(new DefaultMutableTreeNode(taskTreeNode), element);
    }

    public static String getI18n(String rawName) {
        if (rawName == null) {
            return null;
        }
        if (rawName.startsWith(RES_BUNDLE_EXP_START) && rawName.endsWith(RES_BUNDLE_EXP_END)) {
            return $i18n(rawName.substring(RES_BUNDLE_EXP_START.length(), rawName.length() - 1));
        }
        return rawName;
    }

    public static @NotNull List<DefaultMutableTreeNode> loadCustomTask(File taskXml) {
        Element documentElement;
        try {
            Document treeConf = DomUtil.parse(taskXml);
            documentElement = treeConf.getDocumentElement();

        } catch (Exception e) {
            NOTIFICATION_GROUP.createNotification($i18n("notification.group.idf"),
                    e.getMessage(), NotificationType.ERROR).notify(null);
            return List.of();
        }
        if (!ESP_TASKS_ROOT.equals(documentElement.getTagName())) {
            NOTIFICATION_GROUP.createNotification($i18n("notification.group.idf"),
                    $i18nF("idf.xml.load.failed", ESP_TASKS_ROOT), NotificationType.ERROR).notify(null);
            return List.of();
        }
        return loadCustomTaskFromElement(documentElement);
    }

    /**
     * 从已解析的根元素构建自定义任务树。不弹通知，供调用方在实时文档（编辑器未保存内容）
     * 解析失败时自行决定回退策略，避免用户正在输入导致 XML 暂不完整时产生噪音通知。
     */
    public static @NotNull List<DefaultMutableTreeNode> loadCustomTaskFromElement(@NotNull Element documentElement) {
        List<DefaultMutableTreeNode> defaultMutableTreeNodes = new ArrayList<>();
        eachChildrenElement(documentElement, (child) -> {
            String type = child.getTagName();
            Function<Element, NodeModel<Element>> elementNodeModelFunction = factories.get(type);
            if (elementNodeModelFunction == null) {
                return;
            }
            NodeModel<Element> childXmlNode = elementNodeModelFunction.apply(child);
            defaultMutableTreeNodes.add(childXmlNode.getNode());
        });
        return defaultMutableTreeNodes;
    }
}
