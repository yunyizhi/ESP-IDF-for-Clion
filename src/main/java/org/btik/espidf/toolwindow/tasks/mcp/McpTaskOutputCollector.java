package org.btik.espidf.toolwindow.tasks.mcp;

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
        final StringBuilder sb = new StringBuilder();
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
