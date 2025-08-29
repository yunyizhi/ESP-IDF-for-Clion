package org.btik.espidf.util;

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import org.btik.espidf.service.IdfSysConfService;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RunConfigDataUtil {

    public static void readFromElement(@NotNull Element element, @NotNull Object configDataModel) {
        IdfSysConfService sysConfService = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        List<ClassMetaUtils.PropOptMeta> propOptMetas = sysConfService.getPropOptMetas(configDataModel.getClass());
        for (ClassMetaUtils.PropOptMeta propOptMeta : propOptMetas) {
            Class<?> aClass = ClassMetaUtils.propType(propOptMeta);
            if (aClass == String.class) {
                String stringValue = JDOMExternalizerUtil.readField(element, propOptMeta.propName());
                ClassMetaUtils.set(propOptMeta, configDataModel, stringValue);
                continue;
            }
            if (aClass == EnvironmentVariablesData.class) {
                EnvironmentVariablesData environmentVariablesData = EnvironmentVariablesData.readExternal(element);
                ClassMetaUtils.set(propOptMeta, configDataModel, environmentVariablesData);
            }
        }
    }

    public static void writeToElement(@NotNull Element element, @NotNull Object configDataModel) {
        IdfSysConfService sysConfService = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        List<ClassMetaUtils.PropOptMeta> propOptMetas = sysConfService.getPropOptMetas(configDataModel.getClass());
        for (ClassMetaUtils.PropOptMeta propOptMeta : propOptMetas) {
            Class<?> aClass = ClassMetaUtils.propType(propOptMeta);
            if (aClass == String.class) {
                String stringValue = ClassMetaUtils.get(propOptMeta, configDataModel);
                JDOMExternalizerUtil.writeField(element, propOptMeta.propName(), stringValue);
                continue;
            }
            if (aClass == EnvironmentVariablesData.class) {
                EnvironmentVariablesData environmentVariablesData = ClassMetaUtils.get(propOptMeta, configDataModel);
                if (environmentVariablesData == null) {
                    continue;
                }
                EnvironmentVariablesComponent.writeExternal(element, environmentVariablesData.getEnvs());
            }

        }
    }
}
