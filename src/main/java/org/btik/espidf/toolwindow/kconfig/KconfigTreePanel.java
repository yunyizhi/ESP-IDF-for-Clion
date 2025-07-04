package org.btik.espidf.toolwindow.kconfig;

import com.intellij.ui.treeStructure.Tree;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;
import org.btik.espidf.ui.componets.TreeChoseListener;

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
        tree.expandRow(0);
        root.forEach((item) -> {
            rootNode.add(item);
            rootModels.add((ConfModel) item.getUserObject());
        });
        viewport.setView(tree);
    }

    public void addTreeSelectionListener(TreeChoseListener<ConfModel> treeChoseListener) {
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selectedNode == null) {
                return;
            }
            Object userObject = selectedNode.getUserObject();
            if (userObject instanceof ConfModel confModel) {
                treeChoseListener.checkedTree(e, confModel);
            }
        });
    }

    public void onTreeSearchResult(Consumer<ConfModel> treeSearchListener) {
        this.treeSearchListener = treeSearchListener;
    }

    public void filterTree(String keyword) {

        if (keyword == null || keyword.isEmpty()) {
            tree.setModel(defaultTreeModel);
            return;
        }
        ConfModel targetModel = new ConfModel();
        treeRootModel.copyTo(targetModel);
        boolean hasMatches = filterSubtree(treeRootModel, targetModel, keyword);
        if (hasMatches) {
            DefaultMutableTreeNode filteredRootNode = KConfParser.buildTree(targetModel);
            expandFirst(filteredRootNode);
            tree.setModel(new DefaultTreeModel(filteredRootNode));
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
        boolean isMatched = isMatch(sourceModel, keyword);
        boolean hasChildrenMatched = false;
        List<ConfModel> newChildren = new ArrayList<>();

        List<ConfModel> children = sourceModel.getChildren();
        if (children != null) {
            if (sourceModel.getType() == KconfigType.CHOICE) {
                newChildren.addAll(children);
            } else  {
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
        if (isMatched || hasChildrenMatched) {
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
