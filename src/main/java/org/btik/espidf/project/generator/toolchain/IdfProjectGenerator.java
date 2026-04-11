package org.btik.espidf.project.generator.toolchain;

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

public class IdfProjectGenerator<T> extends CLionProjectGenerator<T> implements CustomStepProjectGenerator<T> {

    private final IdfProjectCreatorActions<T> idfProjectCreatorActions = new IdfProjectCreatorActions<>();
    @Override
    public AbstractActionWithPanel createStep(DirectoryProjectGenerator<T> projectGenerator, AbstractNewProjectStep.AbstractCallback<T> callback) {
        return new IdfProjectSettingsStep<>(projectGenerator, callback);
    }

    @Override
    public @NlsContexts.Label @NotNull String getName() {
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
        return IconLoader.getIcon("/org-btik-esp-idf/image/idf16_16.svg", getClass());
    }

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull T settings, @NotNull Module module) {
        idfProjectCreatorActions.generateProject(project, baseDir, settings, module);
    }

    public void setIdfToolChian(IdfToolchain idfToolchain){
        idfProjectCreatorActions.setIdfToolchain(idfToolchain);
    }

    public void setIdfTarget(String idfTarget){
        idfProjectCreatorActions.setIdfTarget(idfTarget);
    }
}
