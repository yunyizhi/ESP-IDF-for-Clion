package org.btik.espidf.util;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link CmdTaskRegistry} 的执行记录语义：
 * 任务结束后记录保留（含退出码），运行中的任务排在前面，已结束记录超出上限时淘汰最早结束的那些，
 * 运行内容已被关闭（descriptor 被 dispose）的记录则在新建任务/任务结束时被顺带淘汰。
 * <p>
 * 用假的 {@link ProcessHandler} 与 mock 的 {@link RunContentDescriptor} 代替真实进程与运行窗口，
 * 直接实例化注册表（只用到存储逻辑，不依赖平台服务）。
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

    /** 运行窗口里内容仍在的 descriptor（未关闭） */
    private static RunContentDescriptor openDescriptor() {
        RunContentDescriptor descriptor = Mockito.mock(RunContentDescriptor.class);
        Mockito.when(descriptor.getAttachedContent()).thenReturn(Mockito.mock(Content.class));
        Mockito.when(descriptor.getExecutionConsole()).thenReturn(Mockito.mock(ExecutionConsole.class));
        return descriptor;
    }

    /** 内容已被关闭/复用的 descriptor：平台 dispose 后内容与控制台引用同时失效 */
    private static RunContentDescriptor closedDescriptor() {
        return Mockito.mock(RunContentDescriptor.class);
    }

    /** 内容尚未显示：内容引用为空，但控制台仍在 */
    private static RunContentDescriptor notShownDescriptor() {
        RunContentDescriptor descriptor = Mockito.mock(RunContentDescriptor.class);
        Mockito.when(descriptor.getExecutionConsole()).thenReturn(Mockito.mock(ExecutionConsole.class));
        return descriptor;
    }

    /** 结束的任务仍留在记录里，并带上退出码与耗时 */
    @Test
    public void finishedTaskIsRetainedWithExitCode() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        CmdTaskManager.CmdTask task = task(taskId, new FakeProcessHandler(), System.currentTimeMillis(), openDescriptor());
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
        CmdTaskManager.CmdTask task = task(taskId, new FakeProcessHandler(), System.currentTimeMillis(), openDescriptor());
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
        CmdTaskManager.CmdTask finished = task(registry.nextTaskId(), new FakeProcessHandler(), 1000L, openDescriptor());
        CmdTaskManager.CmdTask running = task(registry.nextTaskId(), new FakeProcessHandler(), 2000L, openDescriptor());
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
        CmdTaskManager.CmdTask running = task(registry.nextTaskId(), new FakeProcessHandler(), 0L, openDescriptor());
        registry.register(running);

        List<Long> finishedIds = new ArrayList<>();
        for (int i = 0; i < MAX_FINISHED_TASKS + 5; i++) {
            long taskId = registry.nextTaskId();
            registry.register(task(taskId, new FakeProcessHandler(), i, openDescriptor()));
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

    /** 运行窗口内容已关闭的已结束记录，在任务结束时被顺带淘汰 */
    @Test
    public void closedRunContentRecordIsEvicted() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        registry.register(task(taskId, new FakeProcessHandler(), System.currentTimeMillis(), closedDescriptor()));

        registry.markFinished(taskId, 0);

        assertNull(registry.find(taskId));
        assertTrue(registry.tasks().isEmpty());
    }

    /** 运行窗口内容还在的已结束记录必须保留（否则就失去了回看输出的来源） */
    @Test
    public void recordWithOpenRunContentIsRetained() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        registry.register(task(taskId, new FakeProcessHandler(), System.currentTimeMillis(), openDescriptor()));

        registry.markFinished(taskId, 0);

        assertNotNull(registry.find(taskId));
        assertEquals(1, registry.tasks().size());
    }

    /** 内容尚未显示（控制台仍在）时不能当作已关闭，避免误删刚结束任务的记录 */
    @Test
    public void recordNotYetShownIsRetained() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        registry.register(task(taskId, new FakeProcessHandler(), System.currentTimeMillis(), notShownDescriptor()));

        registry.markFinished(taskId, 0);

        assertNotNull(registry.find(taskId));
    }

    /** 没有关联运行内容的记录不做关闭淘汰，保守保留 */
    @Test
    public void recordWithoutRunContentIsRetained() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        registry.register(task(taskId, new FakeProcessHandler(), System.currentTimeMillis()));

        registry.markFinished(taskId, 0);

        assertNotNull(registry.find(taskId));
    }

    /** 新建任务时同样会顺带清理已关闭的记录 */
    @Test
    public void newTaskAlsoEvictsClosedRecords() {
        CmdTaskRegistry registry = registry();
        AtomicBoolean closed = new AtomicBoolean(false);
        Content content = Mockito.mock(Content.class);
        ExecutionConsole console = Mockito.mock(ExecutionConsole.class);
        RunContentDescriptor descriptor = Mockito.mock(RunContentDescriptor.class);
        Mockito.when(descriptor.getAttachedContent()).thenAnswer(invocation -> closed.get() ? null : content);
        Mockito.when(descriptor.getExecutionConsole()).thenAnswer(invocation -> closed.get() ? null : console);

        long firstId = registry.nextTaskId();
        registry.register(task(firstId, new FakeProcessHandler(), 1L, descriptor));
        registry.markFinished(firstId, 0);
        assertNotNull("窗口没关闭时记录必须保留", registry.find(firstId));

        closed.set(true);
        long secondId = registry.nextTaskId();
        registry.register(task(secondId, new FakeProcessHandler(), 2L, openDescriptor()));

        assertNull("新建任务时应顺带淘汰窗口已关闭的记录", registry.find(firstId));
        assertNotNull(registry.find(secondId));
    }

    /** 已结束的任务不能再终止 */
    @Test
    public void terminateIgnoresFinishedTask() {
        CmdTaskRegistry registry = registry();
        long taskId = registry.nextTaskId();
        FakeProcessHandler handler = new FakeProcessHandler();
        registry.register(task(taskId, handler, System.currentTimeMillis(), openDescriptor()));
        registry.markFinished(taskId, 0);

        assertFalse(registry.terminate(taskId));
        assertFalse(handler.destroyed);
        assertFalse(registry.terminate(taskId + 999));
    }
}
