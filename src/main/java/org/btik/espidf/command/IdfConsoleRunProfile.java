package org.btik.espidf.command;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
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

    private GeneralCommandLine commandLine;
    private CommandLineState commandLineState;

    private KillableColoredProcessHandler processHandler;

    private List<ProcessListener> processListeners;

    public IdfConsoleRunProfile(@NotNull String name, Icon icon, GeneralCommandLine commandLine) {
        this.name = name;
        this.icon = icon;
        this.commandLine = commandLine;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        commandLineState = new CommandLineState(environment) {
            @Override
            protected @NotNull ProcessHandler startProcess() throws ExecutionException {
                IdfConsoleRunProfile.this.processHandler = new KillableColoredProcessHandler(commandLine);
                if (processListeners != null) {
                    for (ProcessListener processListener : processListeners) {
                        IdfConsoleRunProfile.this.processHandler.addProcessListener(processListener);
                    }
                }
                TextConsoleBuilder consoleBuilder = getConsoleBuilder();
                System.out.println(consoleBuilder);
                if (consoleBuilder != null) {
                    ConsoleView console = consoleBuilder.getConsole();
                    if (console instanceof ConsoleViewImpl consoleView){
                        Editor editor = consoleView.getEditor();
                        if (editor != null) {
                            Document document = editor.getDocument();
                            document.setReadOnly(true);
                        }
                    }
                    System.out.println(console);
                }
                return IdfConsoleRunProfile.this.processHandler;
            }
        };

        return commandLineState;
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

    public KillableColoredProcessHandler getProcessHandler() {
        return processHandler;
    }
}
