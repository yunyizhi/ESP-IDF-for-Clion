package org.btik.espidf.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.jetbrains.annotations.NotNull;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.util.StringUtils.safeNull;

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
                idfConsoleRunProfile).build(runContentDescriptor -> {
                    ProcessHandler processHandler = runContentDescriptor.getProcessHandler();
                    if (processHandler != null && processListener != null) {
                        processHandler.addProcessListener(processListener);
                    }
                });
        environment.setExecutionId(ExecutionEnvironment.getNextUnusedExecutionId());
        environment.getRunner().execute(environment);
    }

    /**
     * @param continueWithError 遇到错误继续，尽量减少idf误报时，中断可以设为true
     */
    public static void execute(@NotNull Project project,
                               IdfConsoleRunProfile idfConsoleRunProfile, Runnable terminatedCallBack, String failedTip, boolean continueWithError)
            throws ExecutionException {
        execute(project, idfConsoleRunProfile, new ProcessListener() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                if (event.getExitCode() != 0) {
                    I18nMessage.NOTIFICATION_GROUP.createNotification(failedTip,
                            $i18nF("idf.exec.return.error", safeNull(event.getText()), event.getExitCode())
                                    + (continueWithError ? "<br>" + $i18n("next.task.run") : "")
                            , NotificationType.WARNING).notify(project);
                    if (!continueWithError) {
                        return;
                    }
                }
                terminatedCallBack.run();
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
