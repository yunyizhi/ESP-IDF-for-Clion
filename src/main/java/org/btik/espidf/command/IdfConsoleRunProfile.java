package org.btik.espidf.command;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
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

    private final boolean consoleReadOnly;

    private final GeneralCommandLine commandLine;

    private ProcessHandler processHandler;

    private List<ProcessListener> processListeners;

    private ConsoleView consoleView;

    public IdfConsoleRunProfile(@NotNull String name, Icon icon, GeneralCommandLine commandLine) {
        this(name, icon, commandLine, false);
    }

    public IdfConsoleRunProfile(@NotNull String name, Icon icon, GeneralCommandLine commandLine, boolean consoleReadOnly) {
        this.name = name;
        this.icon = icon;
        this.commandLine = commandLine;
        this.consoleReadOnly = consoleReadOnly;
    }

    public void setProcessHandler(ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {

        CommandLineState commandLineState = new CommandLineState(environment) {
            @Override
            protected @NotNull ProcessHandler startProcess() throws ExecutionException {
                if (IdfConsoleRunProfile.this.processHandler == null) {
                    IdfConsoleRunProfile.this.processHandler = new KillableColoredProcessHandler(commandLine);
                }
                if (processListeners != null) {
                    for (ProcessListener processListener : processListeners) {
                        IdfConsoleRunProfile.this.processHandler.addProcessListener(processListener);
                    }
                }
                return IdfConsoleRunProfile.this.processHandler;
            }
        };
        if (consoleReadOnly) {
            TextConsoleBuilder consoleBuilder = commandLineState.getConsoleBuilder();
            if (consoleBuilder != null) {
                consoleBuilder.setViewer(true);
                consoleView = consoleBuilder.getConsole();
            }
        }
        return commandLineState;
    }

    @Override
    public @NotNull String getName() {
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

    public ProcessHandler getProcessHandler() {
        return processHandler;
    }

    public void println(String str) {
        print(str);
        print(System.lineSeparator());
    }

    public void print(String msg) {
        print(msg, ConsoleViewContentType.NORMAL_OUTPUT);
    }

    public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
        if (consoleView != null) {
            consoleView.print(text, contentType);
        }
    }


    public void println(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
        print(text, contentType);
        print(System.lineSeparator(), contentType);
    }
}
