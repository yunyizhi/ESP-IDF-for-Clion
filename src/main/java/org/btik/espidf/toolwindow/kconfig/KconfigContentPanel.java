package org.btik.espidf.toolwindow.kconfig;

import org.btik.espidf.toolwindow.kconfig.model.ConfModel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class KconfigContentPanel extends JScrollPane {
    private final CardLayout cardLayout;
    private final JPanel contentCards;

    private final HashMap<String, Component> viewMap = new HashMap<>();
    private static final String EMPTY = "empty";
    private final JPanel emptyPanel;

    private static final String FILTERED_TMP_PANEL_ID = "FILTERED_TMP_PANEL";
    private Component lastTmpView;

    public KconfigContentPanel(JPanel view, CardLayout cardLayout) {
        super(view);
        this.contentCards = view;
        this.cardLayout = cardLayout;
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
        if (!viewMap.containsKey(confModel.getId())) {
            Component kConfPanel = KConfPanelFactory.createKConfPanel(confModel);
            if (kConfPanel == null) {
                showEmpty();
                return;
            }
            viewMap.put(confModel.getId(), kConfPanel);
            addToCard(kConfPanel, confModel.getId());
        }
        cardLayout.show(contentCards, confModel.getId());
        getViewport().setViewPosition(new Point(0, 0));
    }

    public void showCardWithSelectItem(ConfModel confModel) {
        if (lastTmpView != null) {
            contentCards.remove(lastTmpView);
        }
        Component kConfPanel = KConfPanelFactory.createKConfPanel(confModel);
        if (kConfPanel == null) {
            showEmpty();
            return;
        }
        lastTmpView = kConfPanel;
        addToCard(kConfPanel, FILTERED_TMP_PANEL_ID);
        cardLayout.show(contentCards,FILTERED_TMP_PANEL_ID);
        getViewport().setViewPosition(new Point(0, 0));
    }
}