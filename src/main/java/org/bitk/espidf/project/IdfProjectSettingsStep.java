package org.bitk.espidf.project;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.ui.components.JBPanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.bitk.espidf.project.component.ComboBoxWithRefresh;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.bitk.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfProjectSettingsStep extends ProjectSettingsStepBase<String> {

    private JBPanel<?> panel;
    private static final String ESP_IDF_JSON = "esp_idf.json";

    protected ComboBox<IdfFrameworkItem> idfFrameworks;

    protected ComboBox<String> projectTargets;
    private String idfToolPath;

    private final IdfProjectGenerator idfProjectGenerator;

    public IdfProjectSettingsStep(DirectoryProjectGenerator<String> projectGenerator, AbstractNewProjectStep.AbstractCallback<String> callback) {
        super(projectGenerator, callback);
        this.idfProjectGenerator = (IdfProjectGenerator) projectGenerator;
    }
    public boolean isDumbAware() {
        return true;
    }
    @Override
    public JPanel createAdvancedSettings() {
        panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(3, 2);
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

        JLabel qtCMakePrefixLabel = new JLabel($i18n("idf.tools.path.title"));
        wrapper.add(qtCMakePrefixLabel, createConstraints(0, 0));
        GridConstraints firstRowConstraints = createConstraints(0, 1);
        firstRowConstraints.setFill(1);
        firstRowConstraints.setHSizePolicy(4);
        wrapper.add(idfToolPathBrowserButton, firstRowConstraints);
        idfFrameworks = new ComboBox<>();
        idfFrameworks.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            IdfFrameworkItem item = (IdfFrameworkItem) e.getItem();
            idfProjectGenerator.setIdfId(item.idfId);
            checkValid();
        });

        JLabel idfFrameworkLabel = new JLabel($i18n("idf.framework"));
        wrapper.add(idfFrameworkLabel, createConstraints(1, 0));
        wrapper.add(new ComboBoxWithRefresh<>(idfFrameworks, this::refreshIdfIdSet),
                createConstraints(1, 1));
        JLabel projectTargetLabel = new JLabel($i18n("project.target"));
        projectTargets = new ComboBox<>();
        projectTargets.addItem("esp32");
        wrapper.add(projectTargetLabel, createConstraints(2, 0));
        wrapper.add(new ComboBoxWithRefresh<>(projectTargets, this::refreshProjectTarget),
                createConstraints(2, 1));
        panel.add(wrapper, "West");
        return panel;
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
            if(idfFrameworks.getItemCount() > 0) {
                idfFrameworks.setSelectedIndex(0);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void refreshProjectTarget(){

    }

    private static @NotNull GridConstraints createConstraints(int row, int column) {
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(row);
        constraints.setColumn(column);
        constraints.setAnchor(8);
        return constraints;
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
