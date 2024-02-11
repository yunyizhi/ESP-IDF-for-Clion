package org.bitk.espidf.project;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CLionProjectGenerator;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CMakeProjectGenerator;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.settings.ui.CMakeSettingsPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

import static org.bitk.espidf.util.I18nMessage.$i18n;
import static org.bitk.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/8 20:37
 */
public class IdfProjectGenerator extends CLionProjectGenerator<String> implements CustomStepProjectGenerator<String> {
    private String idfToolPath;

    private String idfId;

    @Override
    public @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription() {
        return $sys("project.type.description");
    }

    @Override
    public @NotNull @NlsContexts.Label String getName() {
        return $sys("project.type.name");
    }

    public @NotNull String getGroupName() {
        return "Embedded";
    }

    public int getGroupOrder() {
        return GroupOrders.EMBEDDED.order;
    }

    public @Nls @NotNull String getGroupDisplayName() {
        return "Embedded";
    }

    @Override
    public @Nullable Icon getLogo() {
        return IconLoader.getIcon("/org-btik-esp-idf/idf16_16.svg", getClass());
    }

    @Override
    public @NotNull ValidationResult validate(@NotNull String baseDirPath) {
        ValidationResult superResult = super.validate(baseDirPath);
        if(!superResult.isOk()) {
            return superResult;
        }
        if(StringUtil.isEmpty(idfToolPath)){
            return new ValidationResult($i18n("please.select.idf.tools.path.for.idf"));
        }
        if(StringUtil.isEmpty(idfId)) {
            return new ValidationResult($i18n("please.select.idf.id"));
        }
        return ValidationResult.OK;
    }

    public void setIdfToolsPath(String text) {
        idfToolPath = text;
    }


    public void setIdfId(String idfId) {
        this.idfId = idfId;
    }

    @Override
    public AbstractActionWithPanel createStep(DirectoryProjectGenerator<String> directoryProjectGenerator, AbstractNewProjectStep.AbstractCallback<String> abstractCallback) {
        return new IdfProjectSettingsStep(directoryProjectGenerator, abstractCallback);
    }
}
