package org.btik.espidf.run.config;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/9/2 21:11
 */
public class EspIdfCustomDebugRunConfigFactory extends ConfigurationFactory {

    public EspIdfCustomDebugRunConfigFactory(EspIdfRunConfigType espIdfRunConfigType) {
        super(espIdfRunConfigType);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new EspIdfCustomDebugRunConfig(project, this);
    }

    @Override
    public @NotNull @NonNls String getId() {
        return $sys("esp.idf.run.config.type.factory.id");
    }

    @Override
    public @NotNull @Nls String getName() {
        return $sys("esp.idf.debug.name");
    }
}
