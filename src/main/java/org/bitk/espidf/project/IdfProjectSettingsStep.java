package org.bitk.espidf.project;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.ui.components.JBPanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.bitk.espidf.project.component.ComboBoxWithRefresh;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.bitk.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfProjectSettingsStep<T> extends ProjectSettingsStepBase<T> {

    protected ComboBox<String> projectTargets;

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
