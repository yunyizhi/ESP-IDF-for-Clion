package org.btik.espidf.state;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfProjectConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author lustre
 * @since 2024/8/25 17:38
 */
@State(
        name = "ESP-IDF_SETTINGS"
        , storages = {@Storage("espidf_settings.xml")}
)
public class IdfProjectConfigComponent implements PersistentStateComponent<IdfProjectConfig>, IdfProjectConfigService {
    private final IdfProjectConfig idfProjectConfig = new IdfProjectConfig();

    @Override
    public @Nullable IdfProjectConfig getState() {
        return idfProjectConfig;
    }

    @Override
    public void loadState(@NotNull IdfProjectConfig idfProjectConfig) {
        XmlSerializerUtil.copyBean(idfProjectConfig, this.idfProjectConfig);
    }

    @Override
    public void updateProjectConfig(IdfProjectConfig idfProjectConfig) {
        XmlSerializerUtil.copyBean(idfProjectConfig, this.idfProjectConfig);
    }

    @Override
    public boolean hasValueChange(IdfProjectConfig viewObj) {
        return !Objects.equals(viewObj, idfProjectConfig);
    }

    @Override
    public IdfProjectConfig getProjectConfig() {
        return XmlSerializerUtil.createCopy(idfProjectConfig);
    }
}
