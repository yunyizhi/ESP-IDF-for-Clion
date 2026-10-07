package org.btik.espidf.util;

import com.intellij.execution.RunContentDescriptorId;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link CmdTaskRegistry} 的执行记录语义：
 * 任务结束后记录保留（含退出码），运行中的任务排在前面，
 * 已结束记录超出上限时淘汰最早结束的那些。
 * <p>
 * 用假的 {@link ProcessHandler} 代替真实进程，直接实例化注册表（只用到存储逻辑，不依赖平台服务）。
 */
public class CmdTaskRegistryTest {

    /** 注册表内的已结束记录保留上限（与 CmdTaskRegistry#MAX_FINISHED_TASKS 一致） */
    private static final int MAX_FINISHED_TASKS = 20;

    /** 执行记录只用到 Project 作为归属标记，无需真实项目 */
    private static final Project PROJECT = Mockito.mock(Project.class);

    private static class FakeProcessHandler extends ProcessHandler {

        private boolean destroyed;

        FakeProcessHandler() {
            // ProcessHandler 会把终止事件排队到 startNotify() 之后派发，假 handler 需显式触发一次
            startNotify();
        }

        @Override
        protected void destroyProcessImpl() {
            destroyed = true;
            notifyProcessTerminated(0);
        }

        @Override
        protected void detachProcessImpl() {
            notifyProcessTerminated(0);
        }

        @Override
        public boolean detachIsDefault() {
            return false;
        }

        @Override
        public @Nullable OutputStream getProcessInput() {
            return null;
        }
    }

    private static CmdTaskManager.CmdTask task(long taskId, @NotNull ProcessHandler handler, long startTime) {
        return task(taskId, handler, startTime, null);
    }

    private static CmdTaskManager.CmdTask task(long taskId, @NotNull ProcessHandler handler, long startTime,
                                               @Nullable RunContentDescriptor descriptor) {
        return new CmdTaskManager.CmdTask(taskId, PROJECT, "task-" + taskId, taskId, startTime, handler, descriptor);
    }

    private static CmdTaskRegistry registry() {
        return new CmdTaskRegistry(PROJECT);
    }

    /** 带「运行内容服务」的项目：{@code shown} 表示当前运行窗口里还留着的内容 */
    private static CmdTaskRegistry registryWithRunContents(@Nullable Collection<RunContentDescriptor> shown) {
        Project project = Mockito.mock(Project.class);
        RunContentManager manager = Mockito.mock(RunContentManager.class);
        Mockito.when(project.getServiceIfCreated(RunContentManager.class)).thenReturn(manager);
        Mockito.when(manager.getRunContentDescriptors()).thenReturn(shown == null ? List.of() : shown);
        return new CmdTaskRegistry(project);
    }

    private static RunContentDescriptor descriptor() {
        RunContentDescriptor descriptor = Mockito.mock(RunContentDescriptor.class);
        Mockito.when(descriptor.getId()).thenReturn(Mockito.mock(RunContentDescriptorId.class));
        return descriptor;
    }

    /** 结束的任务仍留在记录里，并带上退出码与耗时 */
    @Test
    public void finishedTaskIsRetainedWithExitCode() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        CmdTaskManager.CmdTask task = task(taskId, new FakeProcessHandler(), System.currentTimeMillis());
        registry.register(task);

        assertTrue(task.isAlive());
        assertEquals(CmdTaskManager.TaskState.RUNNING, task.state());
        assertEquals(-1, task.exitCode());

        registry.markFinished(taskId, 7);

