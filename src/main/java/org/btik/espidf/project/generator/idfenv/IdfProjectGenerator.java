package org.btik.espidf.project.generator.idfenv;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CLionProjectGenerator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/8 20:37
 */
public class IdfProjectGenerator<T> extends CLionProjectGenerator<T> implements CustomStepProjectGenerator<T> {


    private final IdfProjectCreatorActions<T> creatorActions = new IdfProjectCreatorActions<>();

    @Override
    public @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription() {
        return $sys("project.type.description");
    }

    @Override
    public @NotNull @NlsContexts.Label String getName() {
        return $sys("project.type.name.legacy");
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
        return IconLoader.getIcon("/org-btik-esp-idf/image/idf16_16.svg", getClass());
    }

    @Override
    public @NotNull ValidationResult validate(@NotNull String baseDirPath) {
        ValidationResult superResult = super.validate(baseDirPath);
        if (!superResult.isOk()) {
            return superResult;
        }
        return creatorActions.validate();
    }


    public void setIdfToolsPath(String idfToolsPath) {
        creatorActions.setIdfToolsPath(idfToolsPath);
    }

    public void setIdfEnvConf(IdfEnvConf idfEnvConf) {
        creatorActions.setIdfEnvConf(idfEnvConf);
    }

    @Override
    public AbstractActionWithPanel createStep(DirectoryProjectGenerator<T> directoryProjectGenerator, AbstractNewProjectStep.AbstractCallback<T> abstractCallback) {

        return new IdfProjectSettingsStep<>(directoryProjectGenerator, abstractCallback);
    }

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull T settings, @NotNull Module module) {
        creatorActions.generateProject(project, baseDir, settings, module);
    }

    public void setIdfTarget(String idfTarget) {
        creatorActions.setIdfTarget(idfTarget);
    }
}
