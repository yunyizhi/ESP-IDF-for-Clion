package org.btik.espidf.project.generator.toolchain;

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.notification.NotificationType;
import com.intellij.icons.AllIcons;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.intellij.ui.AncestorListenerAdapter;
import com.intellij.ui.components.JBPanel;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.conf.LastChosenIdfToolchain;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.service.IdfToolchainCacheService;
import org.btik.espidf.ui.componets.GridPanel;
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
import static org.btik.espidf.util.I18nMessage.NOTIFICATION_GROUP;
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
        GridLayoutManager gridLayoutManager = new GridLayoutManager(1, 3);
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
                idfToolsPathValue = $i18n("idf.tools.path.not.set.default") + ' ' + DEFAULT_IDF_TOOLS_PATH;
            }
            idfToolsPath.setText(idfToolsPathValue);
            idfProjectGenerator.setIdfToolChian(idfToolchain);
        }));

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new OpenEimDialogAction(toolchainPanel));
        actionGroup.add(new OpenCustomScriptDialogAction(toolchainPanel));
        JButton newToolchainButton = new JButton($i18n("idf.common.new"));
        newToolchainButton.setToolTipText($i18n("idf.toolchain.new"));
        JButton rebuildCacheButton = new JButton();
        rebuildCacheButton.setIcon(AllIcons.Actions.Rebuild);
        rebuildCacheButton.setToolTipText($i18n("idf.toolchain.cache.rebuild.tip"));
        toolchainPanel.add(idfToolchainCombBox, createHCrowConstraints(0, 0));
        toolchainPanel.add(newToolchainButton, createConstraints(0, 1));
        toolchainPanel.add(rebuildCacheButton, createConstraints(0, 2));
        newToolchainButton.addMouseListener(mouseClicked(
                e -> showPop(newToolchainButton, "", actionGroup)));
        rebuildCacheButton.addActionListener(e -> rebuildToolchainCache(rebuildCacheButton));
        return toolchainPanel;
    }


    @Override
    public JPanel createAdvancedSettings() {
        JBPanel<?> panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridPanel gridPanel = new GridPanel(5, 2);

        JPanel toolchainSelectorPanel = createToolchainSelectorPanel();
        gridPanel.addNewFormRow("idf.env.type.tool.chian", toolchainSelectorPanel, true);
        initIdfTargets();
        gridPanel.addNewFormRow("idf.env.type.target", idfTargets, false);
        gridPanel.addNewFormRow("idf.path", idfPath, false);
        gridPanel.addNewFormRow("idf.tools.path", idfToolsPath, false);
        gridPanel.add(i18nLabel("idf.env.type.target.tip"), 2);

        panel.add(gridPanel, BorderLayout.WEST);
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

    private void rebuildToolchainCache(Component owner) {
        int result = Messages.showYesNoDialog(
                owner,
                $i18n("idf.toolchain.cache.rebuild.confirm.msg"),
                $i18n("idf.toolchain.cache.rebuild.confirm.title"),
                Messages.getQuestionIcon());
        if (result != Messages.YES) {
            return;
        }
        IdfToolchainCacheService cacheService = ApplicationManager.getApplication().getService(IdfToolchainCacheService.class);
        // 仅重建插件级工具链缓存，不修改任何已打开项目的环境变量
        cacheService.rebuildAll(ModalTaskOwner.component(owner));
        // 重建后刷新下拉框展示（重新从缓存加载版本与路径）
        idfToolchainCombBox.reload();
        NOTIFICATION_GROUP.createNotification(
                $i18n("idf.toolchain.cache.rebuild.ok"),
                $i18n("idf.toolchain.cache.rebuild.ok.msg"),
                NotificationType.INFORMATION).notify(null);
    }

    class OpenEimDialogAction extends AnAction {
        private final Component dialogParent;

        public OpenEimDialogAction(Component dialogParent) {
            super($i18n("idf.env.type.tool.chian.eim"));
            this.dialogParent = dialogParent;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            EimDialog eimDialog = new EimDialog(dialogParent, $i18n("idf.create.toolchain.by.eim"));
            eimDialog.show();
            if (eimDialog.getExitCode() != EimDialog.OK_EXIT_CODE) {
                return;
            }
            String scriptPath = eimDialog.getScriptPath();
            String toolChainName = eimDialog.getToolChainName();
            CPPToolchains.Toolchain toolchain = ToolChainTool.newEnvToolChain(scriptPath, toolChainName);
            idfToolchainCombBox.selectToolchain(toolchain);
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
            CustomScriptDialog customScript = new CustomScriptDialog(dialogParent, $i18n("idf.create.toolchain.by.custom.script"));
            customScript.show();
            if (customScript.getExitCode() != CustomScriptDialog.OK_EXIT_CODE) {
                return;
            }
            String scriptPath = customScript.getScriptPath();
            String toolChainName = customScript.getToolChainName();
            CPPToolchains.Toolchain toolchain = ToolChainTool.newEnvToolChain(scriptPath, toolChainName);
            idfToolchainCombBox.selectToolchain(toolchain);
        }
    }
}
