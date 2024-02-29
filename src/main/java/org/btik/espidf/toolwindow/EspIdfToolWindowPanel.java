package org.btik.espidf.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.btik.espidf.toolwindow.tree.EspIdfTaskTreeFactory;
import org.btik.espidf.toolwindow.tree.IconCellRenderer;
import org.btik.espidf.toolwindow.tree.TreeNodeCmdExecutor;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskTerminalCommandNode;
import org.btik.espidf.toolwindow.tree.model.RawCommandNode;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/18 13:05
 */
public class EspIdfToolWindowPanel extends BorderLayoutPanel {
    private final Project project;
    private final ComboBox<GeneratorItem> generatorBox;

    private String generatorValue;

    public EspIdfToolWindowPanel(Project project) {
        this.project = project;
        generatorBox = new ComboBox<>();
        initGenerator();
        JPanel horizontalPanel = new BorderLayoutPanel();
        Border leftPaddingBorder = BorderFactory.createEmptyBorder(0, 15, 0, 0);
        horizontalPanel.setBorder(leftPaddingBorder);
        horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
        horizontalPanel.add(new JLabel($i18n("idf.generator")));
        horizontalPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        horizontalPanel.add(generatorBox);
        horizontalPanel.add(Box.createHorizontalGlue());
        addToTop(horizontalPanel);
        addToCenter(initTaskTree());

    }

    private Tree initTaskTree() {
        DefaultMutableTreeNode root = EspIdfTaskTreeFactory.load();
        if (root == null) {
            root = new DefaultMutableTreeNode("load Failed");
        }
        Tree tree = new Tree(root);
        tree.setCellRenderer(new IconCellRenderer());

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
                        TreeNodeCmdExecutor.execute(commandNode, project, generatorValue);
                    } else if (userObject instanceof EspIdfTaskTerminalCommandNode taskTerminalCommandNode) {
                        TreeNodeCmdExecutor.execute(taskTerminalCommandNode, project, generatorValue);
                    } else if (userObject instanceof RawCommandNode rawCommandNode) {
                        TreeNodeCmdExecutor.execute(rawCommandNode, project);
                    }
                }
            }
        });
        return tree;
    }

    private void initGenerator() {
        String generatorsStr = $sys(IS_WINDOWS ? "idf.win.generators" : "idf.unix.generators");
        String[] generators = generatorsStr.split("\\|");
        for (String generator : generators) {
            generatorBox.addItem(new GeneratorItem(generator));
        }
        generatorBox.addItemListener((event) -> {
            if (event.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            generatorValue = ((GeneratorItem) event.getItem()).value;
        });
        GeneratorItem selectedItem = (GeneratorItem) generatorBox.getSelectedItem();
        if (selectedItem != null) {
            generatorValue = selectedItem.value;
        }

    }

    static class GeneratorItem {
        String displayName;
        String value;

        @Override
        public String toString() {
            return displayName;
        }

        public GeneratorItem(String value) {
            value = value.trim();
            this.displayName = value;
            this.value = value;
        }
    }
}
