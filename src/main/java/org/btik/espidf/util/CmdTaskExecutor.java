package org.btik.espidf.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.tools.ToolProcessAdapter;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/18 9:16
 */
public class CmdTaskExecutor {
    public static void execute(@NotNull Project project,
                               IdfConsoleRunProfile idfConsoleRunProfile, ProcessListener processListener)
            throws ExecutionException {

        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(
                project, DefaultRunExecutor.getRunExecutorInstance(),
                idfConsoleRunProfile).build();
        environment.setExecutionId(ExecutionEnvironment.getNextUnusedExecutionId());
        environment.getRunner().execute(environment);
        OSProcessHandler handler = new OSProcessHandler(idfConsoleRunProfile.getCommandLine());
        if (processListener != null) {
            handler.addProcessListener(processListener);
        }
        handler.startNotify();
    }

    public static void execute(@NotNull Project project,
                               IdfConsoleRunProfile idfConsoleRunProfile, Runnable successCallBack, String failedTip)
            throws ExecutionException {
        execute(project, idfConsoleRunProfile, new ProcessListener() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                ProcessListener.super.processTerminated(event);
                if (event.getExitCode() != 0) {
                    I18nMessage.NOTIFICATION_GROUP.createNotification(failedTip,
                            event.getText(), NotificationType.ERROR).notify(project);
                    return;
                }
                successCallBack.run();
            }
        });
    }

    public static String exeGetStdOut(GeneralCommandLine commandLine, int timeout) {

        try {
            ProcessOutput output = new CapturingProcessRunner(new CapturingProcessHandler(commandLine))
                    .runProcess(timeout);
            if (output.isTimeout()) {
                return null;
            } else if (output.getExitCode() != 0) {
                return null;
            } else {
                return output.getStdout();
            }
        } catch (ExecutionException e) {
            return null;
        }
    }
}
