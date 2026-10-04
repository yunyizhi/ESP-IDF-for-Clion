package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import org.btik.espidf.command.ProcessEventAdaptor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 旁路收集 ESP-IDF 任务通过 MCP 触发时的输出。
 * 监听器在任务进程上挂接，将 stdout/stderr 文本按 {@code 项目路径::任务id} 累积到 map，
 * MCP 工具调用 {@link #await} 阻塞获取结果后返回，不侵入执行器本身的同步逻辑。
 */
public final class McpTaskOutputCollector {

    public static final class McpTaskOutput {
        public final String text;
        public final int exitCode;

        public McpTaskOutput(String text, int exitCode) {
            this.text = text;
            this.exitCode = exitCode;
        }
    }

    private static final class Entry {
        /** stdout/stderr 由不同线程回调，用 StringBuffer 保证写入线程安全 */
        final StringBuffer sb = new StringBuffer();
        final CompletableFuture<McpTaskOutput> future = new CompletableFuture<>();
    }

    private static final Map<String, Entry> REGISTRY = new ConcurrentHashMap<>();

    private McpTaskOutputCollector() {
    }

    /**
     * 为指定 key 注册一个输出收集监听器。
     */
    public static ProcessListener register(@NotNull String key) {
        Entry entry = new Entry();
        REGISTRY.put(key, entry);
        return new ProcessEventAdaptor()
                .withOnTextAvailableCb((event, type) -> entry.sb.append(event.getText()))
                .withProcessTerminatedCb(event -> complete(entry, key, event.getExitCode()))
                .withProcessNotStartedCb(() -> {
                    if (!entry.future.isDone()) {
                        complete(entry, key, -1);
                    }
                });
    }

    private static void complete(Entry entry, String key, int exitCode) {
        if (entry.future.complete(new McpTaskOutput(entry.sb.toString(), exitCode))) {
            REGISTRY.remove(key);
        }
    }

    /**
     * 从「当前时刻」起采集一个<b>已在运行</b>任务的增量输出：把监听器挂到现有进程上，
     * 等待任务结束或超时后摘除监听器。
     * <p>
     * 与 {@link #register} 的区别：不注册 key、不接管任务结束，只返回本次调用窗口内新增的输出，
     * 供 MCP 的「拉取输出」工具轮询长任务日志。
     *
     * @param timeoutMillis 最长等待毫秒数，0 表示立即返回当前已到达的输出
     * @return 窗口内新增的输出；{@code exitCode == -2} 表示等待超时任务仍在运行，
     * {@code exitCode == -3} 表示采集过程异常
     */
    public static @NotNull McpTaskOutput fetchFrom(@NotNull ProcessHandler processHandler, long timeoutMillis) {
        StringBuffer sb = new StringBuffer();
        CompletableFuture<Integer> terminated = new CompletableFuture<>();
        ProcessListener listener = new ProcessEventAdaptor()
                .withOnTextAvailableCb((event, type) -> sb.append(event.getText()))
                .withProcessTerminatedCb(event -> terminated.complete(event.getExitCode()))
                .withProcessNotStartedCb(() -> terminated.complete(-1));
        processHandler.addProcessListener(listener);
        if (processHandler.isProcessTerminated()) {
            // 极窄窗口内任务已结束：补一次结束事件，避免白等整个超时
            Integer exitCode = processHandler.getExitCode();
            terminated.complete(exitCode == null ? -1 : exitCode);
        }
        int exitCode;
        try {
            exitCode = terminated.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            exitCode = -2;
        } catch (Exception e) {
            exitCode = -3;
        } finally {
            processHandler.removeProcessListener(listener);
        }
        return new McpTaskOutput(sb.toString(), exitCode);
    }

    /**
     * 等待指定 key 的任务输出就绪。
     *
     * @return 输出结果；超时返回 {@code exitCode == -2} 的部分输出；key 不存在返回 null
     */
    public static McpTaskOutput await(@NotNull String key, long timeoutMillis) {
        Entry entry = REGISTRY.get(key);
        if (entry == null) {
            return null;
        }
        try {
            return entry.future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return new McpTaskOutput(entry.sb.toString(), -2);
        } catch (Exception e) {
            return new McpTaskOutput(entry.sb.toString(), -3);
        }
    }
}
