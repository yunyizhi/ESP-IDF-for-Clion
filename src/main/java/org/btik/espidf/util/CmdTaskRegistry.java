package org.btik.espidf.util;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 项目级「命令任务」活跃清单：按项目隔离存储正在运行的非终端任务，
 * 由 {@link CmdTaskManager} 登记/移除，供查询与终止。
 * <p>
 * 使用项目级服务而非静态集合，保证多项目之间互不干扰。
 */
@Service(Service.Level.PROJECT)
public final class CmdTaskRegistry {

    private final Project project;
    private final Map<Long, CmdTaskManager.ActiveTask> activeTasks = new ConcurrentHashMap<>();
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

    public void register(@NotNull CmdTaskManager.ActiveTask task) {
        activeTasks.put(task.taskId(), task);
    }

    public void remove(long taskId) {
        activeTasks.remove(taskId);
    }

    /** 当前仍存活的活跃任务（快照，按启动时间升序）。 */
    public @NotNull List<CmdTaskManager.ActiveTask> activeTasks() {
        return activeTasks.values().stream()
                .filter(CmdTaskManager.ActiveTask::isAlive)
                .sorted(Comparator.comparingLong(CmdTaskManager.ActiveTask::startTimeMillis))
                .toList();
    }

    public @Nullable CmdTaskManager.ActiveTask find(long taskId) {
        return activeTasks.get(taskId);
    }

    /** 终止指定任务；返回是否命中并已请求终止。 */
    public boolean terminate(long taskId) {
        CmdTaskManager.ActiveTask task = activeTasks.get(taskId);
        if (task == null) {
            return false;
        }
        task.processHandler().destroyProcess();
        return true;
    }

    /** 终止全部活跃任务，返回请求终止的数量。 */
    public int terminateAll() {
        List<CmdTaskManager.ActiveTask> tasks = activeTasks();
        for (CmdTaskManager.ActiveTask task : tasks) {
            task.processHandler().destroyProcess();
        }
        return tasks.size();
    }

    public @NotNull Project project() {
        return project;
    }
}
