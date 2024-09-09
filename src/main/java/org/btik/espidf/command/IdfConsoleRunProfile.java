package org.btik.espidf.command;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lustre
 * @since 2023/5/18 1:54
 */
public class IdfConsoleRunProfile implements RunProfile {
    private String name;

    private Icon icon;

    GeneralCommandLine commandLine;

    private KillableColoredProcessHandler processHandler;

    private List<ProcessListener> processListeners;

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
                IdfConsoleRunProfile.this.processHandler = new KillableColoredProcessHandler(commandLine);
                if (processListeners != null) {
                    for (ProcessListener processListener : processListeners) {
                        IdfConsoleRunProfile.this.processHandler.addProcessListener(processListener);
                    }
                }
                return IdfConsoleRunProfile.this.processHandler;
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

    public void addProcessListener(@NotNull ProcessListener listener) {
        if (processListeners == null) {
            processListeners = new ArrayList<>();
        }
        processListeners.add(listener);
    }
}
