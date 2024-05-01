package org.btik.espidf.project;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.CapturingProcessRunner;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.project.component.ComboBoxWithRefresh;
import org.btik.espidf.service.IdfToolConfService;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.EnvironmentVarUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.OsUtil.Const.*;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfWindowsProjectSettingsStep<T> extends IdfProjectSettingsStep<T> {

    private static final String ESP_IDF_JSON = "esp_idf.json";

    protected ComboBox<IdfFrameworkItem> idfFrameworks;
    private ComboBoxWithRefresh<IdfFrameworkItem> idfFrameworkItemComboBox;
    private JLabel idfFrameworkLabel;

    protected ComboBox<IdfEnvType> idfPathType;
    private String idfToolPath;

    public IdfWindowsProjectSettingsStep(DirectoryProjectGenerator<T> projectGenerator, AbstractNewProjectStep.AbstractCallback<T> callback) {
        super(projectGenerator, callback);
    }

    @Override
    public JPanel createAdvancedSettings() {
        JBPanel<?> panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(4, 3);
        JPanel wrapper = new JPanel(gridLayoutManager);
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        final TextFieldWithBrowseButton idfToolPathBrowserButton = new TextFieldWithBrowseButton();
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
        int rowIndex = 0;
        JLabel installTypeLabel = new JLabel($i18n("idf.env.type.title"));
        idfPathType = new ComboBox<>();
        idfPathType.addItem(IdfEnvType.IDF_TOOL);
        idfPathType.addItem(IdfEnvType.IDF_FRAMEWORK);
        idfPathType.addItemListener(this::envTypeChange);
        wrapper.add(installTypeLabel, createConstraints(rowIndex, 0));
        wrapper.add(idfPathType, createConstraints(rowIndex, 1));
        rowIndex++;
        JLabel idfToolPrefixLabel = new JLabel($i18n("idf.path.title"));
        wrapper.add(idfToolPrefixLabel, createConstraints(rowIndex, 0));
        GridConstraints firstRowConstraints = createConstraints(rowIndex, 1);
        firstRowConstraints.setFill(1);
        firstRowConstraints.setHSizePolicy(4);
        wrapper.add(idfToolPathBrowserButton, firstRowConstraints);
        rowIndex++;
        idfFrameworks = new ComboBox<>();
        idfFrameworks.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            IdfFrameworkItem item = (IdfFrameworkItem) e.getItem();
            idfProjectGenerator.setIdfId(item.idfId);
            checkValid();
        });

        idfFrameworkLabel = new JLabel($i18n("idf.framework"));
        wrapper.add(idfFrameworkLabel, createConstraints(rowIndex, 0));
        idfFrameworkItemComboBox = new ComboBoxWithRefresh<>(idfFrameworks, this::refreshIdfIdSet);
        wrapper.add(idfFrameworkItemComboBox,
                createConstraints(rowIndex, 1));
        panel.add(wrapper, "West");

        IdfToolConfService service = ApplicationManager.getApplication().getService(IdfToolConfService.class);
        IdfToolConf idfToolConf = service.getLastActivedIdfToolConf();
        if (idfToolConf != null) {
            idfToolPathBrowserButton.getTextField().setText(idfToolConf.getIdfToolPath());
            refreshIdfIdSet();
        }
        return panel;
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
                idfFrameworks.addItem(idfFrameworkItem);
            });
            if (idfFrameworks.getItemCount() > 0) {
                idfFrameworks.setSelectedIndex(0);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static class IdfFrameworkItem {
        private String displayName;

        private String version;

        private String idfId;

        @Override
        public String toString() {
            return displayName;
        }
    }

}
