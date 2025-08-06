package org.btik.espidf.toolwindow.kconfig;

import com.intellij.icons.AllIcons;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.function.Consumer;

public class KconfTreeCellRenderer extends DefaultTreeCellRenderer {

    private final HashMap<String, Integer> checkBoxWidth = new HashMap<>();

    public int getCheckBoxWidth(String id) {
        Integer width = checkBoxWidth.get(id);
        if (width == null) {
            width = 20;
        }
        return width;
    }

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

        if (redefinedType != KconfigType.ENABLE_SWITCH) {
            JLabel jLabel = new JLabel(confModel.getTitle());
            jLabel.setToolTipText(confModel.getHelp());
            setIcon(confModel, jLabel::setIcon);
            return jLabel;
        }
        JCheckBox comp = new JCheckBox();
        comp.setText(confModel.getTitle());
        comp.setToolTipText(confModel.getHelp());
        comp.setSelected(Boolean.parseBoolean(String.valueOf(confModel.getValue())));
        checkBoxWidth.put(confModel.getTitle(), comp.getPreferredSize().width);
        setIcon(confModel, comp::setIcon);
        return comp;
    }

    private void setIcon(ConfModel confModel, Consumer<Icon> setter) {
        if (confModel.isHasPanelItem()) {
            setter.accept(AllIcons.Nodes.Editorconfig);
        }
    }
}
