package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.util.Pair;
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

import static org.btik.espidf.util.I18nMessage.$i18n;

public class KconfigTreePanel extends JScrollPane {
    private final Tree tree;
    private DefaultMutableTreeNode rootNode;
    private final ConfModel treeRootModel;
    private final HashMap<String, DefaultMutableTreeNode> searchMap = new HashMap<>();
    private final HashMap<String, DefaultMutableTreeNode> filteredSearchMap = new HashMap<>();
    private final TreeModel defaultTreeModel;
    private Consumer<ConfModel> treeSearchListener;
    private TreeChoseListener<ConfModel> treeChoseListener;

    private ConfModel lastCheckedModel;
    private final Consumer<KconfigSetCommand> commandSender;

    public KconfigTreePanel(ConfModel treeRootModel, Consumer<KconfigSetCommand> commandSender) {
        this.treeRootModel = treeRootModel;
        this.commandSender = commandSender;
        viewport.setBorder(null);
        setBorder(BorderFactory.createEmptyBorder());
        tree = new Tree();
        tree.setCellRenderer(new KconfTreeCellRenderer());
        defaultTreeModel = tree.getModel();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                // 设置复选框选中
                DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = lastPathComponent.getUserObject();
                if (!(userObject instanceof ConfModel confModel)) {
                    return;
                }
                tree.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        int x = e.getX();
                        int y = e.getY();
                        int selRow = tree.getRowForLocation(x, y);
                        TreePath selPath = tree.getPathForLocation(x, y);
                        if (selRow != -1) {
                            Rectangle pathBounds = tree.getPathBounds(selPath);
                            if (pathBounds != null && pathBounds.contains(x, y)) {
                                if (x < pathBounds.x + 20) {
                                    KconfigSetCommand kconfigSetCommand = new KconfigSetCommand();
                                    Object value = confModel.getValue();
                                    kconfigSetCommand.setValues(Map.of(confModel.getId(), !Boolean.parseBoolean(String.valueOf(value))));
                                    commandSender.accept(kconfigSetCommand);
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    public void clear() {
        rootNode.removeAllChildren();
        tree.setModel(defaultTreeModel);
        tree.updateUI();
    }

    public void onConfigNodesInit() {
        viewport.setView(tree);
        rootNode = KConfParser.buildTree(treeRootModel, searchMap);
        if (rootNode == null) {
            return;
        }
        TreeModel model = tree.getModel();
        if (model instanceof DefaultTreeModel treeModel) {
            treeModel.setRoot(rootNode);
        }
        if (lastCheckedModel != null) {
            treeSearchListener.accept(lastCheckedModel);
        }

    }

    public void onConfigNodesChange(KconfigStatus status, HashMap<String, ConfModel> confModelMap) {
        Map<String, Object> values = status.getValues();
        values.forEach((key, value) -> {
            ConfModel confModel = confModelMap.get(key);
            if (confModel == null) {
                return;
            }
            confModel.setValue(value);
        });
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
                    if (defaultMutableTreeNode.isNodeChild(current)){
                        defaultMutableTreeNode.remove(current);
                    }
                }
            }
        });
        tree.updateUI();

    }

