package org.btik.espidf.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import org.btik.espidf.toolwindow.tree.EspIdfTaskTreeFactory;
import org.btik.espidf.toolwindow.tree.IconCellRenderer;
import org.btik.espidf.toolwindow.tree.TreeNodeCmdExecutor;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskActionNode;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskConsoleCommandNode;
import org.btik.espidf.toolwindow.tree.model.RawCommandNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author lustre
 * @since 2024/2/18 13:05
 */
public class EspIdfToolWindowTaskPanel extends JScrollPane {
    private final Project project;

    public EspIdfToolWindowTaskPanel( @NotNull Project project) {
        this.project = project;
        DefaultMutableTreeNode root = EspIdfTaskTreeFactory.load();
        if (root == null) {
            root = new DefaultMutableTreeNode("load Failed");
        }
        viewport.setBorder(null);
        setBorder(BorderFactory.createEmptyBorder());
        Tree tree = new Tree(root);
        viewport.setView(tree);
        tree.setCellRenderer(new IconCellRenderer());
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
                    } else if (userObject instanceof EspIdfTaskActionNode actionNode) {
                        TreeNodeCmdExecutor.execute(actionNode, project);
                    }
                }
            }
        });
    }
}
