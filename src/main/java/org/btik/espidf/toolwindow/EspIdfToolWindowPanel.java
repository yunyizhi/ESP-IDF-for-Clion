package org.btik.espidf.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import org.btik.espidf.toolwindow.tree.EspIdfTaskTreeFactory;
import org.btik.espidf.toolwindow.tree.IconCellRenderer;
import org.btik.espidf.toolwindow.tree.TreeNodeCmdExecutor;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskCommandNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author lustre
 * @since 2024/2/18 13:05
 */
public class EspIdfToolWindowPanel extends JPanel {
    private final Project project;

    public EspIdfToolWindowPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        DefaultMutableTreeNode root = EspIdfTaskTreeFactory.load();
        if (root == null) {
            root = new DefaultMutableTreeNode("load Failed");
        }
        Tree tree = new Tree(root);
        tree.setCellRenderer(new IconCellRenderer());
        add(tree, BorderLayout.CENTER);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                // 设置复选框选中
                DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) path.getLastPathComponent();
                // 执行命令节点
                if (e.getClickCount() == 2) {
                    Object userObject = lastPathComponent.getUserObject();
                    if (userObject instanceof EspIdfTaskCommandNode commandNode) {
                        TreeNodeCmdExecutor.execute(commandNode, project);
                    }
                }
            }
        });
    }
}
