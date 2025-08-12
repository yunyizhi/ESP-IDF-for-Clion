package org.btik.espidf.toolwindow.kconfig;

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

    private String lastId;

    public KconfigContentPanel(JPanel view, CardLayout cardLayout,Consumer<KconfigSetCommand> commandSender) {
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
        contentCards.add(emptyPanel, EMPTY);
    }

    public void showCard(ConfModel confModel) {
        if (!confModel.isHasPanelItem()) {
            showEmpty();
            return;
        }
        String id = confModel.getId();
        if (!viewMap.containsKey(id)) {
            Component kConfPanel = KConfPanelFactory.createKConfPanel(confModel, commandSender);
            if (kConfPanel == null) {
                showEmpty();
                return;
            }
            viewMap.put(id, kConfPanel);
            addToCard(kConfPanel, id);
        }
        cardLayout.show(contentCards, id);
        getViewport().setViewPosition(new Point(0, 0));
        lastId = id;
    }

    public void showCardWithSelectItem(ConfModel confModel) {
        if (lastTmpView != null) {
            contentCards.remove(lastTmpView);
        }
        Component kConfPanel = KConfPanelFactory.createKConfPanel(confModel, commandSender);
        if (kConfPanel == null) {
            showEmpty();
            return;
        }
        lastTmpView = kConfPanel;
        addToCard(kConfPanel, FILTERED_TMP_PANEL_ID);
        cardLayout.show(contentCards,FILTERED_TMP_PANEL_ID);
        getViewport().setViewPosition(new Point(0, 0));
    }

    public void onConfigNodesChange(KconfigStatus status, HashMap<String, ConfModel> confModelMap) {
        if (lastId == null) {
            return;
        }
        contentCards.removeAll();
        viewMap.clear();
        ConfModel confModel = confModelMap.get(lastId);
        if (confModel == null) {
            showEmpty();
            return;
        }
        showCard(confModel);
    }
}