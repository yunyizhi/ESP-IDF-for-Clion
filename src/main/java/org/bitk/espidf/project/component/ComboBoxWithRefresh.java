package org.bitk.espidf.project.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author lustre
 * @since 2024/2/10 10:19
 */
public class ComboBoxWithRefresh<T> extends JPanel {
    private final ComboBox<T> comboBox;

    private final Runnable clickEventHandler;

    public ComboBoxWithRefresh(ComboBox<T> comboBox, Runnable clickEventHandler) {
        this.comboBox = comboBox;
        this.clickEventHandler = clickEventHandler;
        initUI();
    }

    private void initUI() {
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        setLayout(flowLayout);
        flowLayout.setHgap(0);
        flowLayout.setVgap(0);
        JLabel refresh = new JLabel(AllIcons.Actions.Refresh);
        add(comboBox);
        add(new JLabel("  "));
        add(refresh);
        refresh.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               if (e.getClickCount() == 1) {
                   clickEventHandler.run();
               }
            }
        });
    }

}
