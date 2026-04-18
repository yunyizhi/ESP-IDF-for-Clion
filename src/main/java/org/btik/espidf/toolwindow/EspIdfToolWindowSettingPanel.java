package org.btik.espidf.toolwindow;

import com.intellij.execution.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;

import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.btik.espidf.toolwindow.settings.SerialPortBox;
import org.btik.espidf.toolwindow.settings.model.SerialPortInfo;
import org.btik.espidf.ui.componets.GridPanel;
import org.btik.espidf.util.EspIdfProjectUtil;
import org.btik.espidf.util.UIUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.service.IdfEnvironmentService.*;
import static org.btik.espidf.ui.componets.MouseHooks.mouseClicked;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;
import static org.btik.espidf.util.UIUtils.i18nLabel;

public class EspIdfToolWindowSettingPanel extends JPanel {
    private final Project project;

    private final SerialPortBox portField = new SerialPortBox();
    private final ComboBox<String> monitorBaud = new ComboBox<>();
    private final ComboBox<String> uploadBaud = new ComboBox<>();
    private final ComboBox<String> cmakeProfile = new ComboBox<>();
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
        GridPanel wrapper = new GridPanel(new GridLayoutManager(6, 2, JBUI.insets(16, 16, 0, 16), -1, -1));

        wrapper.addNewFormRow("idf.project.setting.port", portField, true);
        portField.setEditable(true);
        UIUtils.setWidth(portField, 300);
        wrapper.addNewFormRow("idf.project.setting.monitor.baud", monitorBaud, false);
        wrapper.addNewFormRow("idf.project.setting.upload.baud", uploadBaud, false);
        JLabel cmakeProfileLabel = i18nLabel("esp.idf.build.cmake.profile");
        cmakeProfileLabel.setToolTipText($i18n("esp.idf.build.cmake.profile.tooltip"));
        wrapper.addNewFormRow(cmakeProfileLabel, cmakeProfile, false);
        saveButton.setText($i18n("idf.project.setting.save"));
        saveButton.setEnabled(false);
        wrapper.addCol(saveButton, 1);

        add(wrapper, BorderLayout.WEST);

        initValues();

        bindAction();

    }

    private void initValues() {
        initSerialConf();
        initCmakeProfile();
    }

    private void initCmakeProfile() {
        idfProjectConfigService.addProfileChangeListener("EspIdfToolWindowSettingPanel::refreshProfileCheckBox",
                (profileChangeType -> {
                    if (profileChangeType == IdfProjectConfigService.ProfileChangeType.PROFILE_SELECT_CHANGE) {
                        return;
                    }
                    initCmakeProfileVal();
                }));
        initCmakeProfileVal();
    }

    private void initCmakeProfileVal() {
        ApplicationManager.getApplication().invokeLater(() ->
        {
            List<IdfProfileInfo> currentIdfProjectConfig = idfProjectConfigService.getIdfProfileInfoList();
            int itemCount = cmakeProfile.getItemCount();
            Object oldSelect = cmakeProfile.getSelectedItem();
            if (itemCount != currentIdfProjectConfig.size()) {
                cmakeProfile.removeAllItems();
            } else {
                for (int i = 0; i < itemCount; i++) {
                    if (!Objects.equals(cmakeProfile.getItemAt(i), currentIdfProjectConfig.get(i).getDisplayName())) {
                        cmakeProfile.removeAllItems();
                        break;
                    }
                }
            }

            if (cmakeProfile.getItemCount() > 0) {
                return;
            }
            for (IdfProfileInfo idfProfileInfo : currentIdfProjectConfig) {
                cmakeProfile.addItem(idfProfileInfo.getDisplayName());
            }

            if (oldSelect instanceof String oldSelectName && idfProjectConfigService.getIdfProfileInfo(oldSelectName) != null) {
                cmakeProfile.setSelectedItem(oldSelectName);
                idfProjectConfigService.updateProfile(oldSelectName);
            } else {
                IdfProfileInfo firstIdfProjectConfig = idfProjectConfigService.getFirstIdfProfileInfo();
                if (firstIdfProjectConfig != null) {
                    cmakeProfile.setSelectedItem(firstIdfProjectConfig.getDisplayName());
                    idfProjectConfigService.updateProfile(firstIdfProjectConfig.getDisplayName());
                }
            }

        });
    }

    private void initSerialConf() {
        // 初始化波特率
        String baudRates = $sys("idf.baud.rates");
        String[] baudRateArr = baudRates.trim().split(",");
        monitorBaud.setModel(new DefaultComboBoxModel<>(baudRateArr));
        uploadBaud.setModel(new DefaultComboBoxModel<>(baudRateArr));

        // 优先读取项目配置，但凡有一个值配置过，就不读取环境变量

        IdfProjectConfig projectConfig = idfProjectConfigService.getProjectConfig();
        if (projectConfig != null && !projectConfig.isEmpty()) {
            portField.setPortToEditor(projectConfig.getPort());
            monitorBaud.setSelectedItem(projectConfig.getMonitorBaud());
            uploadBaud.setSelectedItem(projectConfig.getUploadBaud());
            return;
        }
        ApplicationManager.getApplication().invokeLater(() ->
        {
            // 没有配置则读取环境变量
            IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);

            Map<String, String> environments = environmentService.getEnvironments();
            String port = environments.get(ESP_PORT);

            if (!StringUtil.isEmpty(port)) {
                portField.setPortToEditor(port);
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
        });

    }

    private void bindAction() {

        saveButton.addMouseListener(mouseClicked(e -> {
                    saveButton.setEnabled(false);
                    String port = portField.getPort();
                    if (!StringUtil.isEmpty(port)) {
                        projectConfigModel.setPort(port);
                    }
                    setBaudStrFormObj(monitorBaud.getSelectedItem(), projectConfigModel::setMonitorBaud);
                    setBaudStrFormObj(uploadBaud.getSelectedItem(), projectConfigModel::setUploadBaud);
                    idfProjectConfigService.updateProjectConfig(projectConfigModel);
                    EspIdfProjectUtil.switchProfileTarget(project, projectConfigModel.getCmakeProfile());

                })
        );

        portField.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Object selectedItem = portField.getSelectedItem();
                if (selectedItem instanceof SerialPortInfo serialPortInfo) {
                    projectConfigModel.setPort(serialPortInfo.getComPort());
                }
                saveButton.setEnabled(idfProjectConfigService.hasValueChange(projectConfigModel));
            }
        });
        portField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                projectConfigModel.setPort(portField.getEditorText());
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
        cmakeProfile.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                projectConfigModel.setCmakeProfile((String) cmakeProfile.getSelectedItem());
                saveButton.setEnabled(idfProjectConfigService.hasValueChange(projectConfigModel));
            }
        });
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
