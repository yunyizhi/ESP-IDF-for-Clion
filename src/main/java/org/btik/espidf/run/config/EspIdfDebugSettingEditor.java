package org.btik.espidf.run.config;

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.ui.MacroComboBoxWithBrowseButton;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.btik.espidf.run.config.model.DebugConfigModel;
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

    private final JTextField arguments = new JTextField();
    private final MacroComboBoxWithBrowseButton appElf ;
    private final MacroComboBoxWithBrowseButton bootloaderElf ;
    private final MacroComboBoxWithBrowseButton romElf ;
    private final MacroComboBoxWithBrowseButton gdb ;

    public EspIdfDebugSettingEditor(@NotNull Project project) {

        rootPanel = new JPanel(new VerticalFlowLayout(0, 2));
        envComponent = new EnvironmentVariablesComponent();
        envComponent.getLabel().setVisible(false);
        JPanel wrapper = new JPanel(new GridLayoutManager(6, 2, JBUI.insetsTop(16), -1, -1));

        int rowIndex = 0;

        wrapper.add(i18nLabel("esp.idf.debug.openocd.arguments"), createConstraints(rowIndex, 0));
        GridConstraints openocdArgConstraints = createConstraints(rowIndex, 1);
        openocdArgConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        openocdArgConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        wrapper.add(arguments, openocdArgConstraints);
        rowIndex++;

        wrapper.add(i18nLabel("esp.idf.debug.app_elf"), createConstraints(rowIndex, 0));
        GridConstraints appElfConstraints = createConstraints(rowIndex, 1);
        appElfConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        appElfConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        FileChooserDescriptor appElfChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        appElf = new MacroComboBoxWithBrowseButton(appElfChooser, project);
        wrapper.add(appElf, appElfConstraints);
        rowIndex++;


        wrapper.add(i18nLabel("esp.idf.debug.bootloader_elf"), createConstraints(rowIndex, 0));
        GridConstraints bootLoaderConstraints = createConstraints(rowIndex, 1);
        bootLoaderConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        bootLoaderConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        FileChooserDescriptor bootloaderElfChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        bootloaderElf = new MacroComboBoxWithBrowseButton(bootloaderElfChooser, project);
        wrapper.add(bootloaderElf, bootLoaderConstraints);
        rowIndex++;

        wrapper.add(i18nLabel("esp.idf.debug.rom_elf"), createConstraints(rowIndex, 0));
        GridConstraints romElfConstraints = createConstraints(rowIndex, 1);
        romElfConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        romElfConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        FileChooserDescriptor romElfChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        romElf = new MacroComboBoxWithBrowseButton(romElfChooser, project);
        wrapper.add(romElf, romElfConstraints);
        rowIndex++;

        wrapper.add(i18nLabel("esp.idf.debug.gdb"), createConstraints(rowIndex, 0));
        GridConstraints gdbArgConstraints = createConstraints(rowIndex, 1);
        gdbArgConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        gdbArgConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        FileChooserDescriptor gdbChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        gdb = new MacroComboBoxWithBrowseButton(gdbChooser, project);
        wrapper.add(gdb, gdbArgConstraints);
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
        DebugConfigModel configDataModel = espIdfRunConfig.getConfigDataModel();
        envComponent.setEnvData(configDataModel.getEnvData());
        arguments.setText(configDataModel.getOpenOcdArguments());
    }

    @Override
    protected void applyEditorTo(@NotNull EspIdfRunConfig espIdfRunConfig) throws ConfigurationException {
        DebugConfigModel debugConfigModel = new DebugConfigModel();

        espIdfRunConfig.setConfigDataModel(debugConfigModel);
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return rootPanel;
    }
}
