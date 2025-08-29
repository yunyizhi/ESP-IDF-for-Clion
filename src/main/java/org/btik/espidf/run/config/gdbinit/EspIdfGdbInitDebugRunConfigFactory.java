package org.btik.espidf.run.config.gdbinit;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.btik.espidf.run.config.EspIdfDebugRunConfig;
import org.btik.espidf.run.config.EspIdfRunConfigType;
import org.btik.espidf.run.config.model.GdbInitDebugConfigModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static org.btik.espidf.util.SysConf.$sys;


public class EspIdfGdbInitDebugRunConfigFactory extends ConfigurationFactory {
    public EspIdfGdbInitDebugRunConfigFactory(EspIdfRunConfigType espIdfRunConfigType) {
        super(espIdfRunConfigType);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new EspIdfDebugRunConfig<>(project, this, GdbInitDebugConfigModel::new,
                EspIdfGdbInitDebugSettingEditor::new);
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "org.btik.ESPIdfGdbInitDebugFactory";
    }

    @Override
    public @NotNull @Nls String getName() {
        return $sys("esp.idf.debug.gdbinit.name");
    }
}
