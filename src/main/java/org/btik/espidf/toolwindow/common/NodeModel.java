package org.btik.espidf.toolwindow.common;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author lustre
 * @since 2025/6/14 19:11
 */
public class NodeModel<T> {
    private DefaultMutableTreeNode node;
    private T model;

    public NodeModel() {
    }

    public NodeModel(DefaultMutableTreeNode node, T model) {
        this.node = node;
        this.model = model;
    }

    public DefaultMutableTreeNode getNode() {
        return node;
    }

    public void setNode(DefaultMutableTreeNode node) {
        this.node = node;
    }

    public T getModel() {
        return model;
    }

    public void setModel(T model) {
        this.model = model;
    }
}
