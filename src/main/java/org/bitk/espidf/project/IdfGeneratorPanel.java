package org.bitk.espidf.project;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.*;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CMakeProjectGenerator;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.settings.ui.CMakeSettingsPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.bitk.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfGeneratorPanel extends CMakeSettingsPanel {

    private static final String ESP_IDF_JSON = "esp_idf.json";

    protected ComboBox<IdfFrameworkItem> idfFrameworks;
    private String idfToolPath;


    public IdfGeneratorPanel(@NotNull CMakeProjectGenerator cMakeProjectGenerator) {
        super(cMakeProjectGenerator);
    }

    @Override
    public void init(@NotNull CMakeProjectGenerator cMakeProjectGenerator) {
        if (!(cMakeProjectGenerator instanceof IdfProjectGenerator idfGenerator)) {
            super.init(cMakeProjectGenerator);
            return;
        }
        this.setLayout(new VerticalFlowLayout(0, 2));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(3, 2);
        JPanel wrapper = new JPanel(gridLayoutManager);
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        final TextFieldWithBrowseButton idfToolPathBrowserButton = new TextFieldWithBrowseButton();
        idfToolPathBrowserButton.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            private void handleChange() {
                idfToolPath = idfToolPathBrowserButton.getText();
                idfGenerator.setIdfToolsPath(idfToolPath);
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
            IdfFrameworkItem item = (IdfFrameworkItem) e.getItem();
            idfGenerator.setIdfId(item.idfId);
        });
        JLabel idfFrameworkLabel = new JLabel($i18n("idf.framework"));
        wrapper.add(idfFrameworkLabel, createConstraints(2, 0));
        wrapper.add(idfFrameworks, createConstraints(2, 1));
        this.add(wrapper, "West");
    }

    private void refreshIdfIdSet() {
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
            idfFrameworks.removeAllItems();
            idfInstalled.asMap().forEach((key, value) -> {
                IdfFrameworkItem idfFrameworkItem = new IdfFrameworkItem();
                idfFrameworks.addItem(idfFrameworkItem);
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
            });
            System.out.println(idfFrameworks.getItemCount());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    static class IdfFrameworkItem {
        private String displayName;

        private String version;

        private String idfId;

        @Override
        public String toString() {
            return displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getVersion() {
            return version;
        }

        public String getIdfId() {
            return idfId;
        }
    }
}
