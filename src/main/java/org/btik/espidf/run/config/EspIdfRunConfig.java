package org.btik.espidf.run.config;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.jetbrains.cidr.cpp.execution.CLionRunConfiguration;
import com.jetbrains.cidr.execution.CidrBuildConfigurationHelper;
import com.jetbrains.cidr.execution.CidrCommandLineState;
import com.jetbrains.cidr.execution.ExecutableData;
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration;
import org.btik.espidf.run.config.build.EspIdfBuildConf;
import org.btik.espidf.run.config.build.EspIdfBuildConfHelper;
import org.btik.espidf.run.config.build.EspIdfBuildTarget;
import org.btik.espidf.run.config.model.DebugConfigModel;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.util.ClassMetaUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/9/2 21:17
 */
public class EspIdfRunConfig extends CLionRunConfiguration<EspIdfBuildConf, EspIdfBuildTarget> {


    private static final String OPEN_OCD_ARGUMENTS = "OPEN_OCD_ARGUMENTS";
    private ExecutableData executableData;
    private DebugConfigModel configDataModel;

    public EspIdfRunConfig(Project project, ConfigurationFactory factory) {
        super(project, factory, $sys("esp.idf.debug.name"));
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new EspIdfDebugSettingEditor(getProject());
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        if (configDataModel == null) {
            configDataModel = new DebugConfigModel();
        }
        IdfSysConfService sysConfService = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        List<ClassMetaUtils.PropOptMeta> propOptMetas = sysConfService.getPropOptMetas();
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

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        if (configDataModel == null) {
            return;
        }
        final DebugConfigModel dataModel = configDataModel;
        IdfSysConfService sysConfService = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        List<ClassMetaUtils.PropOptMeta> propOptMetas = sysConfService.getPropOptMetas();
        for (ClassMetaUtils.PropOptMeta propOptMeta : propOptMetas) {
            Class<?> aClass = ClassMetaUtils.propType(propOptMeta);
            if (aClass == String.class) {
                String stringValue = ClassMetaUtils.get(propOptMeta, dataModel);
                JDOMExternalizerUtil.writeField(element, propOptMeta.propName(), stringValue);
                continue;
            }
            if (aClass == EnvironmentVariablesData.class) {
                EnvironmentVariablesData environmentVariablesData = ClassMetaUtils.get(propOptMeta, dataModel);
                if (environmentVariablesData == null) {
                    continue;
                }
                EnvironmentVariablesComponent.writeExternal(element, environmentVariablesData.getEnvs());
            }

        }

    }

    @Override
    public @NotNull CidrBuildConfigurationHelper<EspIdfBuildConf, EspIdfBuildTarget> getHelper() {
        return new EspIdfBuildConfHelper(getProject());
    }

    @Override
    public @Nullable OCResolveConfiguration getResolveConfiguration(@NotNull ExecutionTarget executionTarget) {
        return null;
    }

    @Override
    public @Nullable ExecutableData getExecutableData() {
        return executableData;
    }

    @Override
    public void setExecutableData(@Nullable ExecutableData executableData) {
        this.executableData = executableData;
    }

    @Override
    public @Nullable CommandLineState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        EspIdfLauncher espIdfLauncher = new EspIdfLauncher(executionEnvironment, this);
        return new CidrCommandLineState(executionEnvironment, espIdfLauncher);
    }


    public DebugConfigModel getConfigDataModel() {
        return configDataModel;
    }

    public void setConfigDataModel(DebugConfigModel configDataModel) {
        this.configDataModel = configDataModel;
    }
}
