package org.btik.espidf.toolwindow;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.Tree;
import org.btik.espidf.toolwindow.tasks.EspIdfTaskTreeFactory;
import org.btik.espidf.toolwindow.tasks.TaskIconCellRenderer;
import org.btik.espidf.toolwindow.tasks.TreeNodeCmdExecutor;
import org.btik.espidf.toolwindow.tasks.TreeXmlMeta;
import org.btik.espidf.toolwindow.tasks.model.*;
import org.btik.espidf.util.I18nMessage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/18 13:05
 */
public class EspIdfToolWindowTaskPanel extends JScrollPane {
    private final Project project;
    private final Tree tree;
    private DefaultMutableTreeNode customTaskNode;
    private final TreeNode customTaskPreSetLastChildNode;

    private final HashMap<String, Runnable> actionMap = new HashMap<>();

    public EspIdfToolWindowTaskPanel(@NotNull Project project) {
        this.project = project;
        DefaultMutableTreeNode rootNode = EspIdfTaskTreeFactory.load();
        if (rootNode == null) {
            rootNode = new DefaultMutableTreeNode("load Failed");
        }
        viewport.setBorder(null);
        setBorder(BorderFactory.createEmptyBorder());
        tree = new Tree(rootNode);
        viewport.setView(tree);
        tree.expandPath(new TreePath(rootNode.getPath()));
        tree.setCellRenderer(new TaskIconCellRenderer());
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) path.getLastPathComponent();
                // 执行命令节点
                if (e.getClickCount() == 2) {
                    Object userObject = lastPathComponent.getUserObject();
                    if (userObject instanceof EspIdfTaskCommandNode commandNode) {
                        TreeNodeCmdExecutor.execute(commandNode, project);
                    } else if (userObject instanceof EspIdfTaskConsoleCommandNode taskTerminalCommandNode) {
                        TreeNodeCmdExecutor.execute(taskTerminalCommandNode, project);
                    } else if (userObject instanceof RawCommandNode rawCommandNode) {
                        TreeNodeCmdExecutor.execute(rawCommandNode, project);
                    } else if (userObject instanceof LocalExecNode localExecNode) {
                        TreeNodeCmdExecutor.execute(localExecNode, project);
                    } else if (userObject instanceof EspIdfTaskActionNode actionNode) {
                        Runnable runnable = actionMap.get(actionNode.getId());
                        if (runnable != null) {
                            runnable.run();
                            return;
                        }
                        TreeNodeCmdExecutor.execute(actionNode, project);
                    }
                }
            }
        });
        actionMap.put("idf.custom.tasks.load", this::loadCustomTask);
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            TreeNode child = rootNode.getChildAt(i);
            if (!(child instanceof DefaultMutableTreeNode defaultMutableTreeNode)) {
                continue;
            }
            Object userObject = defaultMutableTreeNode.getUserObject();
            if (!(userObject instanceof EspIdfTaskTreeNode taskTreeNode)) {
                continue;
            }
            if ("custom.tasks.folder".equals(taskTreeNode.getId())) {
                customTaskNode = defaultMutableTreeNode;
                break;
            }
        }
        if (customTaskNode == null) {
            throw new RuntimeException("customTask folder not found");
        }
        customTaskPreSetLastChildNode = customTaskNode.getLastChild();
        loadCustomTaskInit();
    }

    private void loadCustomTaskInit() {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        Path baseDir = Path.of(basePath);
        File taskXml = baseDir.resolve("esp_custom_tasks.xml").toFile();
        if (!taskXml.exists()) {
            return;
        }
        List<DefaultMutableTreeNode> defaultMutableTreeNodes = EspIdfTaskTreeFactory.loadCustomTask(taskXml);
        for (DefaultMutableTreeNode defaultMutableTreeNode : defaultMutableTreeNodes) {
            customTaskNode.add(defaultMutableTreeNode);
        }
        tree.updateUI();
    }

    private void loadCustomTask() {
        String basePath = project.getBasePath();
        if (basePath == null) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    $i18n("action.exec.base.path.notfound"),
                    NotificationType.ERROR).notify(project);
            return;
        }
        Path baseDir = Path.of(basePath);
        File taskXml = baseDir.resolve(TreeXmlMeta.ESP_CUSTOM_TASKS_XML).toFile();
        if (!taskXml.exists()) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    $i18n("action.exec.task.xml.notfound"),
                    NotificationType.ERROR).notify(project);
            return;
        }

        // 文件不是实时写入的，解析前需要保存
        VirtualFile xmlVirtual = VfsUtil.findFileByIoFile(taskXml, true);
        if (xmlVirtual == null) {
           return;
        }
        FileDocumentManager docManager = FileDocumentManager.getInstance();
        Document document = docManager.getCachedDocument(xmlVirtual);
        if (document != null) {
           docManager.saveDocument(document);
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            int lastIndex = customTaskNode.getIndex(customTaskPreSetLastChildNode);
            int childCount = customTaskNode.getChildCount();
            for (int i = childCount - 1; i > lastIndex; i--) {
                DefaultMutableTreeNode childToRemove = (DefaultMutableTreeNode) customTaskNode.getChildAt(i);
                customTaskNode.remove(childToRemove);
            }
            List<DefaultMutableTreeNode> defaultMutableTreeNodes = EspIdfTaskTreeFactory.loadCustomTask(taskXml);
            for (DefaultMutableTreeNode defaultMutableTreeNode : defaultMutableTreeNodes) {
                customTaskNode.add(defaultMutableTreeNode);
            }
            tree.updateUI();
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.task.xml.load.ok"),
                    $i18n("action.exec.task.xml.load.ok"),
                    NotificationType.INFORMATION).notify(project);
        });

    }
}
