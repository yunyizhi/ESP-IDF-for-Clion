package org.btik.espidf.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
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
 * 并把「任务执行记录」按项目登记到 {@link CmdTaskRegistry}，提供查询与终止能力。
 * <p>
 * 执行记录不再只覆盖「运行中」：进程结束（含未能启动）后记录仍保留，
 * 以便回看执行情况，并通过 {@link RunContentOutputs} 读取运行窗口里仍保留的控制台输出。
 * <p>
 * 覆盖范围：经本类启动的非终端任务（{@code command} / {@code exec}(非终端) / {@code raw-command} / monitor / size 等）。
 * 终端类任务（{@code console-command}、{@code in-terminal} 的 {@code exec}）走 ShRunConfiguration，不在其中。
 *
 * @author lustre
 * @since 2024/2/18 9:16
 */
public final class CmdTaskManager {

    /** 任务执行状态。 */
    public enum TaskState {
        /** 进程已启动并仍在运行。 */
        RUNNING,
        /** 进程已结束（含被终止）。 */
        EXITED,
        /** 进程未能启动。 */
        NOT_STARTED
    }

    /** 一次命令任务的执行记录，运行期与结束之后都保留。 */
    public static final class CmdTask {

        private final long taskId;
        private final @NotNull Project project;
        private final @NotNull String name;
        private final long executionId;
        private final long startTimeMillis;
        private final @NotNull ProcessHandler processHandler;
        private final @Nullable RunContentDescriptor descriptor;

        private volatile TaskState state = TaskState.RUNNING;
        private volatile int exitCode = -1;
        private volatile long finishedTimeMillis = -1L;

        CmdTask(long taskId,
                @NotNull Project project,
                @NotNull String name,
                long executionId,
                long startTimeMillis,
                @NotNull ProcessHandler processHandler,
                @Nullable RunContentDescriptor descriptor) {
            this.taskId = taskId;
            this.project = project;
            this.name = name;
            this.executionId = executionId;
            this.startTimeMillis = startTimeMillis;
            this.processHandler = processHandler;
            this.descriptor = descriptor;
        }

        public long taskId() {
            return taskId;
        }

        public @NotNull Project project() {
            return project;
        }

        public @NotNull String name() {
            return name;
        }

        public long executionId() {
            return executionId;
        }

        public long startTimeMillis() {
            return startTimeMillis;
        }

        /** 底层进程处理器；结束后依然可读退出码。 */
        public @NotNull ProcessHandler processHandler() {
            return processHandler;
        }

        /** 对应的运行窗口内容；用于读取控制台输出（tab 关闭后可能已不可用）。 */
        public @Nullable RunContentDescriptor descriptor() {
            return descriptor;
        }

        public @NotNull TaskState state() {
            return state;
        }

        /** 进程退出码；未结束或未能启动时为 {@code -1}。 */
        public int exitCode() {
            return exitCode;
        }

        /** 结束时间；仍在运行时为 {@code -1}。 */
        public long finishedTimeMillis() {
            return finishedTimeMillis;
        }

        /** 已运行/总运行时长（毫秒）。 */
        public long durationMillis() {
            long end = finishedTimeMillis > 0 ? finishedTimeMillis : System.currentTimeMillis();
            return end - startTimeMillis;
        }

        /** 底层进程是否仍在运行。 */
        public boolean isAlive() {
            return state == TaskState.RUNNING && !processHandler.isProcessTerminated();
        }

        void markFinished(int exitCode) {
            this.exitCode = exitCode;
            this.finishedTimeMillis = System.currentTimeMillis();
            this.state = TaskState.EXITED;
        }

        void markNotStarted() {
            this.exitCode = -1;
            this.finishedTimeMillis = System.currentTimeMillis();
            this.state = TaskState.NOT_STARTED;
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
            registry.register(new CmdTask(taskId, project, safeNull(idfConsoleRunProfile.getName()),
                    runContentDescriptor.getExecutionId(), System.currentTimeMillis(),
                    processHandler, runContentDescriptor));
            // 结束与未启动都只标记状态：记录保留在注册表中，供查询执行情况与回看输出
            processHandler.addProcessListener(new ProcessEventAdaptor()
                    .withProcessTerminatedCb(event -> registry.markFinished(taskId, event.getExitCode()))
                    .withProcessNotStartedCb(() -> registry.markNotStarted(taskId)));
        });
        environment.setExecutionId(ExecutionEnvironment.getNextUnusedExecutionId());
        environment.getRunner().execute(environment);
    }

    /** 当前项目的运行中任务（快照，按启动时间升序）。 */
    public static @NotNull List<CmdTask> activeTasks(@NotNull Project project) {
        return CmdTaskRegistry.getInstance(project).activeTasks();
    }

    /** 当前项目的全部任务执行记录：运行中的在前，其后是已结束的记录。 */
    public static @NotNull List<CmdTask> tasks(@NotNull Project project) {
        return CmdTaskRegistry.getInstance(project).tasks();
    }

    /** 按 taskId 查找当前项目的任务执行记录（含已结束的）。 */
    public static @Nullable CmdTask findTask(@NotNull Project project, long taskId) {
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
