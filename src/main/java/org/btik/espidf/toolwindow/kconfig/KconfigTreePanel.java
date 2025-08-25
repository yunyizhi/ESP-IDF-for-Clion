package org.btik.espidf.toolwindow.kconfig;

import com.intellij.ui.treeStructure.Tree;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigSetCommand;
import org.btik.espidf.toolwindow.kconfig.model.KconfigStatus;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;
import org.btik.espidf.ui.componets.TreeChoseListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import java.util.List;
import java.util.function.Consumer;

public class KconfigTreePanel extends JScrollPane {
    private final Tree tree;
    private DefaultMutableTreeNode rootNode;
    private final ConfModel treeRootModel;
    private final HashMap<String, DefaultMutableTreeNode> searchMap = new HashMap<>();
    private final TreeModel defaultTreeModel;
    private TreeChoseListener<ConfModel> treeChoseListener;
    private final KconfTreeCellRenderer kconfTreeCellRenderer;

    private final Consumer<KconfigSetCommand> commandSender;

    private boolean isTreeCheckEnabled = true;
    private long lastCheckTime = 0L;

    public KconfigTreePanel(ConfModel treeRootModel, Consumer<KconfigSetCommand> commandSender) {
        this.treeRootModel = treeRootModel;
        this.commandSender = commandSender;
        viewport.setBorder(null);
        setBorder(BorderFactory.createEmptyBorder());
        tree = new Tree();
        kconfTreeCellRenderer = new KconfTreeCellRenderer();
        tree.setCellRenderer(kconfTreeCellRenderer);
        defaultTreeModel = tree.getModel();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                TreePath path = tree.getPathForLocation(x, y);
                if (path == null) {
                    return;
                }
                int selRow = tree.getRowForLocation(x, y);
                TreePath selPath = tree.getPathForLocation(x, y);
                if (selRow == -1) {
                    return;
                }
                Rectangle pathBounds = tree.getPathBounds(selPath);
                if (pathBounds == null || !pathBounds.contains(x, y)) {
                    return;
                }
                DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = lastPathComponent.getUserObject();
                if (!(userObject instanceof ConfModel confModel)) {
                    return;
                }
                if (confModel.getRedefinedType() != KconfigType.ENABLE_SWITCH) {
                    return;
                }
                int checkBoxWidth = kconfTreeCellRenderer.getCheckBoxWidth(confModel.getId());
                if (x < pathBounds.x + checkBoxWidth && (isTreeCheckEnabled || (System.currentTimeMillis() - lastCheckTime) > 3000)) {
                    isTreeCheckEnabled = false;
                    KconfigSetCommand kconfigSetCommand = new KconfigSetCommand();
                    Object value = confModel.getValue();
                    kconfigSetCommand.setValues(Map.of(confModel.getId(), !Boolean.parseBoolean(String.valueOf(value))));
                    KconfigTreePanel.this.commandSender.accept(kconfigSetCommand);
                    lastCheckTime = System.currentTimeMillis();
                }
            }
        });
    }

    public void clear() {
        rootNode.removeAllChildren();
        rootNode = null;
        tree.updateUI();
        tree.setVisible(false);
    }

    public void onConfigNodesInit() {
        tree.setVisible(true);
        viewport.setView(tree);
        rootNode = KConfParser.buildTree(treeRootModel, searchMap);
        if (rootNode == null) {
            return;
        }
        TreeModel model = tree.getModel();
        if (model instanceof DefaultTreeModel treeModel) {
            treeModel.setRoot(rootNode);
        }

    }

    public void onConfigNodesChange(KconfigStatus status, HashMap<String, ConfModel> confModelMap) {
        try {
            Map<String, Boolean> visible = status.getVisible();
            visible.forEach((key, value) -> {
                ConfModel confModel = confModelMap.get(key);
                boolean wasVisible = confModel.isVisible();
                if (wasVisible == value) {
                    return;
                }
                confModel.setVisible(value);
                if (!confModel.isTreeNode()) {
                    return;
                }
                if (value) {
                    addNode(confModel, visible);
                } else {
                    DefaultMutableTreeNode defaultMutableTreeNode = searchMap.get(confModel.getParent().getId());
                    DefaultMutableTreeNode current = searchMap.get(confModel.getId());
                    if (defaultMutableTreeNode != null && current != null) {
                        if (defaultMutableTreeNode.isNodeChild(current)) {
                            defaultMutableTreeNode.remove(current);
                        }
                    }
                }
            });
            tree.updateUI();
        } finally {
            isTreeCheckEnabled = true;
        }
    }

    private DefaultMutableTreeNode addNode(ConfModel confModel, Map<String, Boolean> visible) {

        ConfModel parent = confModel.getParent();
        if (parent == null) {
            System.out.println(confModel.dump());
            return null;
        }
        String id = parent.getId();
        DefaultMutableTreeNode parentNode = searchMap.get(id);
        if (parentNode == null) {
            Boolean parentVisible = visible.get(id);
            if (parentVisible == null || !parentVisible) {
                return null;
            }
            parentNode = addNode(parent, visible);
            if (parentNode == null) {
                return null;
            }
        }


        String currentId = confModel.getId();
        DefaultMutableTreeNode defaultMutableTreeNode = searchMap.computeIfAbsent(currentId, k -> new DefaultMutableTreeNode(confModel));
        if (!defaultMutableTreeNode.isNodeChild(parentNode)) {
            parentNode.add(defaultMutableTreeNode);
        }
        return defaultMutableTreeNode;
    }

    public void addTreeSelectionListener(@NotNull TreeChoseListener<ConfModel> treeChoseListener) {
        this.treeChoseListener = treeChoseListener;
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selectedNode == null) {
                return;
            }

            Object userObject = selectedNode.getUserObject();
            if (!(userObject instanceof ConfModel confModel)) {
                return;
            }
            if (defaultTreeModel == tree.getModel()) {
                this.treeChoseListener.checkedTree(e, confModel);
            }
        });
    }

    public void jumpTo(ConfModel searchModel) {
            TreePath path = buildPathTo(searchModel);
            if (path == null) {
                return;
            }
            tree.scrollPathToVisible(path);
            tree.setSelectionPath(path);
            tree.expandPath(path);

    }
    public TreePath buildPathTo(ConfModel target) {
        if (target == null) {
            return null;
        }
        if (target.getRedefinedType() == KconfigType.CHOICE_ITEM) {
            target = target.getParent();
        }
        DefaultMutableTreeNode defaultMutableTreeNode = searchMap.get(target.getId());
        if (defaultMutableTreeNode == null) {
            return null;
        }
        List<TreeNode> path = new ArrayList<>();
        TreeNode current = defaultMutableTreeNode;
        while (current != null) {
            path.add(current);
            current = current.getParent();
        }

        Collections.reverse(path);
        return new TreePath(path.toArray());
    }
}
