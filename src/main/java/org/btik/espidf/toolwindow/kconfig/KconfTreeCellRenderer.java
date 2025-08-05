package org.btik.espidf.toolwindow.kconfig;

import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class KconfTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component treeCellRendererComponent = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (node == null) {
            return treeCellRendererComponent;
        }
        Object userObject = node.getUserObject();
        if (!(userObject instanceof ConfModel confModel)) {
            return treeCellRendererComponent;
        }
        KconfigType redefinedType = confModel.getRedefinedType();
        JLabel jLabel = new JLabel(confModel.getTitle());
        jLabel.setToolTipText(confModel.getHelp());
        if (redefinedType != KconfigType.ENABLE_SWITCH) {
            return jLabel;
        }
        JCheckBox comp = new JCheckBox();
        comp.setText(confModel.getTitle());
        comp.setToolTipText(confModel.getHelp());
        comp.setSelected(Boolean.parseBoolean(String.valueOf(confModel.getValue())));
        return comp;
    }
}
