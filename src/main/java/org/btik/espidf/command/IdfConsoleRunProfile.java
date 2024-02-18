package org.btik.espidf.command;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author lustre
 * @since 2023/5/18 1:54
 */
public class IdfConsoleRunProfile implements RunProfile {
    private String name;

    private Icon icon;

    GeneralCommandLine commandLine;

    public IdfConsoleRunProfile(String name, Icon icon, GeneralCommandLine commandLine) {
        this.name = name;
        this.icon = icon;
        this.commandLine = commandLine;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new CommandLineState(environment) {
            @Override
            protected @NotNull ProcessHandler startProcess() throws ExecutionException {
                return new KillableProcessHandler(commandLine);
            }
        };
    }

    @Override
    public @NlsSafe @NotNull String getName() {
        return name;
    }

    @Override
    public @Nullable Icon getIcon() {
        return icon;
    }

    public GeneralCommandLine getCommandLine() {
        return commandLine;
    }
}
