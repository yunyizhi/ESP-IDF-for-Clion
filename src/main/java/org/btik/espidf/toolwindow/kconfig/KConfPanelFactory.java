package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;

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
                System.out.println(type);
                continue;
            }
            JComponent comp = itemCreator.create(child);
            panel.add(comp);
        }
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private static JComponent boolItemCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox comp = new JCheckBox(confModel.getTitle());
        comp.setToolTipText(confModel.getHelp());
        wrapper.add(comp);
        wrapper.setSize(comp.getPreferredSize());
        return wrapper;
    }

    private static JComponent selectItemCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        ComboBox<ConfModel> comboBox = new ComboBox<>();
        List<ConfModel> children = confModel.getChildren();
        for (ConfModel child : children) {
            if (child.getType().equals(BOOL)) {
                comboBox.addItem(child);
            }
        }
        JLabel comp = new JLabel(confModel.getTitle() + ":");
        comp.setToolTipText(confModel.getHelp());
        wrapper.add(comp);
        wrapper.add(comboBox);
        wrapper.add(Box.createVerticalGlue());
        return wrapper;
    }

    private static JComponent hexCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JBTextField jTextField = new JBTextField();
        JLabel comp = new JLabel(confModel.getTitle() + ":");
        comp.setToolTipText(confModel.getHelp());
        wrapper.add(comp);
        wrapper.add(jTextField);
        wrapper.add(Box.createVerticalGlue());
        return wrapper;
    }

    private static JComponent intCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JBTextField jTextField = new JBTextField();
        JLabel comp = new JLabel(confModel.getTitle() + ":");
        comp.setToolTipText(confModel.getHelp());
        wrapper.add(comp);
        wrapper.add(jTextField);
        return wrapper;
    }

    private static JComponent stringCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JBTextField jTextField = new JBTextField();
        JLabel comp = new JLabel(confModel.getTitle() + ":");
        comp.setToolTipText(confModel.getHelp());
        wrapper.add(comp);
        wrapper.add(jTextField);
        return wrapper;
    }
}
