package org.btik.espidf.util;

import com.intellij.execution.RunContentDescriptorId;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 项目级「命令任务」执行记录：按项目隔离存储插件启动的非终端任务，
 * 由 {@link CmdTaskManager} 登记，并在进程结束/未能启动时标记状态（记录保留，不再立即移除）。
 * <p>
 * 运行中的记录用于查询与终止；已结束的记录用于回看执行情况与结果，并有两种淘汰时机：
 * 运行窗口里的内容被关闭后（在新建任务或任务结束时顺带清理，见 {@link #evictClosedRunContents()}），
 * 以及数量超过 {@link #MAX_FINISHED_TASKS} 时淘汰最早结束的兜底上限。
 * <p>
 * 使用项目级服务而非静态集合，保证多项目之间互不干扰。
 */
@Service(Service.Level.PROJECT)
public final class CmdTaskRegistry {

    /** 已结束任务的保留上限：超出时淘汰最早结束的记录。 */
    private static final int MAX_FINISHED_TASKS = 20;

    private final Project project;
    private final Map<Long, CmdTaskManager.CmdTask> tasks = new ConcurrentHashMap<>();
    private final AtomicLong nextTaskId = new AtomicLong();

    public CmdTaskRegistry(@NotNull Project project) {
        this.project = project;
    }

    public static @NotNull CmdTaskRegistry getInstance(@NotNull Project project) {
        return project.getService(CmdTaskRegistry.class);
    }

    public long nextTaskId() {
        return nextTaskId.incrementAndGet();
    }

    public void register(@NotNull CmdTaskManager.CmdTask task) {
        tasks.put(task.taskId(), task);
        evictClosedRunContentsAsync();
    }

    /** 标记任务已结束并记录退出码；记录保留，供后续查询执行结果。 */
    public void markFinished(long taskId, int exitCode) {
        CmdTaskManager.CmdTask task = tasks.get(taskId);
        if (task == null) {
            return;
        }
        task.markFinished(exitCode);
        evictClosedRunContentsAsync();
        evictFinishedOverflow();
    }

    /** 标记任务未能启动；记录保留，便于调用方区分「未启动」与「无此任务」。 */
    public void markNotStarted(long taskId) {
        CmdTaskManager.CmdTask task = tasks.get(taskId);
        if (task == null) {
            return;
        }
        task.markNotStarted();
        evictClosedRunContentsAsync();
        evictFinishedOverflow();
    }

    /**
     * 淘汰「运行窗口已被关闭」的已结束记录：输出已经不可用，记录也没有保留价值。
     * <p>
     * 不在关闭时立即响应（不订阅运行内容事件），只在新建任务或任务结束时顺带检查一次，
     * 因此窗口关闭到记录被清理之间允许有延迟；检查本身不创建运行内容服务，
     * 服务不存在（例如未打开过运行窗口）时不做任何淘汰，避免误删仍在窗口里的记录。
     */
    private void evictClosedRunContents() {
        try {
            if (project == null || project.isDisposed()) {
                return;
            }
            RunContentManager manager = RunContentManager.getInstanceIfCreated(project);
            if (manager == null) {
                return;
            }
            Set<RunContentDescriptorId> shownIds = new HashSet<>();
            Collection<RunContentDescriptor> shown = manager.getRunContentDescriptors();
            if (shown == null) {
                return;
            }
            for (RunContentDescriptor descriptor : shown) {
                if (descriptor.getId() != null) {
                    shownIds.add(descriptor.getId());
                }
            }
            for (CmdTaskManager.CmdTask task : tasks.values()) {
                if (task.isAlive()) {
                    continue;
                }
                RunContentDescriptor descriptor = task.descriptor();
                // id 为空说明该内容未进入运行内容服务，无法判断是否关闭，保守保留
                if (descriptor != null && descriptor.getId() != null && !shownIds.contains(descriptor.getId())) {
                    tasks.remove(task.taskId());
                }
            }
        } catch (Throwable ignored) {
            // 清理只是顺带做的优化，失败不影响任务记录本身
        }
    }

    /**
     * 触发一次关闭内容清理：需要读运行窗口（EDT 上的 UI 数据），
     * 后台线程改派到 EDT 异步执行（用户不感知延迟），无 Application 的单元测试环境直接同步执行。
     */
    private void evictClosedRunContentsAsync() {
        Application application = ApplicationManager.getApplication();
        if (application == null || application.isDispatchThread()) {
            evictClosedRunContents();
            return;
        }
        application.invokeLater(this::evictClosedRunContents);
    }

    /** 已结束记录超出上限时，淘汰最早结束的那些（结束时间相同时按 taskId 先后）。 */
    private void evictFinishedOverflow() {
        List<CmdTaskManager.CmdTask> finished = tasks.values().stream()
                .filter(task -> !task.isAlive())
                .sorted(Comparator.comparingLong(CmdTaskManager.CmdTask::finishedTimeMillis)
                        .thenComparingLong(CmdTaskManager.CmdTask::taskId))
                .toList();
        int overflow = finished.size() - MAX_FINISHED_TASKS;
        for (int i = 0; i < overflow; i++) {
            tasks.remove(finished.get(i).taskId());
        }
    }

    /** 当前仍存活的运行中任务（快照，按启动时间升序）。 */
    public @NotNull List<CmdTaskManager.CmdTask> activeTasks() {
        return tasks.values().stream()
                .filter(CmdTaskManager.CmdTask::isAlive)
                .sorted(Comparator.comparingLong(CmdTaskManager.CmdTask::startTimeMillis))
                .toList();
    }

    /**
     * 全部执行记录（快照）：运行中的在前，其后是已结束的记录，
     * 两组内部各自按启动时间升序（同一时间按 taskId 先后），便于按「先看活的、再看历史」的顺序阅读。
     */
    public @NotNull List<CmdTaskManager.CmdTask> tasks() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(CmdTaskManager.CmdTask::isAlive).reversed()
                        .thenComparingLong(CmdTaskManager.CmdTask::startTimeMillis)
                        .thenComparingLong(CmdTaskManager.CmdTask::taskId))
                .toList();
    }

    public @Nullable CmdTaskManager.CmdTask find(long taskId) {
        return tasks.get(taskId);
    }

    /** 终止指定任务；返回是否命中且仍在运行。 */
    public boolean terminate(long taskId) {
        CmdTaskManager.CmdTask task = tasks.get(taskId);
        if (task == null || !task.isAlive()) {
            return false;
        }
        task.processHandler().destroyProcess();
        return true;
    }

    /** 终止全部运行中的任务，返回请求终止的数量。 */
    public int terminateAll() {
        List<CmdTaskManager.CmdTask> active = activeTasks();
        for (CmdTaskManager.CmdTask task : active) {
            task.processHandler().destroyProcess();
        }
        return active.size();
    }

    public @NotNull Project project() {
        return project;
    }
}