        assertFalse(task.isAlive());
        assertEquals(CmdTaskManager.TaskState.EXITED, task.state());
        assertEquals(7, task.exitCode());
        assertTrue(task.finishedTimeMillis() > 0);
        assertTrue(task.durationMillis() >= 0);
        assertEquals(1, registry.tasks().size());
        assertTrue(registry.activeTasks().isEmpty());
        assertNotNull(registry.find(taskId));
    }

    /** 未能启动的任务同样保留，便于区分「未启动」与「无此记录」 */
    @Test
    public void notStartedTaskIsRetained() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        CmdTaskManager.CmdTask task = task(taskId, new FakeProcessHandler(), System.currentTimeMillis());
        registry.register(task);

        registry.markNotStarted(taskId);

        assertFalse(task.isAlive());
        assertEquals(CmdTaskManager.TaskState.NOT_STARTED, task.state());
        assertEquals(1, registry.tasks().size());
    }

    /** 列表把运行中的任务排在已结束记录之前 */
    @Test
    public void runningTasksComeFirst() {
        CmdTaskRegistry registry = registry();
        CmdTaskManager.CmdTask finished = task(registry.nextTaskId(), new FakeProcessHandler(), 1000L);
        CmdTaskManager.CmdTask running = task(registry.nextTaskId(), new FakeProcessHandler(), 2000L);
        registry.register(finished);
        registry.register(running);
        registry.markFinished(finished.taskId(), 0);

        List<CmdTaskManager.CmdTask> all = registry.tasks();
        assertEquals(2, all.size());
        assertEquals(running.taskId(), all.get(0).taskId());
        assertEquals(finished.taskId(), all.get(1).taskId());
        assertEquals(List.of(running.taskId()),
                registry.activeTasks().stream().map(CmdTaskManager.CmdTask::taskId).toList());
    }

    /** 已结束记录超出上限时淘汰最早结束的，运行中的不受影响 */
    @Test
    public void oldestFinishedTasksAreEvicted() {
        CmdTaskRegistry registry = registry();
        CmdTaskManager.CmdTask running = task(registry.nextTaskId(), new FakeProcessHandler(), 0L);
        registry.register(running);

        List<Long> finishedIds = new ArrayList<>();
        for (int i = 0; i < MAX_FINISHED_TASKS + 5; i++) {
            long taskId = registry.nextTaskId();
            registry.register(task(taskId, new FakeProcessHandler(), i));
            finishedIds.add(taskId);
        }
        for (Long taskId : finishedIds) {
            registry.markFinished(taskId, 0);
        }

        assertEquals(MAX_FINISHED_TASKS + 1, registry.tasks().size());
        for (int i = 0; i < 5; i++) {
            assertNull(registry.find(finishedIds.get(i)));
        }
        assertNotNull(registry.find(finishedIds.get(MAX_FINISHED_TASKS)));
        assertNotNull(registry.find(running.taskId()));
    }

    /** 运行窗口里内容已关闭的已结束记录，在任务结束时被顺带淘汰 */
    @Test
    public void closedRunContentRecordIsEvicted() {
        CmdTaskRegistry registry = registryWithRunContents(List.of());
        long taskId = registry.nextTaskId();
        registry.register(task(taskId, new FakeProcessHandler(), System.currentTimeMillis(), descriptor()));

        registry.markFinished(taskId, 0);

        assertNull(registry.find(taskId));
        assertTrue(registry.tasks().isEmpty());
    }

    /** 运行窗口里内容还在的已结束记录必须保留（否则就失去了回看输出的来源） */
    @Test
    public void recordWithOpenRunContentIsRetained() {
        RunContentDescriptor runContent = descriptor();
        CmdTaskRegistry registry = registryWithRunContents(List.of(runContent));
        long taskId = registry.nextTaskId();
        registry.register(task(taskId, new FakeProcessHandler(), System.currentTimeMillis(), runContent));

        registry.markFinished(taskId, 0);

        assertNotNull(registry.find(taskId));
        assertEquals(1, registry.tasks().size());
    }

    /** 运行内容服务不存在（未打开过运行窗口）时不做淘汰，避免误删仍在窗口里的记录 */
    @Test
    public void missingRunContentServiceKeepsRecords() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        registry.register(task(taskId, new FakeProcessHandler(), System.currentTimeMillis(), descriptor()));

        registry.markFinished(taskId, 0);

        assertNotNull(registry.find(taskId));
    }

    /** 新建任务时同样会顺带清理已关闭的记录 */
    @Test
    public void newTaskAlsoEvictsClosedRecords() {
        RunContentDescriptor closed = descriptor();
        Project project = Mockito.mock(Project.class);
        RunContentManager manager = Mockito.mock(RunContentManager.class);
        Mockito.when(project.getServiceIfCreated(RunContentManager.class)).thenReturn(manager);
        // 用可变的「当前运行窗口内容」模拟 tab 的打开与关闭
        AtomicReference<Collection<RunContentDescriptor>> shown = new AtomicReference<>(List.of(closed));
        Mockito.when(manager.getRunContentDescriptors()).thenAnswer(invocation -> shown.get());
        CmdTaskRegistry registry = new CmdTaskRegistry(project);

        long firstId = registry.nextTaskId();
        registry.register(task(firstId, new FakeProcessHandler(), 1L, closed));
        registry.markFinished(firstId, 0);
        assertNotNull("窗口没关闭时记录必须保留", registry.find(firstId));

        shown.set(List.of());
        long secondId = registry.nextTaskId();
        registry.register(task(secondId, new FakeProcessHandler(), 2L, descriptor()));

        assertNull("新建任务时应顺带淘汰窗口已关闭的记录", registry.find(firstId));
        assertNotNull(registry.find(secondId));
    }

    /** 已结束的任务不能再终止 */
    @Test
    public void terminateIgnoresFinishedTask() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        FakeProcessHandler handler = new FakeProcessHandler();
        registry.register(task(taskId, handler, System.currentTimeMillis()));
        registry.markFinished(taskId, 0);

        assertFalse(registry.terminate(taskId));
        assertFalse(handler.destroyed);
        assertFalse(registry.terminate(taskId + 999));
    }
}
