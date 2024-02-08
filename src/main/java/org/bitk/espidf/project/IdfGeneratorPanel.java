package org.bitk.espidf.project;

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.util.PathMappingsComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jetbrains.cidr.cpp.CLionCMakeBundle;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CMakeProjectGenerator;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.settings.ui.CMakeSettingsPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static org.bitk.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfGeneratorPanel extends CMakeSettingsPanel {
    private PathMappingsComponent idfToolsPath;

    private VerticalLayout layout;


    public IdfGeneratorPanel(@NotNull CMakeProjectGenerator cMakeProjectGenerator) {
        super(cMakeProjectGenerator);
        createUIComponents();
        setLayout(layout);
        layout.addLayoutComponent("PATH", idfToolsPath);
    }

    private void createUIComponents() {
        idfToolsPath = new PathMappingsComponent();
        layout = new VerticalLayout();
    }

    @Override
    public void init(@NotNull CMakeProjectGenerator cMakeProjectGenerator) {
        if (!(cMakeProjectGenerator instanceof IdfProjectGenerator idfGenerator)) {
            super.init(cMakeProjectGenerator);
            return;
        }
        this.setLayout(new VerticalFlowLayout(0, 2));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(3, 2);
        JPanel wrapper = new JPanel(gridLayoutManager);
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        final TextFieldWithBrowseButton idfToolPathBrowser = new TextFieldWithBrowseButton();
        idfToolPathBrowser.addBrowseFolderListener($i18n("select.idf.tools.path"), $i18n("select.idf.tools.path.for.idf"), (Project)null, descriptor);
        idfToolPathBrowser.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            private void handleChange() {
                idfGenerator.setIdfToolsPath(idfToolPathBrowser.getText());
            }

            public void insertUpdate(DocumentEvent e) {
                this.handleChange();
            }

            public void removeUpdate(DocumentEvent e) {
                this.handleChange();
            }

            public void changedUpdate(DocumentEvent e) {
                this.handleChange();
            }
        });
        if (idfToolPathBrowser.getTextField() instanceof JBTextField) {
            ((JBTextField)idfToolPathBrowser.getTextField()).getEmptyText().setText(CLionCMakeBundle.message("cmake.settings.qt.cmake.prefix.path.optional", new Object[0]));
        }

        JLabel qtCMakePrefixLabel = new JLabel($i18n("idf.tools.path.title"));
        wrapper.add(qtCMakePrefixLabel, createConstraints(0, 0));
        GridConstraints firstRowConstraints = createConstraints(0, 1);
        firstRowConstraints.setFill(1);
        firstRowConstraints.setHSizePolicy(4);
        wrapper.add(idfToolPathBrowser, firstRowConstraints);
        this.add(wrapper, "West");
    }
}
