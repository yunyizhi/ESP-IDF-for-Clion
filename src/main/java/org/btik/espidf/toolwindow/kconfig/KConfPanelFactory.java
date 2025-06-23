package org.btik.espidf.toolwindow.kconfig;

import org.btik.espidf.toolwindow.kconfig.model.ConfModel;

import java.awt.*;
import java.util.List;

public class KConfPanelFactory {
    public static Component createKConfPanel(ConfModel confModel) {
        List<ConfModel> children = confModel.getChildren();
        children.stream().filter(ConfModel::isVisible).filter(ConfModel::isLeaf).forEach(System.out::println);
        return null;
    }
}
