package org.btik.espidf.run.config.build;

import com.jetbrains.cidr.execution.CidrBuildConfiguration;
import org.jetbrains.annotations.NotNull;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
* @author lustre
*@since 2024/9/3 0:04
*/
public class EspIdfBuildConf implements CidrBuildConfiguration {
    @Override
    public @NotNull String getName() {
        return $i18n("esp.idf.debug.type");
    }
}
