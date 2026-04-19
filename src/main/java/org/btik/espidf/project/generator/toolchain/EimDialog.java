package org.btik.espidf.project.generator.toolchain;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.common.reflect.TypeToken;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.ui.componets.GridPanel;
import org.btik.espidf.ui.componets.TextFieldFileChooser;
import org.btik.espidf.util.UIUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.btik.espidf.ui.componets.DocumentChangeListener.bindDocChange;
import static org.btik.espidf.ui.componets.SelectedItemListener.selectedListener;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.util.ListCellRendererAttr.GRAY_ITALIC_SMALL_ATTRIBUTES;

public class EimDialog extends DialogWrapper {
    private static final String EIM_IDF_JSON = "eim_idf.json";
    private final TextFieldFileChooser eimToolsFolderBrowserButton = new TextFieldFileChooser();
    private final JBTextField toolChainName = new JBTextField();
    private final ComboBox<EimIdfItemInfo> eimIdfItemInfoComboBox = new ComboBox<>();

    protected EimDialog(Component parent, @NotNull String title) {
        super(parent, true);
        this.setTitle(title);
        init();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }


    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridPanel gridPanel = new GridPanel(3, 2);
        eimToolsFolderBrowserButton.addActionListener(null, FileChooserDescriptorFactory.singleFile().withFileFilter(file -> EIM_IDF_JSON.equals(file.getName()))
                .withTitle($i18n("idf.select.eim.json.path"))
                .withDescription($i18n("idf.select.eim.json.path.desc")));
        gridPanel.addNewFormRow("idf.eim.json.path", eimToolsFolderBrowserButton, true);
        gridPanel.addNewFormRow("idf.toolchain.name", toolChainName, true);
        gridPanel.addNewFormRow("idf.framework", eimIdfItemInfoComboBox, true);
        eimIdfItemInfoComboBox.setRenderer(new EimItemListCellRenderer());
        eimIdfItemInfoComboBox.setEditable(false);
        eimIdfItemInfoComboBox.setLightWeightPopupEnabled(true);
        bindDocChange(eimToolsFolderBrowserButton, e -> refreshIdfs());
        eimIdfItemInfoComboBox.addItemListener(selectedListener(e ->{
            Object item = e.getItem();
            if (!(item instanceof EimIdfItemInfo eimIdfItemInfo)){
                return;
            }
            if (StringUtils.isEmpty(toolChainName.getText())){
                toolChainName.setText("ESP-IDF " + eimIdfItemInfo.name());
            }
        }));
        UIUtils.setWidth(eimIdfItemInfoComboBox, 300);
        panel.add(gridPanel, BorderLayout.WEST);
        UIUtils.setWidth(gridPanel, 500);
        return panel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String eimJsonPath = eimToolsFolderBrowserButton.getText();
        if (StringUtils.isEmpty(eimJsonPath)) {
            return new ValidationInfo($i18n("idf.eim.json.empty"), eimToolsFolderBrowserButton);
        }
        Path path = Path.of(eimJsonPath);
        if (!Files.exists(path)) {
            return new ValidationInfo($i18n("idf.eim.json.not.exists"), eimToolsFolderBrowserButton);
        }
        Object selectedItem = eimIdfItemInfoComboBox.getSelectedItem();
        if (!(selectedItem instanceof EimIdfItemInfo eimIdfItemInfo)) {
            return new ValidationInfo($i18n("idf.not.selected"), eimIdfItemInfoComboBox);
        }
        if (!Files.exists(Path.of(eimIdfItemInfo.activationScript))) {
            return new ValidationInfo($i18nF("idf.eim.active.script.not.exists", eimIdfItemInfo.activationScript), eimIdfItemInfoComboBox);
        }
        if (!Files.exists(Path.of(eimIdfItemInfo.path))) {
            return new ValidationInfo($i18nF("idf.eim.path.not.exists", eimIdfItemInfo.path), eimIdfItemInfoComboBox);
        }

        return super.doValidate();
    }

    public String getToolChainName() {
        return toolChainName.getText();
    }

    public String getScriptPath() {
        Object selectedItem = eimIdfItemInfoComboBox.getSelectedItem();
        if (!(selectedItem instanceof EimIdfItemInfo eimIdfItemInfo)) {
            return null;
        }
        return eimIdfItemInfo.activationScript();
    }


    private void refreshIdfs() {
        eimIdfItemInfoComboBox.removeAllItems();
        String eimJsonPath = eimToolsFolderBrowserButton.getText();
        if (StringUtil.isEmpty(eimJsonPath)) {
            return;
        }
        Path path = Path.of(eimJsonPath);
        if (!Files.exists(path)) {
            return;
        }
        Gson gson = new Gson();
        try {
            String json = Files.readString(path);
            JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
            JsonObject rootObject = jsonElement.getAsJsonObject();
            JsonElement idfInstalled = rootObject.get("idfInstalled");
            List<EimIdfItemInfo> idfInfoList = gson.fromJson(idfInstalled, new TypeToken<List<EimIdfItemInfo>>() {
            }.getType());
            for (EimIdfItemInfo idfInfo : idfInfoList) {
                eimIdfItemInfoComboBox.addItem(idfInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public record EimIdfItemInfo(String activationScript, String id, String idfToolsPath, String name, String path,
                                 String python) {
        @Override
        public @NonNull String toString() {
            return name;
        }
    }


    public static class EimItemListCellRenderer implements ListCellRenderer<EimIdfItemInfo> {

        @Override
        public Component getListCellRendererComponent(JList<? extends EimIdfItemInfo> list, EimIdfItemInfo idfInfo, int index, boolean isSelected, boolean cellHasFocus) {
            if (idfInfo == null) {
                return new JPanel(new BorderLayout());
            }
            JPanel panel = new JPanel(new BorderLayout());
            panel.getAccessibleContext().setAccessibleName(idfInfo.name());
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            Color bg = isSelected ? list.getSelectionBackground() : list.getBackground();
            panel.setBackground(bg);

            SimpleColoredComponent primary = new SimpleColoredComponent();
            primary.setOpaque(false); // 透明背景，继承 panel 背景
            primary.setIpad(JBUI.emptyInsets());
            primary.append(idfInfo.name(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            SimpleColoredComponent secondary = new SimpleColoredComponent();
            secondary.setOpaque(false);
            secondary.setIpad(JBUI.emptyInsets()); // 更小内边距
            secondary.append(idfInfo.path(), GRAY_ITALIC_SMALL_ATTRIBUTES);
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