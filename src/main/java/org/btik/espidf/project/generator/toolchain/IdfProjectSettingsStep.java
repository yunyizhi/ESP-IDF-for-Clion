package org.btik.espidf.project.generator.toolchain;

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.ui.components.JBPanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.conf.LastChosenIdfToolchian;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.util.UIUtils;

import javax.swing.*;
import java.awt.*;

import static org.btik.espidf.service.IdfEnvironmentService.DEFAULT_IDF_TOOLS_PATH;
import static org.btik.espidf.ui.componets.SelectedItemListener.selectedListener;
import static org.btik.espidf.util.SysConf.$sys;
import static org.btik.espidf.util.UIUtils.createConstraints;
import static org.btik.espidf.util.UIUtils.i18nLabel;

public class IdfProjectSettingsStep<T> extends ProjectSettingsStepBase<T> {

    protected final IdfProjectGenerator<?> idfProjectGenerator;
    private ComboBox<String> idfTargets;
    private IdfToolchainCombBox idfToolchainCombBox;
    private final JLabel idfPath = new JLabel();
    private final JLabel idfToolsPath = new JLabel();


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

    private void initIdfToolChianBox() {
        idfToolchainCombBox = new IdfToolchainCombBox();
        UIUtils.setWidth(idfToolchainCombBox, 300);
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

    }


    @Override
    public JPanel createAdvancedSettings() {
        JBPanel<?> panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(5, 2);
        JPanel wrapper = new JPanel(gridLayoutManager);
        int rowIndex = 0;

        initIdfToolChianBox();
        wrapper.add(i18nLabel("idf.env.type.tool.chian"), createConstraints(rowIndex, 0));
        initIdfTargets();
        wrapper.add(idfToolchainCombBox, createConstraints(rowIndex, 1));
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
        IdfSysConfService service = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        LastChosenIdfToolchian lastChosenIdfToolchian = service.getLastChosenIdfToolchian();
        if (lastChosenIdfToolchian != null){
            idfToolchainCombBox.setSelectedToolchain(lastChosenIdfToolchian.getEnvFile());
        }
        return panel;
    }
}
