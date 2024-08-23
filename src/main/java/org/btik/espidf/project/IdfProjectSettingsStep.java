package org.btik.espidf.project;

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.openapi.ui.*;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.uiDesigner.core.GridConstraints;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ItemEvent;

import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfProjectSettingsStep<T> extends ProjectSettingsStepBase<T> {

    protected final IdfProjectGenerator<?> idfProjectGenerator;

    protected ComboBox<String> idfTargets;

    public IdfProjectSettingsStep(DirectoryProjectGenerator<T> projectGenerator, AbstractNewProjectStep.AbstractCallback<T> callback) {
        super(projectGenerator, callback);
        this.idfProjectGenerator = (IdfProjectGenerator<?>) projectGenerator;
    }
    public boolean isDumbAware() {
        return true;
    }

    protected static @NotNull GridConstraints createConstraints(int row, int column) {
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(row);
        constraints.setColumn(column);
        constraints.setAnchor(GridConstraints.ANCHOR_WEST);
        return constraints;
    }

    protected void initIdfTargets() {
        idfTargets = new ComboBox<>();
        String lastTargetStr = $sys("idf.targets.last");
        String[] targetArr = lastTargetStr.trim().split(",");
        for (String target : targetArr) {
            idfTargets.addItem(target);
        }
        idfTargets.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            idfProjectGenerator.setIdfTarget(String.valueOf(e.getItem()));
        });
    }
}
