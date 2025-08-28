package org.btik.espidf.run.config;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
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
        return new EspIdfGdbInitDebugRunConfig(project, this);
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
