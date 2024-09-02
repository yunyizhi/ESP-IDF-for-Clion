package org.btik.espidf.project;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.ui.components.JBPanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.project.component.ComboBoxWithRefresh;
import org.btik.espidf.service.IdfToolConfService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;


import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.UIUtils.createConstraints;
import static org.btik.espidf.util.UIUtils.i18nLabel;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfWindowsProjectSettingsStep<T> extends IdfProjectSettingsStep<T> {

    private static final String ESP_IDF_JSON = "esp_idf.json";

    protected ComboBox<IdfFrameworkItem> idfFrameworks;
    private ComboBoxWithRefresh<IdfFrameworkItem> idfFrameworkItemComboBox;
    private JLabel idfFrameworkLabel;

    private ComboBox<IdfEnvType> idfPathType;
    private TextFieldWithBrowseButton idfToolPathBrowserButton;
    private String idfToolPath;

    public IdfWindowsProjectSettingsStep(DirectoryProjectGenerator<T> projectGenerator, AbstractNewProjectStep.AbstractCallback<T> callback) {
        super(projectGenerator, callback);
    }

    @Override
    public JPanel createAdvancedSettings() {
        JBPanel<?> panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(5, 3, JBUI.emptyInsets(), 2 ,2);
        JPanel wrapper = new JPanel(gridLayoutManager);
        int rowIndex = 0;

        initIdfEnvType();
        wrapper.add(i18nLabel("idf.env.type.title"), createConstraints(rowIndex, 0));
        wrapper.add(idfPathType, createConstraints(rowIndex, 1));
        rowIndex++;
        
        wrapper.add(i18nLabel("idf.path.title"), createConstraints(rowIndex, 0));
        initIdfPathBrowser();
        GridConstraints pathCell = createConstraints(rowIndex, 1);
        pathCell.setFill(GridConstraints.FILL_HORIZONTAL);
        pathCell.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        wrapper.add(idfToolPathBrowserButton, pathCell);
        rowIndex++;

        initIdfFrameworkComboBox();
        wrapper.add(idfFrameworkLabel, createConstraints(rowIndex, 0));
        idfFrameworkItemComboBox = new ComboBoxWithRefresh<>(idfFrameworks, this::refreshIdfIdSet);
        wrapper.add(idfFrameworkItemComboBox, createConstraints(rowIndex, 1));
        rowIndex++;

        wrapper.add(i18nLabel("idf.env.type.target"), createConstraints(rowIndex, 0));
        initIdfTargets();
        wrapper.add(idfTargets, createConstraints(rowIndex, 1));
        rowIndex++;

        GridConstraints targetTipCell = createConstraints(rowIndex, 0);
        targetTipCell.setColSpan(2);
        wrapper.add(i18nLabel("idf.env.type.target.tip"), targetTipCell);
        setLastValue(idfToolPathBrowserButton);
        panel.add(wrapper, BorderLayout.WEST);
        return panel;
    }

    private void initIdfFrameworkComboBox() {
        idfFrameworks = new ComboBox<>();
        idfFrameworks.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            IdfFrameworkItem item = (IdfFrameworkItem) e.getItem();
            idfProjectGenerator.setIdfId(item.idfId);
            checkValid();
        });

        idfFrameworkLabel = i18nLabel("idf.framework");
    }

    private void initIdfPathBrowser() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        idfToolPathBrowserButton = new TextFieldWithBrowseButton();
        idfToolPathBrowserButton.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            private void handleChange() {
                idfToolPath = idfToolPathBrowserButton.getText();
                idfProjectGenerator.setIdfToolsPath(idfToolPath);
                checkValid();
            }

            public void insertUpdate(DocumentEvent e) {
                this.handleChange();
            }

            public void removeUpdate(DocumentEvent e) {
                this.handleChange();
            }

            public void changedUpdate(DocumentEvent e) {
                this.handleChange();
            }
        });
        idfToolPathBrowserButton.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>(
                $i18n("select.idf.tools.path"), $i18n("select.idf.tools.path.for.idf"),
                idfToolPathBrowserButton, null, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                refreshIdfIdSet();
            }
        });
    }

    private void initIdfEnvType() {
        idfPathType = new ComboBox<>();
        idfPathType.addItem(IdfEnvType.IDF_TOOL);
        idfPathType.addItem(IdfEnvType.IDF_FRAMEWORK);
        idfPathType.addItemListener(this::envTypeChange);
    }

    private void setLastValue(TextFieldWithBrowseButton idfToolPathBrowserButton) {
        IdfToolConfService service = ApplicationManager.getApplication().getService(IdfToolConfService.class);
        IdfToolConf idfToolConf = service.getLastActivedIdfToolConf();
        if (idfToolConf != null) {
            idfToolPathBrowserButton.getTextField().setText(idfToolConf.getIdfToolPath());
            if (StringUtil.isEmpty(idfToolConf.getIdfId())) {
                idfPathType.setSelectedItem(IdfEnvType.IDF_FRAMEWORK);
            } else {
                refreshIdfIdSet();
            }

        }
    }

    private void envTypeChange(ItemEvent e) {
        if (e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        IdfEnvType envType = (IdfEnvType) e.getItem();
        idfProjectGenerator.setEnvType(envType);
        if (envType == IdfEnvType.IDF_FRAMEWORK) {
            idfFrameworkItemComboBox.setVisible(false);
            idfFrameworkLabel.setVisible(false);
        } else {
            idfFrameworkItemComboBox.setVisible(true);
            idfFrameworkLabel.setVisible(true);
        }
        checkValid();
    }

    private void refreshIdfIdSet() {
        idfFrameworks.removeAllItems();
        if (StringUtil.isEmpty(idfToolPath)) {
            checkValid();
            return;
        }
        Path path = Path.of(idfToolPath, ESP_IDF_JSON);
        if (!Files.exists(path)) {
            return;
        }
        Gson gson = new Gson();

        try {
            String json = Files.readString(path);
            JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonObject idfInstalled = jsonObject.get("idfInstalled").getAsJsonObject();
            ArrayList<IdfFrameworkItem> idfFrameworksItems = new ArrayList<>();
            idfInstalled.asMap().forEach((key, value) -> {
                IdfFrameworkItem idfFrameworkItem = new IdfFrameworkItem();
                idfFrameworkItem.idfId = key;
                JsonObject idfInfo = value.getAsJsonObject();
                idfFrameworkItem.version = idfInfo.get("version").getAsString();
                String idfPath = idfInfo.get("path").getAsString();
                String[] split = idfPath.split("[\\\\|/]");
                if (split[split.length - 1].isEmpty()) {
                    idfFrameworkItem.displayName = split[split.length - 2];
                } else {
                    idfFrameworkItem.displayName = split[split.length - 1];
                }
                idfFrameworksItems.add(idfFrameworkItem);
            });
            if (!idfFrameworksItems.isEmpty()) {
                idfFrameworksItems.stream()
                        .sorted()
                        .forEach(idfFrameworks::addItem);
                idfFrameworks.setSelectedIndex(0);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static class IdfFrameworkItem implements Comparable<IdfFrameworkItem> {
        private String displayName;

        private String version;

        private String idfId;

        @Override
        public String toString() {
            return displayName;
        }

        @Override
        public int compareTo(@NotNull IdfWindowsProjectSettingsStep.IdfFrameworkItem o) {
            if (StringUtil.isEmpty(o.displayName)) {
                return -1;
            }
            return o.displayName.compareTo(displayName);
        }
    }

}
