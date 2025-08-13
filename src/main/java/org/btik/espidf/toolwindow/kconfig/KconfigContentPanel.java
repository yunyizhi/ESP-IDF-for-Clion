package org.btik.espidf.toolwindow.kconfig;

import org.apache.commons.collections.CollectionUtils;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigSetCommand;
import org.btik.espidf.toolwindow.kconfig.model.KconfigStatus;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.function.Consumer;

public class KconfigContentPanel extends JScrollPane {
    private final CardLayout cardLayout;
    private final JPanel contentCards;

    private final HashMap<String, Component> viewMap = new HashMap<>();
    private static final String EMPTY = "empty";
    private final JPanel emptyPanel;

    private static final String FILTERED_TMP_PANEL_ID = "FILTERED_TMP_PANEL";
    private Component lastTmpView;

    private final Consumer<KconfigSetCommand> commandSender;

    private final HashMap<String, JComponent> confItemComponentMap = new HashMap<>();

    private String lastId;

    public KconfigContentPanel(JPanel view, CardLayout cardLayout, Consumer<KconfigSetCommand> commandSender) {
        super(view);
        this.contentCards = view;
        this.cardLayout = cardLayout;
        this.commandSender = commandSender;
        getVerticalScrollBar().setUnitIncrement(16);
        emptyPanel = new JPanel();
        contentCards.add(emptyPanel, EMPTY);
    }

    public void addToCard(Component comp, Object constraints) {
        contentCards.add(comp, constraints);
    }

    private void showEmpty() {
        cardLayout.show(contentCards, EMPTY);
        getViewport().setViewPosition(new Point(0, 0));
    }

    public void clear() {
        contentCards.removeAll();
        viewMap.clear();
        confItemComponentMap.clear();
        contentCards.add(emptyPanel, EMPTY);
    }

    public void showCard(ConfModel confModel) {
        String id = confModel.getId();
        try {
            if (confModel.isAsMenuPanelItem()) {
                showParentCard(confModel);
                return;
            }
            if (!confModel.isHasPanelItem() || CollectionUtils.isEmpty(confModel.getChildren())) {
                showEmpty();
                return;
            }
            if (!viewMap.containsKey(id)) {
                Component kConfPanel = KConfPanelFactory.createKConfPanel(confModel, commandSender, confItemComponentMap);
                if (kConfPanel == null) {
                    showEmpty();
                    return;
                }
                viewMap.put(id, kConfPanel);
                addToCard(kConfPanel, id);
            }
            cardLayout.show(contentCards, id);
            getViewport().setViewPosition(new Point(0, 0));
        } finally {
            lastId = id;
        }

    }

    private void showParentCard(ConfModel confModel) {
        ConfModel parent = confModel.getParent();
        showCard(parent);
        String id = confModel.getId();
        JComponent component = confItemComponentMap.get(id);
        if (component == null) {
            return;
        }
        component.requestFocus();
        component.scrollRectToVisible(new Rectangle(0, 0, component.getWidth(), component.getHeight()));
    }

    public void showCardWithSelectItem(ConfModel confModel) {
        if (lastTmpView != null) {
            contentCards.remove(lastTmpView);
        }
        Component kConfPanel = KConfPanelFactory.createKConfPanel(confModel, commandSender, confItemComponentMap);
        if (kConfPanel == null) {
            showEmpty();
            return;
        }
        lastTmpView = kConfPanel;
        addToCard(kConfPanel, FILTERED_TMP_PANEL_ID);
        cardLayout.show(contentCards, FILTERED_TMP_PANEL_ID);
        getViewport().setViewPosition(new Point(0, 0));
    }

    public void onConfigNodesChange(KconfigStatus status, HashMap<String, ConfModel> confModelMap) {
        if (lastId == null) {
            return;
        }
        contentCards.removeAll();
        contentCards.add(emptyPanel, EMPTY);
        viewMap.clear();
        ConfModel confModel = confModelMap.get(lastId);
        if (confModel == null) {
            showEmpty();
            return;
        }
        showCard(confModel);
    }
}