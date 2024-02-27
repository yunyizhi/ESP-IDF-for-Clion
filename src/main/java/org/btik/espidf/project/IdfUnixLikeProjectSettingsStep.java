package org.btik.espidf.project;

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.*;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.ui.components.JBPanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.project.component.ComboBoxWithRefresh;
import org.btik.espidf.service.IdfToolConfService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfUnixLikeProjectSettingsStep<T> extends IdfProjectSettingsStep<T> {

    private JBPanel<?> panel;

    protected ComboBox<String> projectTargets;
    private String idfFrameworkPath;


    public IdfUnixLikeProjectSettingsStep(DirectoryProjectGenerator<T> projectGenerator, AbstractNewProjectStep.AbstractCallback<T> callback) {
        super(projectGenerator, callback);
    }

    @Override
    public JPanel createAdvancedSettings() {
        panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(2, 2);
        JPanel wrapper = new JPanel(gridLayoutManager);
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        final TextFieldWithBrowseButton idfFrameworkPathBrowserButton = new TextFieldWithBrowseButton();
        idfFrameworkPathBrowserButton.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            private void handleChange() {
                idfFrameworkPath = idfFrameworkPathBrowserButton.getText();
                idfProjectGenerator.setIdfFrameworkPath(idfFrameworkPath);
                checkValid();
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
        idfFrameworkPathBrowserButton.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>(
                $i18n("select.idf.tools.path"), $i18n("select.idf.tools.path.for.idf"),
                idfFrameworkPathBrowserButton, null, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
            }
        });

        JLabel idfFrameworkPathLabel = new JLabel($i18n("idf.tools.path.title"));
        wrapper.add(idfFrameworkPathLabel, createConstraints(0, 0));
        GridConstraints firstRowConstraints = createConstraints(0, 1);
        firstRowConstraints.setFill(1);
        firstRowConstraints.setHSizePolicy(4);
        wrapper.add(idfFrameworkPathBrowserButton, firstRowConstraints);
        JLabel projectTargetLabel = new JLabel($i18n("project.target"));
        projectTargets = new ComboBox<>();
        projectTargets.addItem("esp32");
        wrapper.add(projectTargetLabel, createConstraints(1, 0));
        wrapper.add(new ComboBoxWithRefresh<>(projectTargets, this::refreshProjectTarget),
                createConstraints(1, 1));
        panel.add(wrapper, "West");
        IdfToolConfService service = ApplicationManager.getApplication().getService(IdfToolConfService.class);
        IdfToolConf idfToolConf = service.getLastActivedIdfToolConf();
        if (idfToolConf != null) {
            idfFrameworkPathBrowserButton.getTextField().setText(idfToolConf.getIdfToolPath());
        }
        return panel;
    }

    private void refreshProjectTarget(){

    }

}
