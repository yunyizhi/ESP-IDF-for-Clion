package org.btik.espidf.run.config.build;

import com.jetbrains.cidr.execution.CidrBuildTarget;
import org.btik.espidf.icon.EspIdfIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/9/3 0:07
 */
public class EspIdfBuildTarget implements CidrBuildTarget<EspIdfBuildConf> {
    private final String projectName;
    private final EspIdfBuildConf espIdfBuildConf;

    public EspIdfBuildTarget(String projectName) {
        this.projectName = projectName;
        this.espIdfBuildConf = new EspIdfBuildConf();
    }

    @Override
    public @NotNull String getName() {
        return $i18n("esp.idf.debug.type");
    }

    @Override
    public @NotNull String getProjectName() {
        return projectName;
    }

    @Override
    public @Nullable Icon getIcon() {
        return EspIdfIcon.IDF_16_16;
    }

    @Override
    public boolean isExecutable() {
        return true;
    }

    @Override
    public @NotNull List<EspIdfBuildConf> getBuildConfigurations() {
        return List.of(espIdfBuildConf);
    }
}
