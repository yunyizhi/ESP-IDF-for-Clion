package org.bitk.espidf.project;

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


import static org.bitk.espidf.util.OsUtil.IS_WINDOWS;
import static org.bitk.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/8 20:37
 */
public class IdfProjectGenerator<T> extends CLionProjectGenerator<T> implements CustomStepProjectGenerator<T> {

    private WindowsGenerator<T> windowsGenerator;

    private UnixLikeGenerator<T> unixLikeGenerator;


    public IdfProjectGenerator() {

        if (IS_WINDOWS) {
            windowsGenerator = new WindowsGenerator<>();
        } else {
            unixLikeGenerator = new UnixLikeGenerator<>();
        }
    }

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
        if (!superResult.isOk()) {
            return superResult;
        }
        return IS_WINDOWS ? windowsGenerator.validate() : unixLikeGenerator.validate();
    }

    public void setIdfToolsPath(String text) {
        windowsGenerator.setIdfToolsPath(text);
    }


    public void setIdfId(String idfId) {
        windowsGenerator.setIdfId(idfId);
    }

    public void setIdfFrameworkPath(String idfFrameworkPath) {
        unixLikeGenerator.setIdfFrameworkPath(idfFrameworkPath);
    }

    @Override
    public AbstractActionWithPanel createStep(DirectoryProjectGenerator<T> directoryProjectGenerator, AbstractNewProjectStep.AbstractCallback<T> abstractCallback) {

        return IS_WINDOWS ? new IdfWindowsProjectSettingsStep<>(directoryProjectGenerator, abstractCallback) :
                new IdfUnixLikeProjectSettingsStep<>(directoryProjectGenerator, abstractCallback);
    }

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull T settings, @NotNull Module module) {
        super.generateProject(project, baseDir, settings, module);
        if(IS_WINDOWS) {
            windowsGenerator.generateProject(project, baseDir, settings, module);
        }

    }
}
