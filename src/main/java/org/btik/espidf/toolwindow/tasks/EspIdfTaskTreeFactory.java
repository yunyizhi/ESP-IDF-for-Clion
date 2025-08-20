package org.btik.espidf.toolwindow.tasks;

import com.intellij.notification.NotificationType;
import org.btik.espidf.toolwindow.common.NodeModel;
import org.btik.espidf.toolwindow.tasks.model.*;
import org.btik.espidf.util.DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.HashMap;
import java.util.LinkedList;
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

    private static NodeModel<Element> buildNode(Element element, EspIdfTaskTreeNode taskTreeNode) {
        String toolTip = element.getAttribute(TOOL_TIP);
        if (toolTip.startsWith(RES_BUNDLE_EXP_START) && toolTip.endsWith(RES_BUNDLE_EXP_END)) {
            toolTip = $i18n(toolTip.substring(RES_BUNDLE_EXP_START.length(), toolTip.length() - 1));
        }
        taskTreeNode.setToolTip(toolTip);
        taskTreeNode.setId(element.getAttribute(ID));
        taskTreeNode.setIcon(element.getAttribute(ICON));
        return new NodeModel<>(new DefaultMutableTreeNode(taskTreeNode), element);
    }

}
