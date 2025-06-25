package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.ui.ComboBox;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

import static org.btik.espidf.toolwindow.kconfig.model.KconfigType.*;

public class KConfPanelFactory {
    interface ItemCreator {
        Component create(ConfModel confModel);
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
            panel.add(creators.get(type).create(child));
        }
        return panel;
    }

    private static Component boolItemCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox comp = new JCheckBox(confModel.getTitle());
        comp.setToolTipText(confModel.getHelp());
        wrapper.add(comp);
        return wrapper;
    }

    private static Component selectItemCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ComboBox<String> comboBox = new ComboBox<>();
        return wrapper;
    }

    private static Component hexCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));

        return wrapper;
    }

    private static Component intCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));

        return wrapper;
    }

    private static Component stringCreator(ConfModel confModel) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));

        return wrapper;
    }
}
