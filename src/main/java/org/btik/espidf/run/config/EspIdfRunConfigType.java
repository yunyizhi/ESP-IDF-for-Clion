package org.btik.espidf.run.config;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import org.btik.espidf.icon.EspIdfIcon;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/9/2 20:57
 */
public class EspIdfRunConfigType implements ConfigurationType {
    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return $sys("esp.idf.debug.type.display.name");
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getConfigurationTypeDescription() {
        return $i18n("esp.idf.debug.type");
    }

    @Override
    public Icon getIcon() {
        return EspIdfIcon.IDF_16_16;
    }

    @Override
    public @NotNull @NonNls String getId() {
        return $sys("esp.idf.debug.type.id");
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{
                new EspIdfRunConfigFactory(this)
        };
    }

}
