package org.btik.espidf.project;

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.openapi.ui.*;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.uiDesigner.core.GridConstraints;
import org.jetbrains.annotations.NotNull;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfProjectSettingsStep<T> extends ProjectSettingsStepBase<T> {

    protected final IdfProjectGenerator<?> idfProjectGenerator;

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
        constraints.setAnchor(8);
        return constraints;
    }
}
