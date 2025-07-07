package org.btik.espidf.toolwindow.kconfig;

import com.intellij.ui.treeStructure.Tree;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;
import org.btik.espidf.ui.componets.TreeChoseListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import javax.swing.tree.*;
import java.util.ArrayList;

import java.util.List;
import java.util.function.Consumer;

import static org.btik.espidf.util.I18nMessage.$i18n;

public class KconfigTreePanel extends JScrollPane {
    List<DefaultMutableTreeNode> root;
    List<ConfModel> rootModels = new ArrayList<>();
    private final Tree tree;
    private final DefaultMutableTreeNode rootNode;
    private final ConfModel treeRootModel;
    private final TreeModel defaultTreeModel;
    private Consumer<ConfModel> treeSearchListener;
    private TreeChoseListener<ConfModel> treeChoseListener;

    public KconfigTreePanel(ConfModel treeRootModel) {
        this.treeRootModel = treeRootModel;
        viewport.setBorder(null);
        setBorder(BorderFactory.createEmptyBorder());
        rootNode = new DefaultMutableTreeNode(treeRootModel);
        tree = new Tree(rootNode);
        defaultTreeModel = tree.getModel();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    public void setRoot(List<DefaultMutableTreeNode> root) {
        this.root = root;
        root.forEach((item) -> {
            rootNode.add(item);
            rootModels.add((ConfModel) item.getUserObject());
        });
        viewport.setView(tree);
        tree.expandPath(new TreePath(rootNode.getPath()));
    }

    public void addTreeSelectionListener(@NotNull  TreeChoseListener<ConfModel> treeChoseListener) {
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
            treeChoseListener.checkedTree(null, treeRootModel);
            return;
        }
        ConfModel targetModel = new ConfModel();
        treeRootModel.copyTo(targetModel);
        boolean hasMatches = filterSubtree(treeRootModel, targetModel, keyword);
        if (hasMatches) {
            DefaultMutableTreeNode filteredRootNode = KConfParser.buildTree(targetModel);
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
            // 下拉菜单本身被命中需要保留所有选项 但下拉菜单依然需要
            if (sourceModel.getType() == KconfigType.CHOICE) {
                for (ConfModel child : children) {
                    newChildren.add(child);
                    if (isMatch(child, keyword)) {
                        hasChildrenMatched = true;
                    }
                }
            } else {
                for (ConfModel child : children) {
                    ConfModel newChild = new ConfModel();
                    newChild.setParent(targetModel);
                    boolean childMatched = filterSubtree(child, newChild, keyword);
                    if (childMatched) {
                        newChildren.add(newChild);
                        hasChildrenMatched = true;
                    }
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

    private boolean isMatch(ConfModel model, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true;

        String lowerKeyword = keyword.toLowerCase();
        return (model.getId() != null && model.getId().toLowerCase().contains(lowerKeyword)) ||
                (model.getName() != null && model.getName().toLowerCase().contains(lowerKeyword)) ||
                (model.getTitle() != null && model.getTitle().toLowerCase().contains(lowerKeyword));
    }
}
