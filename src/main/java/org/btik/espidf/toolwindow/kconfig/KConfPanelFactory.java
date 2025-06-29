package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;
import org.btik.espidf.ui.componets.InsertPanel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

import static org.btik.espidf.toolwindow.kconfig.model.KconfigType.*;

public class KConfPanelFactory {
    interface ItemCreator {
        JComponent create(ConfModel confModel);
    }

    private static final HashMap<KconfigType, ItemCreator> creators = new HashMap<>();

    static {
        creators.put(BOOL, KConfPanelFactory::boolItemCreator);
        creators.put(CHOICE, KConfPanelFactory::selectItemCreator);
        creators.put(HEX, KConfPanelFactory::hexCreator);
        creators.put(INT, KConfPanelFactory::intCreator);
        creators.put(STRING, KConfPanelFactory::stringCreator);
        creators.put(MENU, KConfPanelFactory::menuItemCreator);
    }

    public static Component createKConfPanel(ConfModel confModel) {
        List<ConfModel> children = confModel.getChildren();

        List<ConfModel> filteredList = children.stream()
                .filter(ConfModel::isVisible)
                .filter(ConfModel::isAsMenuPanelItem)
                .toList();
        if (filteredList.isEmpty()) {
            return null;
        }
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (ConfModel child : filteredList) {
            KconfigType type = child.getType();
            ItemCreator itemCreator = creators.get(type);
            if (itemCreator == null) {
                System.out.println(child.getId());
                continue;
            }
            JComponent comp = itemCreator.create(child);
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(comp);
        }
        return panel;
    }

    private static void addComponent(JPanel panel, Component component, int gridx, int gridy) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(2, 10, 0, 0); // 内边距
        panel.add(component, gbc);
    }

    private static JComponent boolItemCreator(ConfModel confModel) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JCheckBox comp = new JCheckBox(confModel.getTitle());
        comp.setToolTipText(confModel.getHelp());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(2, 10, 0, 0); // 内边距
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        wrapper.add(comp, gbc);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        return wrapper;
    }

    private static JComponent selectItemCreator(ConfModel confModel) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JLabel label = new JLabel(confModel.getTitle() + ":");
        label.setToolTipText(confModel.getHelp());

        ComboBox<ConfModel> comboBox = new ComboBox<>();
        List<ConfModel> children = confModel.getChildren();
        FontMetrics fm = comboBox.getFontMetrics(comboBox.getFont());
        int width = 200 ;
        final int paddingAndLogo = 50;
        for (ConfModel child : children) {
            if (child.getType().equals(BOOL)) {
                comboBox.addItem(child);
                width = Math.max(fm.stringWidth(child.toString()) + paddingAndLogo, width);
            }
        }
        comboBox.setPreferredSize(new Dimension(width, 30));
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        addComponent(wrapper, label, 0, 0);
        addComponent(wrapper, comboBox, 0, 1);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        return wrapper;
    }

    private static JComponent hexCreator(ConfModel confModel) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JLabel label = new JLabel(confModel.getTitle() + ":");
        label.setToolTipText(confModel.getHelp());

        JBTextField textField = new JBTextField();
        textField.setPreferredSize(new Dimension(200, 30));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        addComponent(wrapper, label, 0, 0);
        addComponent(wrapper, textField, 0, 1);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        return wrapper;
    }

    private static JComponent intCreator(ConfModel confModel) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JLabel label = new JLabel(confModel.getTitle() + ":");
        label.setToolTipText(confModel.getHelp());

        JBTextField textField = new JBTextField();
        textField.setPreferredSize(new Dimension(200, 30));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        addComponent(wrapper, label, 0, 0);
        addComponent(wrapper, textField, 0, 1);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        return wrapper;
    }

    private static JComponent stringCreator(ConfModel confModel) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JLabel label = new JLabel(confModel.getTitle() + ":");
        label.setToolTipText(confModel.getHelp());

        JBTextField textField = new JBTextField();
        textField.setPreferredSize(new Dimension(200, 30));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        addComponent(wrapper, label, 0, 0);
        addComponent(wrapper, textField, 0, 1);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        return wrapper;
    }

    private static JComponent menuItemCreator(ConfModel confModel) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JCheckBox comp = new JCheckBox(confModel.getTitle());
        comp.setToolTipText(confModel.getHelp());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(2, 10, 0, 0); // 内边距
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        wrapper.add(comp, gbc);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        return wrapper;
    }
}