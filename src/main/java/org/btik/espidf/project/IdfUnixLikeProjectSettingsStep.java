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
import org.btik.espidf.service.IdfSysConfService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.UIUtils.createConstraints;
import static org.btik.espidf.util.UIUtils.i18nLabel;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfUnixLikeProjectSettingsStep<T> extends IdfProjectSettingsStep<T> {

    private JBPanel<?> panel;
    private String idfFrameworkPath;
    private TextFieldWithBrowseButton idfFrameworkPathBrowserButton;

    public IdfUnixLikeProjectSettingsStep(DirectoryProjectGenerator<T> projectGenerator, AbstractNewProjectStep.AbstractCallback<T> callback) {
        super(projectGenerator, callback);
    }

    @Override
    public JPanel createAdvancedSettings() {
        panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(3, 2);
        JPanel wrapper = new JPanel(gridLayoutManager);
        int rowIndex = 0;

        wrapper.add(i18nLabel("idf.path.title"), createConstraints(rowIndex, 0));
        initIdfPathBrowser();
        GridConstraints firstRowConstraints = createConstraints(rowIndex, 1);
        firstRowConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        firstRowConstraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        wrapper.add(idfFrameworkPathBrowserButton, firstRowConstraints);
        rowIndex++;

        wrapper.add(i18nLabel("idf.env.type.target"), createConstraints(rowIndex, 0));
        initIdfTargets();
        wrapper.add(idfTargets, createConstraints(rowIndex, 1));
        rowIndex++;

        GridConstraints targetTipCell = createConstraints(rowIndex, 0);
        targetTipCell.setColSpan(2);
        wrapper.add(i18nLabel("idf.env.type.target.tip"), targetTipCell);

        panel.add(wrapper, BorderLayout.WEST);
        IdfSysConfService service = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        IdfToolConf idfToolConf = service.getLastActivedIdfToolConf();
        if (idfToolConf != null) {
            idfFrameworkPathBrowserButton.getTextField().setText(idfToolConf.getIdfToolPath());
        }
        return panel;
    }

    private void initIdfPathBrowser() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        idfFrameworkPathBrowserButton = new TextFieldWithBrowseButton();
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
                $i18n("select.idf.path"), $i18n("select.idf.path.for.idf"),
                idfFrameworkPathBrowserButton, null, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
    }
}
