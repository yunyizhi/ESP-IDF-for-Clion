package org.btik.espidf.run.config.gdbinit;

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.run.config.EspIdfDebugRunConfig;
import org.btik.espidf.run.config.model.GdbInitDebugConfigModel;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.state.model.IdfProfileInfo;
import org.btik.espidf.ui.componets.MouseHooks;
import org.btik.espidf.ui.componets.TextFieldFileChooser;
import org.btik.espidf.util.UIUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Map;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.UIUtils.*;

/**
 * @author lustre
 * @since 2024/9/2 22:26
 */
public class EspIdfGdbInitDebugSettingEditor extends SettingsEditor<EspIdfDebugRunConfig<GdbInitDebugConfigModel>> {

    private final JPanel rootPanel;

    private final EnvironmentVariablesComponent envComponent;

    private final JTextField arguments = new JTextField();
    private final GdbSymbolsPathBox gdbSymbolsPathBox;
    private final TextFieldFileChooser gdb;
    private final JButton setDefault = new JButton();
    private final Project project;

    public EspIdfGdbInitDebugSettingEditor(@NotNull Project project) {
        this.project = project;
        rootPanel = new JPanel(new VerticalFlowLayout(0, 2));
        envComponent = new EnvironmentVariablesComponent();
        envComponent.getLabel().setVisible(false);
        JPanel wrapper = new JPanel(new GridLayoutManager(7, 2, JBUI.insetsTop(16), -1, -1));

        int rowIndex = 0;

        wrapper.add(i18nLabel("esp.idf.debug.openocd.arguments"), createConstraints(rowIndex, 0));
        GridConstraints openocdArgConstraints = createConstraints(rowIndex, 1);
        openocdArgConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        openocdArgConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        wrapper.add(arguments, openocdArgConstraints);
        rowIndex++;

        wrapper.add(i18nLabel("esp.idf.debug.gdb.init"), createConstraints(rowIndex, 0));
        GridConstraints appElfConstraints = createConstraints(rowIndex, 1);
        appElfConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        appElfConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        gdbSymbolsPathBox = new GdbSymbolsPathBox(project);
        UIUtils.setWidth(gdbSymbolsPathBox, 120);
        wrapper.add(gdbSymbolsPathBox, appElfConstraints);
        rowIndex++;

        wrapper.add(i18nLabel("esp.idf.debug.gdb"), createConstraints(rowIndex, 0));
        GridConstraints gdbArgConstraints = createConstraints(rowIndex, 1);
        gdbArgConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        gdbArgConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        gdb = new TextFieldFileChooser();
        gdb.addActionListener(project, newExtFileChooser("gdb" , $i18n("select.esp.gdb.path"), $i18n("select.esp.gdb.path")));
        wrapper.add(gdb, gdbArgConstraints);
        rowIndex++;

        wrapper.add(i18nLabel("esp.idf.debug.openocd.environment.variables"), createConstraints(rowIndex, 0));
        GridConstraints envConstraints = createConstraints(rowIndex, 1);
        envConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        envConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        wrapper.add(envComponent, envConstraints);
        rowIndex++;

        wrapper.add(new JLabel(""), createConstraints(rowIndex, 0));
        wrapper.add(setDefault, createConstraints(rowIndex, 1));
        setDefault.setText($i18n("esp.idf.debug.set.default"));

        rootPanel.add(wrapper, BorderLayout.CENTER);

        bindAction();
    }


    private void bindAction() {
        setDefault.addMouseListener(new MouseHooks().withClickedCB(e -> {
                    try {
                        setDefault.setEnabled(false);
                        initValue();
                    } finally {
                        setDefault.setEnabled(true);
                    }
                })
        );
        gdbSymbolsPathBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Object selectedItem = gdbSymbolsPathBox.getSelectedItem();
                if (selectedItem instanceof GdbInitProfileInfo gdbInitProfileInfo) {
                    setGdbByTarget(gdbInitProfileInfo.target());
                    gdbSymbolsPathBox.setCmakeBuildDir(gdbInitProfileInfo.buildDir(), false);
                }
            }
        });

    }

    private void setGdbByTarget(String target) {
        if (StringUtils.isEmpty(target)) {
            return;
        }
        IdfSysConfService service = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        String gdbExecutable = service.getGdbExecutable(target);
        gdb.setText(gdbExecutable);
        Map<String, String> environments = project.getService(IdfEnvironmentService.class).getEnvironments();
        String path = environments.get("Path");
        if (path == null) {
            path = environments.get("PATH");
        }
        if (path == null) {
            return;
        }
        File gdbFile = PathEnvironmentVariableUtil.findInPath(gdbExecutable, path, null);
        if (gdbFile != null) {
            gdb.setRootDir(gdbFile.getParentFile());
        }
    }

    private void initValue() {
        IdfProjectConfigService idfProjectConfigService = project.getService(IdfProjectConfigService.class);
        IdfProfileInfo selectedIdfProfileInfo = idfProjectConfigService.getSelectedIdfProfileInfo();
        String target = selectedIdfProfileInfo.getTarget();
        gdbSymbolsPathBox.setCmakeBuildDir(selectedIdfProfileInfo.getBuildDir(), true);
        setGdbByTarget(target);

    }

    @Override
    protected void resetEditorFrom(@NotNull EspIdfDebugRunConfig<GdbInitDebugConfigModel> debugRunConfig) {
        var configDataModel = debugRunConfig.getConfigDataModel();
        if (configDataModel == null) {
            initValue();
            return;
        }
        envComponent.setEnvData(configDataModel.getEnvData());
        arguments.setText(configDataModel.getOpenOcdArguments());
        gdb.setText(configDataModel.getGdbExe());
        gdbSymbolsPathBox.setEditorText(configDataModel.getPath());
    }

    @Override
    protected void applyEditorTo(@NotNull EspIdfDebugRunConfig<GdbInitDebugConfigModel> debugRunConfig) throws ConfigurationException {
        if (StringUtils.isEmpty(gdb.getText())) {
            throw new ConfigurationException($i18n("esp.idf.debugging.gdb.not.selected"));
        }
        if (StringUtils.isEmpty(gdbSymbolsPathBox.getEditorText())) {
            throw new ConfigurationException($i18n("esp.idf.debugging.symbols.not.selected"));
        }
        var configDataModel = debugRunConfig.getConfigDataModel();
        var debugConfigModel = configDataModel == null ? new GdbInitDebugConfigModel() : configDataModel;
        debugRunConfig.setConfigDataModel(debugConfigModel);
        debugConfigModel.setOpenOcdArguments(arguments.getText());
        debugConfigModel.setPath(gdbSymbolsPathBox.getEditorText());
        debugConfigModel.setBuildDir(gdbSymbolsPathBox.getBuildDir());
        debugConfigModel.setGdbExe(gdb.getText());
        debugConfigModel.setEnvData(envComponent.getEnvData());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return rootPanel;
    }

}
