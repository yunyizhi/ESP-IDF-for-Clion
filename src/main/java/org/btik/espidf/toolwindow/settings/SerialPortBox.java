package org.btik.espidf.toolwindow.settings;

import com.intellij.openapi.ui.ComboBox;
import org.btik.espidf.toolwindow.settings.model.SerialPortInfo;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.util.List;

public class SerialPortBox extends ComboBox<SerialPortInfo> {

    public SerialPortBox() {
        setEditable(true);
        setLightWeightPopupEnabled(true);
        setRenderer(new SerialPortCellRenderer());
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
                if (lastSelectedItem != null) {
                    getEditor().setItem(lastSelectedItem.getComPort());
                }

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