    private void addNode(ConfModel confModel, Map<String, Boolean> visible) {

        ConfModel parent = confModel.getParent();
        if (parent == null) {
            System.out.println(confModel.dump());
            return;
        }
        String id = parent.getId();
        DefaultMutableTreeNode parentNode = searchMap.get(id);
        if (parentNode == null) {
            Boolean parentVisible = visible.get(id);
            if (parentVisible == null || !parentVisible) {
                return;
            }
            addNode(parent, visible);
            return;
        }
        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(confModel);
        parentNode.add(newChild);
        searchMap.put(confModel.getId(), newChild);
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
            lastCheckedModel = confModel;
            if (defaultTreeModel == tree.getModel()) {
                treeChoseListener.checkedTree(e, confModel);
                return;
            }
            if (treeSearchListener != null) {
                treeSearchListener.accept(confModel);
            }
        });
    }

    public void onTreeSearchResult(Consumer<ConfModel> treeSearchListener) {
        this.treeSearchListener = treeSearchListener;
    }

    public void filterTree(String keyword) {

        if (keyword == null || keyword.isEmpty()) {
            tree.setModel(defaultTreeModel);
            tree.expandPath(new TreePath(rootNode.getPath()));
            lastCheckedModel = treeRootModel;
            treeChoseListener.checkedTree(null, treeRootModel);
            return;
        }
        ConfModel targetModel = new ConfModel();
        treeRootModel.copyTo(targetModel);
        boolean hasMatches = filterSubtree(treeRootModel, targetModel, keyword);
        if (hasMatches) {
            filteredSearchMap.clear();
            DefaultMutableTreeNode filteredRootNode = KConfParser.buildTree(targetModel, filteredSearchMap);
            tree.setModel(new DefaultTreeModel(filteredRootNode));
            expandFirst(filteredRootNode);
        } else {
            DefaultMutableTreeNode emptyRoot = new DefaultMutableTreeNode($i18n("esp.idf.kconfig.search.not.found"));
            tree.setModel(new DefaultTreeModel(emptyRoot));
        }
    }

    private void expandFirst(DefaultMutableTreeNode filteredRootNode) {
        if (filteredRootNode == null) {
            return;
        }
        DefaultMutableTreeNode firstLeaf = filteredRootNode.getFirstLeaf();
        if (firstLeaf == null) {
            return;
        }
        TreePath treePath = new TreePath(firstLeaf.getPath());
        tree.expandPath(treePath);
        Object userObject = firstLeaf.getUserObject();
        if (treeSearchListener != null && userObject instanceof ConfModel confModel) {
            treeSearchListener.accept(confModel);
        }
    }

    private boolean filterSubtree(ConfModel sourceModel, ConfModel targetModel, String keyword) {
        if (!sourceModel.isVisible()) {
            return false;
        }
        boolean isCurrentMatched = isMatch(sourceModel, keyword);
        boolean hasChildrenMatched = false;
        List<ConfModel> newChildren = new ArrayList<>();

        List<ConfModel> children = sourceModel.getChildren();
        if (children != null) {
            for (ConfModel child : children) {
                ConfModel newChild = new ConfModel();
                newChild.setParent(targetModel);
                boolean childMatched = filterSubtree(child, newChild, keyword);
                if (childMatched) {
                    hasChildrenMatched = true;
                    newChildren.add(newChild);
                } else if (isCurrentMatched && sourceModel.isHasPanelItem()) {
                    deepCopy(child, newChild);
                    newChildren.add(newChild);
                }
            }
        }

        if (isCurrentMatched || hasChildrenMatched) {
            sourceModel.copyTo(targetModel);
            if (!newChildren.isEmpty()) {
                targetModel.setChildren(newChildren);
            } else {
                targetModel.setChildren(null);
            }
            return true;
        }
        return false;
    }

    private void deepCopy(ConfModel sourceModel, ConfModel targetModel) {
        if (sourceModel == null || targetModel == null) {
            return;
        }
        LinkedList<Pair<ConfModel, ConfModel>> queue = new LinkedList<>();
        queue.offer(Pair.create(sourceModel, targetModel));
        while (!queue.isEmpty()) {
            Pair<ConfModel, ConfModel> pair = queue.poll();
            ConfModel source = pair.getFirst();
            ConfModel target = pair.getSecond();
            source.copyTo(target);
            List<ConfModel> children = source.getChildren();
            if (children != null && !children.isEmpty()) {
                List<ConfModel> newChildren = new ArrayList<>();
                for (ConfModel child : children) {
                    ConfModel newChild = new ConfModel();
                    newChildren.add(newChild);
                    queue.offer(Pair.create(child, newChild));
                }
                target.setChildren(newChildren);
            }
        }
    }

    private boolean isMatch(ConfModel model, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true;

        String lowerKeyword = keyword.toLowerCase();
        return (model.getId() != null && model.getId().toLowerCase().contains(lowerKeyword)) ||
                (model.getName() != null && model.getName().toLowerCase().contains(lowerKeyword)) ||
                (model.getTitle() != null && model.getTitle().toLowerCase().contains(lowerKeyword));
    }
}
