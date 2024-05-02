package org.btik.espidf.toolwindow.tree;

import com.intellij.notification.NotificationType;
import org.btik.espidf.toolwindow.tree.model.*;
import org.btik.espidf.util.DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Function;

import static org.btik.espidf.toolwindow.tree.TreeXmlMeta.*;
import static org.btik.espidf.util.DomUtil.eachChildrenElement;
import static org.btik.espidf.util.DomUtil.getFirstElementByName;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.NOTIFICATION_GROUP;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;

/**
 * @author lustre
 * @since 2024/2/18 15:16
 */
public class EspIdfTaskTreeFactory {
    private static final HashMap<String, Function<Element, XmlNode>> factories = new HashMap<>();

    static {
        factories.put(FOLDER_TAG, EspIdfTaskTreeFactory::newFolder);
        factories.put(COMMAND_TAG, EspIdfTaskTreeFactory::newCmd);
        factories.put(TERMINAL_COMMAND, EspIdfTaskTreeFactory::newTerminalCmd);
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
                    $i18n("tree.load.failed"), NotificationType.ERROR).notify(null);
            return null;
        }

        return build(treeRoot);
    }

    private static DefaultMutableTreeNode build(Element treeRoot) {
        XmlNode rootNode = newFolder(treeRoot);
        LinkedList<XmlNode> treeNodeQueue = new LinkedList<>();
        treeNodeQueue.add(rootNode);
        while (!treeNodeQueue.isEmpty()) {
            XmlNode xmlNode = treeNodeQueue.removeFirst();
            eachChildrenElement(xmlNode.element, (child) -> {
                String type = child.getTagName();
                XmlNode childXmlNode = factories.get(type).apply(child);
                xmlNode.node.add(childXmlNode.node);
                treeNodeQueue.add(childXmlNode);
            });
        }

        return rootNode.node;
    }

    static class XmlNode {
        Element element;
        DefaultMutableTreeNode node;

        public XmlNode(Element element, DefaultMutableTreeNode parent) {
            this.element = element;
            this.node = parent;
        }
    }

    private static XmlNode newFolder(Element element) {
        String name = element.getAttribute(NAME);
        return buildNode(element, new EspIdfTaskFolderNode(name));
    }

    private static XmlNode newCmd(Element element) {
        String name = element.getAttribute(NAME);
        String command = element.getAttribute(VALUE);
        EspIdfTaskCommandNode taskTreeNode = new EspIdfTaskCommandNode(name, command);
        return buildNode(element, taskTreeNode);
    }

    private static XmlNode newAction(Element element) {
        String name = element.getAttribute(NAME);
        EspIdfTaskActionNode espIdfTaskActionNode = new EspIdfTaskActionNode(name);
        return buildNode(element, espIdfTaskActionNode);
    }

    private static XmlNode newTerminalCmd(Element element) {
        String name = element.getAttribute(NAME);
        String command = element.getAttribute(VALUE);
        EspIdfTaskTerminalCommandNode taskTreeNode = new EspIdfTaskTerminalCommandNode(name, command);
        return buildNode(element, taskTreeNode);
    }

    private static XmlNode newRawCmd(Element element) {
        String name = element.getAttribute(NAME);
        String command = element.getAttribute(IS_WINDOWS ? WIN_VALUE : UNIX_VALUE);
        return buildNode(element, new RawCommandNode(name, command));
    }

    private static XmlNode buildNode(Element element, EspIdfTaskTreeNode taskTreeNode) {
        String toolTip = element.getAttribute(TOOL_TIP);
        if (toolTip.startsWith(RES_BUNDLE_EXP_START) && toolTip.endsWith(RES_BUNDLE_EXP_END)) {
            toolTip = $i18n(toolTip.substring(RES_BUNDLE_EXP_START.length(), toolTip.length() - 1));
        }
        taskTreeNode.setToolTip(toolTip);
        taskTreeNode.setId(element.getAttribute(ID));
        taskTreeNode.setIcon(element.getAttribute(ICON));
        return new XmlNode(element,
                new DefaultMutableTreeNode(taskTreeNode));
    }

}
