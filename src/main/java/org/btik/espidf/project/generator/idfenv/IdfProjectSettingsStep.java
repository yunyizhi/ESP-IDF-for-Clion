package org.btik.espidf.project.generator.idfenv;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.ui.components.JBPanel;
import org.btik.espidf.conf.LastChosenIdfEnv;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.ui.componets.GridPanel;
import org.btik.espidf.util.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.service.IdfEnvironmentService.DEFAULT_IDF_TOOLS_PATH;
import static org.btik.espidf.ui.componets.DocumentChangeListener.bindDocChange;
import static org.btik.espidf.ui.componets.SelectedItemListener.selectedListener;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/8 21:47
 */
public class IdfProjectSettingsStep<T> extends ProjectSettingsStepBase<T> {

    protected final IdfProjectGenerator<?> idfProjectGenerator;

    protected IdfEnvComboBox idfs;

    protected ComboBox<String> idfTargets;

    private static final String IDF_ENV_JSON = "idf-env.json";


    private JBPanel<?> panel;
    private String idfToolsPath;
    private TextFieldWithBrowseButton idfToolsPathBrowserButton;

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
        idfTargets.addItemListener(
                selectedListener(
                        e -> idfProjectGenerator.setIdfTarget(String.valueOf(e.getItem()))
                )
        );
    }

    protected void initIdfs() {
        idfs = new IdfEnvComboBox();
        UIUtils.setWidth(idfs, 300);
        idfs.addItemListener(selectedListener(e -> {
            IdfEnvConf item = (IdfEnvConf) e.getItem();
            idfTargets.removeAllItems();
            for (String target : item.getTargets()) {
                idfTargets.addItem(target);
            }
            idfProjectGenerator.setIdfEnvConf(item);
        }));
    }


    @Override
    public JPanel createAdvancedSettings() {
        panel = new JBPanel<>(new VerticalFlowLayout(0, 2));
        GridPanel gridPanel = new GridPanel(3, 2);
        initIdfPathBrowser();
        gridPanel.addNewFormRow("idf.tools.path.title", idfToolsPathBrowserButton, true);
        initIdfs();
        gridPanel.addNewFormRow("idf.framework", idfs, true);
        initIdfTargets();
        gridPanel.addNewFormRow("idf.env.type.target", idfTargets, false);
        panel.add(gridPanel, BorderLayout.WEST);
        IdfSysConfService service = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        LastChosenIdfEnv lastChosenIdfEnv = service.getLastChosenIdfEnv();
        if (lastChosenIdfEnv != null) {
            idfToolsPathBrowserButton.getTextField().setText(lastChosenIdfEnv.getIdfToolsPath());
        } else {
            idfToolsPathBrowserButton.getTextField().setText(DEFAULT_IDF_TOOLS_PATH);
        }
        refreshIdfs();
        if (lastChosenIdfEnv != null) {
            selectIdf(lastChosenIdfEnv.getIdfPath());
        }
        return panel;
    }

    private void selectIdf(String idfPath) {
        for (int i = 0; i < idfs.getItemCount(); i++) {
            IdfEnvConf item = idfs.getItemAt(i);
            if (Objects.equals(idfPath, item.getPath())) {
                idfs.setSelectedIndex(i);
                break;
            }
        }
    }

    private void initIdfPathBrowser() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.singleFile().
                withTitle($i18n("select.idf.path")).withDescription($i18n("select.idf.path.for.idf"));
        idfToolsPathBrowserButton = new TextFieldWithBrowseButton();
        bindDocChange(idfToolsPathBrowserButton, e -> {
            idfToolsPath = idfToolsPathBrowserButton.getText();
            idfProjectGenerator.setIdfToolsPath(idfToolsPath);
            checkValid();
        });
        idfToolsPathBrowserButton.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>(
                idfToolsPathBrowserButton, null, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                refreshIdfs();
            }
        });
    }

    private void refreshIdfs() {
        idfs.removeAllItems();
        if (StringUtil.isEmpty(idfToolsPath)) {
            checkValid();
            return;
        }
        Path path = Path.of(idfToolsPath, IDF_ENV_JSON);
        if (!Files.exists(path)) {
            return;
        }
        Gson gson = new Gson();
        List<IdfEnvConf> idfEnvConfList = new ArrayList<>();
        try {
            String json = Files.readString(path);
            JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
            JsonObject rootObject = jsonElement.getAsJsonObject();
            JsonElement idfInstalled = rootObject.get("idfInstalled");
            for (Map.Entry<String, JsonElement> idfEnvs : idfInstalled.getAsJsonObject().entrySet()) {
                IdfEnvConf idfEnvConf = gson.fromJson(idfEnvs.getValue(), IdfEnvConf.class);
                idfEnvConfList.add(idfEnvConf);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (IdfEnvConf idfEnvConf : idfEnvConfList) {
            idfs.addItem(idfEnvConf);
        }

    }
}
