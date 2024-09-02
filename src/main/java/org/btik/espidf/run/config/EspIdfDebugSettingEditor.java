package org.btik.espidf.run.config;

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.UIUtils.createConstraints;
import static org.btik.espidf.util.UIUtils.i18nLabel;

/**
 * @author lustre
 * @since 2024/9/2 22:26
 */
public class EspIdfDebugSettingEditor extends SettingsEditor<EspIdfRunConfig> {

    private final JPanel rootPanel;

    private final EnvironmentVariablesComponent envComponent;

    private final JTextField arguments;

    public EspIdfDebugSettingEditor() {
        rootPanel = new JPanel(new VerticalFlowLayout(0, 2));
        arguments = new JTextField();
        envComponent = new EnvironmentVariablesComponent();
        envComponent.getLabel().setVisible(false);
        JPanel wrapper = new JPanel(new GridLayoutManager(3, 2, JBUI.insetsTop(16), -1, -1));

        int rowIndex = 0;

        wrapper.add(i18nLabel("esp.idf.debug.openocd.arguments"), createConstraints(rowIndex, 0));
        GridConstraints firstRowConstraints = createConstraints(rowIndex, 1);
        firstRowConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        firstRowConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        wrapper.add(arguments, firstRowConstraints);
        rowIndex++;

        wrapper.add(new JLabel($i18n("esp.idf.debug.openocd.environment.variables")), createConstraints(rowIndex, 0));
        GridConstraints envConstraints = createConstraints(rowIndex, 1);
        envConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        envConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        wrapper.add(envComponent, envConstraints);

        rootPanel.add(wrapper, BorderLayout.CENTER);
    }

    @Override
    protected void resetEditorFrom(@NotNull EspIdfRunConfig espIdfRunConfig) {
        envComponent.setEnvData(espIdfRunConfig.getEnvData());
        arguments.setText(espIdfRunConfig.getOpenOcdArguments());
    }

    @Override
    protected void applyEditorTo(@NotNull EspIdfRunConfig espIdfRunConfig) throws ConfigurationException {
        espIdfRunConfig.setEnvData(envComponent.getEnvData());
        espIdfRunConfig.setOpenOcdArguments(arguments.getText());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return rootPanel;
    }
}
