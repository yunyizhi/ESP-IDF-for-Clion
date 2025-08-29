package org.btik.espidf.run.config;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.jetbrains.cidr.cpp.execution.CLionRunConfiguration;
import com.jetbrains.cidr.execution.CidrBuildConfigurationHelper;
import com.jetbrains.cidr.execution.CidrCommandLineState;
import com.jetbrains.cidr.execution.ExecutableData;
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration;
import org.btik.espidf.run.config.build.EspIdfBuildConf;
import org.btik.espidf.run.config.build.EspIdfBuildConfHelper;
import org.btik.espidf.run.config.build.EspIdfBuildTarget;
import org.btik.espidf.util.RunConfigDataUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author lustre
 * @since 2024/9/2 21:17
 */
public class EspIdfDebugRunConfig<T> extends CLionRunConfiguration<EspIdfBuildConf, EspIdfBuildTarget> {

    private ExecutableData executableData;
    private T configDataModel;
    private final Supplier<T> configDataModelNew;
    private final Function<Project, SettingsEditor<? extends RunConfiguration>> settingsEditorNew;

    public EspIdfDebugRunConfig(Project project, ConfigurationFactory factory,
                                @NotNull Supplier<T> configDataModelNew,
                                @NotNull Function<Project, SettingsEditor<? extends RunConfiguration>> settingsEditorNew) {
        super(project, factory, factory.getName());
        this.configDataModelNew = configDataModelNew;
        this.settingsEditorNew = settingsEditorNew;
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return settingsEditorNew.apply(getProject());
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        var historyConfigDataModel = configDataModelNew.get();
        RunConfigDataUtil.readFromElement(element, historyConfigDataModel);
        setConfigDataModel(historyConfigDataModel);
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        if (configDataModel == null) {
            return;
        }
        final T dataModel = configDataModel;
        RunConfigDataUtil.writeToElement(element, dataModel);
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
        var espIdfLauncher = new EspIdfLauncher<>(executionEnvironment, this);
        return new CidrCommandLineState(executionEnvironment, espIdfLauncher);
    }


    public T getConfigDataModel() {
        return configDataModel;
    }

    public void setConfigDataModel(T configDataModel) {
        this.configDataModel = configDataModel;
    }
}
