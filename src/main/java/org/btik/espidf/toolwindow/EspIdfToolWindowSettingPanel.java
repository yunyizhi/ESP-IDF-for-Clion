package org.btik.espidf.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;

import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

import static org.btik.espidf.service.IdfEnvironmentService.*;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;

public class EspIdfToolWindowSettingPanel extends JPanel {
    private final Project project;

    private final JBTextField portField = new JBTextField();
    private final ComboBox<String> monitorBaud = new ComboBox<>();
    private final ComboBox<String> uploadBaud = new ComboBox<>();
    private final JButton saveButton = new JButton();
    private final IdfProjectConfigService idfProjectConfigService;
    private final IdfProjectConfig projectConfigModel;

    public EspIdfToolWindowSettingPanel(Project project) {
        super(new VerticalFlowLayout(0, 2));
        this.project = project;
        this.idfProjectConfigService = project.getService(IdfProjectConfigService.class);
        projectConfigModel = new IdfProjectConfig();
        initUI();

    }

    private void initUI() {
        JPanel wrapper = new JPanel(new GridLayoutManager(4, 2, JBUI.insets(16, 16, 0, 16), -1, -1));

        int rowIndex = 0;

        wrapper.add(new JLabel($i18n("idf.project.setting.port")), createConstraints(rowIndex, 0));
        GridConstraints firstRowConstraints = createConstraints(rowIndex, 1);
        firstRowConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        firstRowConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        wrapper.add(portField, firstRowConstraints);
        rowIndex++;

        wrapper.add(new JLabel($i18n("idf.project.setting.monitor.baud")), createConstraints(rowIndex, 0));
        wrapper.add(monitorBaud, createConstraints(rowIndex, 1));
        rowIndex++;

        wrapper.add(new JLabel($i18n("idf.project.setting.upload.baud")), createConstraints(rowIndex, 0));
        wrapper.add(uploadBaud, createConstraints(rowIndex, 1));

        rowIndex++;
        saveButton.setText($i18n("idf.project.setting.save"));
        saveButton.setEnabled(false);
        wrapper.add(saveButton, createConstraints(rowIndex, 1));

        add(wrapper, BorderLayout.WEST);

        initValues();

        bindAction();

    }

    private void initValues() {
        // 初始化波特率
        String baudRates = $sys("idf.baud.rates");
        String[] baudRateArr = baudRates.trim().split(",");
        monitorBaud.setModel(new DefaultComboBoxModel<>(baudRateArr));
        uploadBaud.setModel(new DefaultComboBoxModel<>(baudRateArr));

        // 优先读取项目配置，但凡有一个值配置过，就不读取环境变量

        IdfProjectConfig projectConfig = idfProjectConfigService.getProjectConfig();
        if (projectConfig != null && !projectConfig.isEmpty()) {
            portField.setText(projectConfig.getPort());
            monitorBaud.setSelectedItem(projectConfig.getMonitorBaud());
            uploadBaud.setSelectedItem(projectConfig.getUploadBaud());
            return;
        }
        // 没有配置则读取环境变量
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);

        Map<String, String> environments = environmentService.getEnvironments();
        String port = environments.get(ESP_PORT);
        if (!StringUtil.isEmpty(port)) {
            portField.setText(port);
        }
        String monitorBaudValue = environments.get(MONITOR_BAUD);
        if (StringUtil.isEmpty(monitorBaudValue)) {
            monitorBaudValue = environments.get(IDF_MONITOR_BAUD);
        }
        if (!StringUtil.isEmpty(monitorBaudValue)) {
            monitorBaud.setSelectedItem(monitorBaudValue);
        }
        String uploadBaudValue = environments.get(ESP_BAUD);
        if (!StringUtil.isEmpty(uploadBaudValue)) {
            uploadBaud.setSelectedItem(uploadBaudValue);
        }
    }

    private void bindAction() {
        saveButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                saveButton.setEnabled(false);
                projectConfigModel.setPort(portField.getText());
                setBaudStrFormObj(monitorBaud.getSelectedItem(), projectConfigModel::setMonitorBaud);
                setBaudStrFormObj(uploadBaud.getSelectedItem(), projectConfigModel::setUploadBaud);
                idfProjectConfigService.updateProjectConfig(projectConfigModel);
            }
        });

        portField.getDocument().addDocumentListener(new DocumentAdapter() {

            @Override
            protected void textChanged(@NotNull DocumentEvent documentEvent) {
                projectConfigModel.setPort(portField.getText());
                saveButton.setEnabled(idfProjectConfigService.hasValueChange(projectConfigModel));
            }
        });
        monitorBaud.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                projectConfigModel.setMonitorBaud((String) monitorBaud.getSelectedItem());
                saveButton.setEnabled(idfProjectConfigService.hasValueChange(projectConfigModel));
            }
        });
        uploadBaud.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                projectConfigModel.setUploadBaud((String) uploadBaud.getSelectedItem());
                saveButton.setEnabled(idfProjectConfigService.hasValueChange(projectConfigModel));
            }
        });
    }


    private static @NotNull GridConstraints createConstraints(int row, int column) {
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(row);
        constraints.setColumn(column);
        constraints.setAnchor(GridConstraints.ANCHOR_WEST);
        return constraints;
    }

    private static void setBaudStrFormObj(Object baudObj, @NotNull Consumer<String> setter) {
        if (baudObj != null) {
            String baudObjString = baudObj.toString();
            if (StringUtil.isEmpty(baudObjString)) {
                return;
            }
            setter.accept(baudObjString);
        }
    }
}
