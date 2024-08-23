package org.btik.espidf.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;

import org.btik.espidf.service.IdfEnvironmentService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;

public class EspIdfToolWindowSettingPanel extends JPanel {

    private static final String ESP_PORT = "ESPPORT";
    private static final String IDF_MONITOR_BAUD = "IDF_MONITOR_BAUD";
    private static final String MONITOR_BAUD = "MONITORBAUD";
    private static final String ESP_BAUD = "ESPBAUD";
    private final Project project;

    private final JBTextField editorTextField = new JBTextField();
    private final ComboBox<String> monitorBaud = new ComboBox<>();
    private final ComboBox<String> uploadBaud = new ComboBox<>();

    public EspIdfToolWindowSettingPanel(Project project) {
        super(new VerticalFlowLayout(0, 2));
        this.project = project;
        initUI();
    }

    private void initUI() {
        JPanel wrapper = new JPanel(new GridLayoutManager(3, 2, JBUI.insets(16, 16, 0, 16), -1, -1));

        int rowIndex = 0;

        wrapper.add(new JLabel($i18n("idf.project.setting.port")), createConstraints(rowIndex, 0));
        GridConstraints firstRowConstraints = createConstraints(rowIndex, 1);
        firstRowConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        firstRowConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        wrapper.add(editorTextField, firstRowConstraints);
        rowIndex++;

        wrapper.add(new JLabel($i18n("idf.project.setting.monitor.baud")), createConstraints(rowIndex, 0));
        wrapper.add(monitorBaud, createConstraints(rowIndex, 1));
        rowIndex++;

        wrapper.add(new JLabel($i18n("idf.project.setting.upload.baud")), createConstraints(rowIndex, 0));
        wrapper.add(uploadBaud, createConstraints(rowIndex, 1));

        String baudRates = $sys("idf.baud.rates");
        String[] baudRateArr = baudRates.trim().split(",");
        monitorBaud.setModel(new DefaultComboBoxModel<>(baudRateArr));
        uploadBaud.setModel(new DefaultComboBoxModel<>(baudRateArr));
        add(wrapper, BorderLayout.WEST);

        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        Map<String, String> environments = environmentService.getEnvironments();
        String port = environments.get(ESP_PORT);
        if (!StringUtil.isEmpty(port)) {
            editorTextField.setText(port);
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

    private static @NotNull GridConstraints createConstraints(int row, int column) {
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(row);
        constraints.setColumn(column);
        constraints.setAnchor(GridConstraints.ANCHOR_WEST);
        return constraints;
    }
}
