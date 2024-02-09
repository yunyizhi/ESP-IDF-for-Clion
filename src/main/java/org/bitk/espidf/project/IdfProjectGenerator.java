package org.bitk.espidf.project;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CMakeProjectGenerator;
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.settings.ui.CMakeSettingsPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

import static org.bitk.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/8 20:37
 */
public class IdfProjectGenerator extends CMakeProjectGenerator {
    private String idfToolPath;

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
        return ValidationResult.OK;
    }

    @Override
    protected VirtualFile @NotNull [] createSourceFiles(@NotNull String s, @NotNull VirtualFile virtualFile) throws IOException {
        return new VirtualFile[0];
    }

    protected CMakeSettingsPanel createSettingsPanel() {
        return new IdfGeneratorPanel(this);
    }

    public void setIdfToolsPath(String text) {
        idfToolPath = text;
    }


    public void setIdfId(String idfId) {

    }
}
