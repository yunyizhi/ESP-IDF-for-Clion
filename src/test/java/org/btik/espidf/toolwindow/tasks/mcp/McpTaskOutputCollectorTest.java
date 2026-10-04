package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

/**
 * {@link McpTaskOutputCollector#fetchFrom} 的行为约定：
 * 只采集「挂接之后」产生的输出，任务结束返回退出码，仍在运行则超时返回 -2。
 * <p>
 * 用假的 {@link ProcessHandler} 手工触发文本/结束事件，避免依赖真实进程。
 */
public class McpTaskOutputCollectorTest {

    /** 超时标记：调用结束时任务仍在运行 */
    private static final int STILL_RUNNING = -2;

    private static class FakeProcessHandler extends ProcessHandler {
        FakeProcessHandler() {
            // ProcessHandler 会把「终止事件 + 状态迁移」排队到 startNotify() 之后执行（真实进程由平台启动后调用），
            // 因此假 handler 需显式触发一次，否则终止事件永远不派发。
            startNotify();
        }

        @Override
        protected void destroyProcessImpl() {
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

        void emit(@NotNull String text) {
            notifyTextAvailable(text, ProcessOutputTypes.STDOUT);
        }

        void finish(int exitCode) {
            notifyProcessTerminated(exitCode);
        }
    }

    /** 只采集挂接之后产生的输出，任务结束后返回退出码 */
    @Test
    public void fetchFrom_collectsNewOutputUntilProcessExit() throws Exception {
        FakeProcessHandler handler = new FakeProcessHandler();
        Thread emitter = new Thread(() -> {
            sleep(100);
            handler.emit("line-1\n");
            handler.emit("line-2\n");
            handler.finish(0);
        });
        emitter.start();

        McpTaskOutputCollector.McpTaskOutput out = McpTaskOutputCollector.fetchFrom(handler, 5000);

        assertEquals(0, out.exitCode);
        assertEquals("line-1\nline-2\n", out.text);
        emitter.join();
    }

    /** 挂接前的历史输出不属于「本次调用窗口」，不能返回 */
    @Test
    public void fetchFrom_ignoresOutputProducedBeforeAttach() throws Exception {
        FakeProcessHandler handler = new FakeProcessHandler();
        handler.emit("before-attach\n");
        Thread emitter = new Thread(() -> {
            sleep(150);
            handler.emit("after-attach\n");
        });
        emitter.start();

        McpTaskOutputCollector.McpTaskOutput out = McpTaskOutputCollector.fetchFrom(handler, 600);

        assertEquals(STILL_RUNNING, out.exitCode);
        assertEquals("after-attach\n", out.text);
        emitter.join();
    }

    /** 任务已结束：直接返回退出码，没有可拉取的新输出 */
    @Test
    public void fetchFrom_alreadyTerminatedReturnsExitCode() {
        FakeProcessHandler handler = new FakeProcessHandler();
        handler.finish(7);

        McpTaskOutputCollector.McpTaskOutput out = McpTaskOutputCollector.fetchFrom(handler, 0);

        assertEquals(7, out.exitCode);
        assertEquals("", out.text);
    }

    /** 每次调用结束后摘除监听器，避免重复累积与泄漏 */
    @Test
    public void fetchFrom_detachesListenerAfterReturn() {
        FakeProcessHandler handler = new FakeProcessHandler();

        McpTaskOutputCollector.McpTaskOutput first = McpTaskOutputCollector.fetchFrom(handler, 50);
        assertEquals(STILL_RUNNING, first.exitCode);
        assertEquals("", first.text);

        handler.emit("late\n");
        McpTaskOutputCollector.McpTaskOutput second = McpTaskOutputCollector.fetchFrom(handler, 50);
        assertEquals(STILL_RUNNING, second.exitCode);
        assertEquals("", second.text);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
