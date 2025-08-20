package org.btik.espidf.toolwindow.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.btik.espidf.toolwindow.settings.model.SerialPortInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.util.List;

public class SerialPortBox extends ComboBox<SerialPortInfo> {

    public SerialPortBox() {
        setEditable(true);
        setRenderer(new ColoredListCellRenderer<>() {

            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends SerialPortInfo> list, SerialPortInfo value, int index, boolean selected, boolean hasFocus) {
                append(value.getComPort(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                append(" ");
                if (value.getProductName() != null) {
                    append(value.getProductName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    append("|");
                }
                if (value.getVendorName() != null) {
                    append(value.getVendorName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    append(" ");
                }
                if (value.getDescriptivePortName() != null) {
                    append(value.getDescriptivePortName(), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
                }

            }
        });
        addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                Object selectedItem = getSelectedItem();
                SerialPortInfo lastSelectedItem = null;
                if (selectedItem instanceof SerialPortInfo serialPortInfo) {
                    lastSelectedItem = serialPortInfo;
                }
                removeAllItems();
                List<SerialPortInfo> serialPortInfos = SerialPortLoader.getSerialPortInfo();
                for (SerialPortInfo serialPortInfo : serialPortInfos) {
                    addItem(serialPortInfo);
                }
                getEditor().setItem(lastSelectedItem);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
    }

}
