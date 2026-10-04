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
import org.btik.espidf.command.ProcessEventAdaptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.util.StringTools.safeNull;

/**
 * 命令任务管理器（静态门面）：统一通过 Run 内容执行命令任务（与界面点击一致），
 * 并把「活跃任务」按项目登记到 {@link CmdTaskRegistry}，提供查询与终止能力。
 * <p>
 * 覆盖范围：经本类启动的非终端任务（{@code command} / {@code exec}(非终端) / {@code raw-command} / monitor / size 等）。
 * 终端类任务（{@code console-command}、{@code in-terminal} 的 {@code exec}）走 ShRunConfiguration，不在其中。
 *
 * @author lustre
 * @since 2024/2/18 9:16
 */
public final class CmdTaskManager {

    /** 一个当前活跃（正在运行）的任务。 */
    public record ActiveTask(long taskId,
                             @NotNull Project project,
                             @NotNull String name,
                             long executionId,
                             long startTimeMillis,
                             @NotNull ProcessHandler processHandler) {

        /** 底层进程是否仍在运行。 */
        public boolean isAlive() {
            return !processHandler.isProcessTerminated();
        }
    }

    private CmdTaskManager() {
    }

    public static void execute(@NotNull Project project,
                               IdfConsoleRunProfile idfConsoleRunProfile, ProcessListener processListener)
            throws ExecutionException {

        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(
                project, DefaultRunExecutor.getRunExecutorInstance(),
                idfConsoleRunProfile).build(runContentDescriptor -> {
            ProcessHandler processHandler = runContentDescriptor.getProcessHandler();
            if (processHandler == null) {
                return;
            }
            if (processListener != null) {
                processHandler.addProcessListener(processListener);
            }
            CmdTaskRegistry registry = CmdTaskRegistry.getInstance(project);
            long taskId = registry.nextTaskId();
            registry.register(new ActiveTask(taskId, project, safeNull(idfConsoleRunProfile.getName()),
                    runContentDescriptor.getExecutionId(), System.currentTimeMillis(), processHandler));
            processHandler.addProcessListener(new ProcessEventAdaptor()
                    .withProcessTerminatedCb(event -> registry.remove(taskId))
                    .withProcessNotStartedCb(() -> registry.remove(taskId)));
        });
        environment.setExecutionId(ExecutionEnvironment.getNextUnusedExecutionId());
        environment.getRunner().execute(environment);
    }

    /** 当前项目的活跃任务（快照，按启动时间升序）。 */
    public static @NotNull List<ActiveTask> activeTasks(@NotNull Project project) {
        return CmdTaskRegistry.getInstance(project).activeTasks();
    }

    /** 按 taskId 查找当前项目的活跃任务。 */
    public static @Nullable ActiveTask findTask(@NotNull Project project, long taskId) {
        return CmdTaskRegistry.getInstance(project).find(taskId);
    }

    /** 终止当前项目中指定 taskId 的任务；返回是否命中并已请求终止。 */
    public static boolean terminate(@NotNull Project project, long taskId) {
        return CmdTaskRegistry.getInstance(project).terminate(taskId);
    }

    /**
     * 执行任务，失败时提示；{@code continueWithError} 为 true 时遇错继续。
     *
     * @param continueWithError 遇到错误继续，尽量减少idf误报时，中断可以设为true
     */
    public static void execute(@NotNull Project project,
                               IdfConsoleRunProfile idfConsoleRunProfile, Runnable terminatedCallBack, String failedTip, boolean continueWithError)
            throws ExecutionException {
        execute(project, idfConsoleRunProfile, new ProcessEventAdaptor().withProcessTerminatedCb((event) -> {
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
        }));
    }

    public static String exeGetStdOut(GeneralCommandLine commandLine, int timeoutInMilliseconds) {

        try {
            ProcessOutput output = new CapturingProcessRunner(new CapturingProcessHandler(commandLine))
                    .runProcess(timeoutInMilliseconds);
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
