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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.btik.espidf.util.I18nMessage.$i18n;

public class CustomScriptDialog extends DialogWrapper {
    private final TextFieldFileChooser scriptBrowserButton = new TextFieldFileChooser();
    private final JBTextField toolChainName = new JBTextField();

    public CustomScriptDialog(Component parent, @NotNull String title) {
        super(parent, true);
        this.setTitle(title);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridPanel gridPanel = new GridPanel(2, 2);
        scriptBrowserButton.addActionListener(null, FileChooserDescriptorFactory.singleFile().withTitle($i18n("idf.select.script.file")));
        gridPanel.addNewFormRow("idf.custom.script.path", scriptBrowserButton, true);
        gridPanel.addNewFormRow("idf.toolchain.name", toolChainName, true);
        panel.add(gridPanel, BorderLayout.WEST);
        UIUtils.setWidth(gridPanel, 500);
        return panel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String scriptPath = getScriptPath();
        if (StringUtils.isEmpty(scriptPath)) {
            return new ValidationInfo($i18n("idf.custom.script.path.empty"), scriptBrowserButton);
        }
        Path folder = Path.of(scriptPath);
        if (!Files.exists(folder)) {
            return new ValidationInfo($i18n("idf.path.not.exist"), scriptBrowserButton);
        }
        return super.doValidate();
    }

    public String getScriptPath() {
        return scriptBrowserButton.getText();
    }

    public String getToolChainName() {
        return toolChainName.getText();
    }
}
