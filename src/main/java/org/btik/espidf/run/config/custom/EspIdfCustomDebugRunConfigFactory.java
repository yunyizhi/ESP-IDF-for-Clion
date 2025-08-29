package org.btik.espidf.run.config.custom;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.btik.espidf.run.config.EspIdfDebugRunConfig;
import org.btik.espidf.run.config.EspIdfRunConfigType;
import org.btik.espidf.run.config.model.CustomDebugConfigModel;
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
        return new EspIdfDebugRunConfig<>(project, this, CustomDebugConfigModel::new,
                EspIdfCustomDebugSettingEditor::new);
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
