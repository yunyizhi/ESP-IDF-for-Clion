package org.btik.espidf.project.generator.idfenv;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

import static org.btik.espidf.util.ListCellRendererAttr.GRAY_ITALIC_SMALL_ATTRIBUTES;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;

public class IdfEnvComboBox extends ComboBox<IdfEnvConf> {
    public IdfEnvComboBox() {
        setEditable(false);
        setRenderer(new IdfEnvCellRenderer());

    }

    static class IdfEnvCellRenderer implements ListCellRenderer<IdfEnvConf> {

        @Override
        public Component getListCellRendererComponent(JList<? extends IdfEnvConf> list, IdfEnvConf idfEnvConf, int index, boolean isSelected, boolean cellHasFocus) {
            if (idfEnvConf == null) {
                return new JPanel(new BorderLayout());
            }
            JPanel panel = new JPanel(new BorderLayout());
            panel.getAccessibleContext().setAccessibleName(idfEnvConf.getPath());
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            Color bg = isSelected ? list.getSelectionBackground() : list.getBackground();
            panel.setBackground(bg);

            SimpleColoredComponent primary = new SimpleColoredComponent();
            primary.setOpaque(false); // 透明背景，继承 panel 背景
            primary.setIpad(IS_WINDOWS ? new JBInsets(2, 0, 2, 0) : JBUI.emptyInsets());
            primary.append(idfEnvConf.getPath(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            SimpleColoredComponent secondary = new SimpleColoredComponent();
            secondary.setOpaque(false);
            secondary.setIpad(JBUI.emptyInsets()); // 更小内边距
            secondary.append(idfEnvConf.getVersion(), GRAY_ITALIC_SMALL_ATTRIBUTES);
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(primary);
            textPanel.add(secondary);

            panel.add(textPanel, BorderLayout.CENTER);
            return panel;
        }
    }
}
