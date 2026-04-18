package org.btik.espidf.project.generator.toolchain;

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.ui.AncestorListenerAdapter;
import com.intellij.ui.components.JBPanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.conf.LastChosenIdfToolchain;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.util.ToolChainTool;
import org.btik.espidf.util.UIUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import java.awt.*;

import static org.btik.espidf.service.IdfEnvironmentService.DEFAULT_IDF_TOOLS_PATH;
import static org.btik.espidf.ui.componets.MouseHooks.mouseClicked;
import static org.btik.espidf.ui.componets.SelectedItemListener.selectedListener;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;
import static org.btik.espidf.util.UIUtils.*;

public class IdfProjectSettingsStep<T> extends ProjectSettingsStepBase<T> {

    protected final IdfProjectGenerator<?> idfProjectGenerator;
    private ComboBox<String> idfTargets;
    private IdfToolchainCombBox idfToolchainCombBox;
    private final JLabel idfPath = new JLabel();
    private final JLabel idfToolsPath = new JLabel();
    private boolean toolchainLoaded = false;


    public IdfProjectSettingsStep(DirectoryProjectGenerator<T> projectGenerator, AbstractNewProjectStep.AbstractCallback<T> callback) {
        super(projectGenerator, callback);
        this.idfProjectGenerator = (IdfProjectGenerator<?>) projectGenerator;
    }

    public boolean isDumbAware() {
        return true;
    }

    protected void initIdfTargets() {
        idfTargets = new ComboBox<>();
        String lastTargetStr = $sys("idf.targets.last");
        String[] targetArr = lastTargetStr.trim().split(",");
        for (String target : targetArr) {
            idfTargets.addItem(target);
        }
        idfTargets.addItemListener(selectedListener(
                e -> idfProjectGenerator.setIdfTarget(String.valueOf(e.getItem()))
        ));
    }

    private JPanel createToolchainSelectorPanel() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(1, 2);
        JPanel toolchainPanel = new JPanel(gridLayoutManager);
        idfToolchainCombBox = new IdfToolchainCombBox();
        UIUtils.setWidth(idfToolchainCombBox, 250);
        idfToolchainCombBox.addItemListener(selectedListener(e -> {
            Object selectedItem = e.getItem();
            if (!(selectedItem instanceof IdfToolchain idfToolchain)) {
                return;
            }
            idfPath.setText(idfToolchain.getIdfPath());
            String idfToolsPathValue = idfToolchain.getIdfToolsPath();
            if (StringUtils.isEmpty(idfToolsPathValue)) {
                idfToolsPathValue = "not set default as:" + DEFAULT_IDF_TOOLS_PATH;
            }
            idfToolsPath.setText(idfToolsPathValue);
            idfProjectGenerator.setIdfToolChian(idfToolchain);
        }));

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new OpenEimDialogAction());
        actionGroup.add(new OpenCustomScriptDialogAction(toolchainPanel));
        JButton newToolchainButton = new JButton($i18n("idf.common.new"));
        newToolchainButton.setToolTipText($i18n("idf.toolchain.new"));
        toolchainPanel.add(idfToolchainCombBox, createHCrowConstraints(0, 0));
        toolchainPanel.add(newToolchainButton, createConstraints(0, 1));
        newToolchainButton.addMouseListener(mouseClicked(
                e -> showPop(newToolchainButton, "", actionGroup)));
        return toolchainPanel;
    }


    @Override
    public JPanel createAdvancedSettings() {
        JBPanel<?> panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(5, 2);
        JPanel wrapper = new JPanel(gridLayoutManager);
        int rowIndex = 0;

        JPanel toolchainSelectorPanel = createToolchainSelectorPanel();
        wrapper.add(i18nLabel("idf.env.type.tool.chian"), createConstraints(rowIndex, 0));
        initIdfTargets();
        wrapper.add(toolchainSelectorPanel, createHCrowConstraints(rowIndex, 1));
        rowIndex++;
        wrapper.add(i18nLabel("idf.env.type.target"), createConstraints(rowIndex, 0));
        initIdfTargets();
        wrapper.add(idfTargets, createConstraints(rowIndex, 1));
        rowIndex++;
        wrapper.add(i18nLabel("idf.path"), createConstraints(rowIndex, 0));
        wrapper.add(idfPath, createConstraints(rowIndex, 1));
        rowIndex++;
        wrapper.add(i18nLabel("idf.tools.path"), createConstraints(rowIndex, 0));
        wrapper.add(idfToolsPath, createConstraints(rowIndex, 1));
        rowIndex++;
        GridConstraints targetTipCell = createConstraints(rowIndex, 0);
        targetTipCell.setColSpan(2);
        wrapper.add(i18nLabel("idf.env.type.target.tip"), targetTipCell);

        panel.add(wrapper, BorderLayout.WEST);
        panel.addAncestorListener(
                new AncestorListenerAdapter() {
                    @Override
                    public void ancestorAdded(AncestorEvent event) {
                        if (!toolchainLoaded) {
                            loadToolchain();
                            toolchainLoaded = true;
                        }
                    }
                }
        );
        return panel;
    }

    private void loadToolchain() {
        IdfSysConfService service = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        LastChosenIdfToolchain lastChosenIdfToolchain = service.getLastChosenIdfToolchian();
        if (lastChosenIdfToolchain != null) {
            idfToolchainCombBox.setSelectedToolchain(lastChosenIdfToolchain.getEnvFile());
        } else {
            idfToolchainCombBox.load();
        }
    }

    static class OpenEimDialogAction extends AnAction {
        public OpenEimDialogAction() {
            super($i18n("idf.env.type.tool.chian.eim"));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

        }
    }

    class OpenCustomScriptDialogAction extends AnAction {
        private final Component dialogParent;

        public OpenCustomScriptDialogAction(JPanel toolchainPanel) {
            super($i18n("idf.env.type.tool.chian.custom.script"));
            this.dialogParent = toolchainPanel;

        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            CustomScriptDialog customScript = new CustomScriptDialog(dialogParent, "Custom Script");
            customScript.show();
            String scriptPath = customScript.getScriptPath();
            String toolChainName = customScript.getToolChainName();
            CPPToolchains.Toolchain toolchain = ToolChainTool.newEnvToolChain(scriptPath, toolChainName);
            idfToolchainCombBox.selectToolchain(toolchain);
        }
    }
}
