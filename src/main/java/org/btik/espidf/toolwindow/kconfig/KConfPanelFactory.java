package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.CheckBox;
import com.intellij.util.ui.JBUI;
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        children.stream()
                .filter(ConfModel::isVisible)
                .filter(ConfModel::isLeaf)
                .forEach(item -> {
                    ItemCreator itemCreator = creators.get(item.getType());
                    panel.add(itemCreator.create(item));
                });

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
