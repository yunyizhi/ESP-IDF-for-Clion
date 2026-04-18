package org.btik.espidf.project.generator.toolchain;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.ui.componets.GridPanel;
import org.btik.espidf.ui.componets.TextFieldFileChooser;
import org.btik.espidf.util.UIUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.UIUtils.*;

public class CustomScriptDialog extends DialogWrapper {
    private TextFieldFileChooser scriptBrowserButton;
    private JBTextField toolChainName;

    public CustomScriptDialog(Component parent, @NotNull String title) {
        super(parent, true);
        this.setTitle(title);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridPanel gridPanel = new GridPanel(2, 2);
        gridPanel.add(i18nLabel("idf.custom.script.path"));
        scriptBrowserButton = new TextFieldFileChooser();
        scriptBrowserButton.addActionListener(null, FileChooserDescriptorFactory.singleFile().withTitle($i18n("idf.select.script.file")));
        gridPanel.addGrow(scriptBrowserButton);
        gridPanel.newRow();
        gridPanel.add(i18nLabel("idf.toolchain.name"));
        toolChainName = new JBTextField();
        gridPanel.addGrow(toolChainName);
        panel.add(gridPanel, BorderLayout.WEST);
        UIUtils.setWidth(panel, 300);
        return panel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (StringUtils.isEmpty(getScriptPath())) {
            return new ValidationInfo($i18n("idf.custom.script.path.empty"), scriptBrowserButton);
        }
        return super.doValidate();
    }

    public String getScriptPath() {
        return scriptBrowserButton != null ? scriptBrowserButton.getText() : null;
    }

    public String getToolChainName() {
        return toolChainName != null ? toolChainName.getText() : null;
    }
}
