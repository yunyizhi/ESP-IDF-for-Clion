package org.btik.espidf.run.config.gdbinit;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.state.model.IdfProfileInfo;

import static org.btik.espidf.util.ListCellRendererAttr.*;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.util.List;

public class GdbInitPathBox extends ComboBox<GdbInitProfileInfo> {
    private static final String GDB_INIT_PATH_IN_BUILD = "/gdbinit/gdbinit";
    private Project project;

    private final GdbInitProfileInfoEditor gdbInitProfileInfoEditor;


    public GdbInitPathBox(@NotNull Project project) {
        gdbInitProfileInfoEditor = new GdbInitProfileInfoEditor();
        setEditor(gdbInitProfileInfoEditor);
        setEditable(true);
        setLightWeightPopupEnabled(true);
        setRenderer(new GdbInitProfileInfoRenderer());
        addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                Object selectedItem = getSelectedItem();
                GdbInitProfileInfo lastSelectedItem = null;
                if (selectedItem instanceof GdbInitProfileInfo gdbInitProfileInfo) {
                    lastSelectedItem = gdbInitProfileInfo;
                }
                removeAllItems();
                if (lastSelectedItem != null) {
                    getEditor().setItem(lastSelectedItem.path());
                }
                IdfProjectConfigService idfProjectConfigService = project.getService(IdfProjectConfigService.class);
                List<IdfProfileInfo> idfProfileInfoList = idfProjectConfigService.getIdfProfileInfoList();
                for (IdfProfileInfo idfProfileInfo : idfProfileInfoList) {
                    addItem(new GdbInitProfileInfo(
                            idfProfileInfo.getBuildDir() + GDB_INIT_PATH_IN_BUILD,
                            idfProfileInfo.getTarget(), idfProfileInfo.getDisplayName()));
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


    static class GdbInitProfileInfoRenderer implements ListCellRenderer<GdbInitProfileInfo> {


        @Override
        public Component getListCellRendererComponent(JList<? extends GdbInitProfileInfo> list, GdbInitProfileInfo value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.getAccessibleContext().setAccessibleName(value.path());
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            Color bg = isSelected ? list.getSelectionBackground() : list.getBackground();
            panel.setBackground(bg);

            SimpleColoredComponent primary = new SimpleColoredComponent();
            primary.setOpaque(false); // 透明背景，继承 panel 背景
            primary.setIpad(JBUI.emptyInsets());
            primary.append(value.path(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            SimpleColoredComponent secondary = new SimpleColoredComponent();
            secondary.setOpaque(false);
            secondary.setIpad(JBUI.emptyInsets()); // 更小内边距

            secondary.append(value.target(), GRAY_ITALIC_SMALL_ATTRIBUTES);
            secondary.append(" ");
            secondary.append(value.profileName(), GRAY_ITALIC_SMALL_ATTRIBUTES);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(primary);
            textPanel.add(secondary);

            panel.add(textPanel, BorderLayout.CENTER);


            return panel;
        }
    }

    static class GdbInitProfileInfoEditor extends BasicComboBoxEditor {
        public JTextField getTextField() {
            return editor;
        }
    }

    public void setCmakeBuildDir(String text) {
        gdbInitProfileInfoEditor.getTextField().setText(text + GDB_INIT_PATH_IN_BUILD);
    }

    public void setEditorText(String text) {
        gdbInitProfileInfoEditor.getTextField().setText(text);
    }

    public String getEditorText() {
        return gdbInitProfileInfoEditor.getTextField().getText();
    }

}
