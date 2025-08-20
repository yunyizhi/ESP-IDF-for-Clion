package org.btik.espidf.toolwindow.settings;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.toolwindow.settings.model.SerialPortInfo;

import javax.swing.*;
import java.awt.*;


public class SerialPortCellRenderer implements ListCellRenderer<SerialPortInfo> {

    public static final SimpleTextAttributes GRAY_ITALIC_SMALL_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER | SimpleTextAttributes.STYLE_ITALIC, JBColor.GRAY);

    @Override
    public Component getListCellRendererComponent(
            JList<? extends SerialPortInfo> list,
            SerialPortInfo value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        // 创建一个垂直布局的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.getAccessibleContext().setAccessibleName(value.getComPort());
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // 设置背景色
        Color bg = isSelected ? list.getSelectionBackground() : list.getBackground();
        panel.setBackground(bg);

        SimpleColoredComponent primary = new SimpleColoredComponent();
        primary.setOpaque(false); // 透明背景，继承 panel 背景
        primary.setIpad(JBUI.emptyInsets());

        SimpleColoredComponent secondary = new SimpleColoredComponent();
        secondary.setOpaque(false);
        secondary.setIpad(JBUI.emptyInsets()); // 更小内边距

        int vendorId = value.getVendorId();
        primary.setIcon(vendorId != -1 ? EspIdfIcon.USB : EspIdfIcon.SERIAL_PORT);
        primary.append(value.getComPort(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        primary.append("    ");
        if (value.getProductName() != null) {
            primary.append(value.getProductName(), SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
        }

        if (value.getVendorName() != null) {
            secondary.append(value.getVendorName(), SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
            secondary.append(" :",SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
        }
        if (value.getDescriptivePortName() != null) {
            secondary.append(value.getDescriptivePortName(), GRAY_ITALIC_SMALL_ATTRIBUTES);
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(primary);
        textPanel.add(secondary);

        panel.add(textPanel, BorderLayout.CENTER);

        return panel;
    }
}
