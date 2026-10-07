package org.btik.espidf.util;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 从 JetBrains「运行窗口」读取任务控制台输出。
 * <p>
 * RUN 工具窗口里的每个 tab 都对应一个 {@link RunContentDescriptor}：进程结束后只要 tab 没被关闭，
 * 运行内容与控制台文本仍然保留，这正是「已经结束但还留有运行窗口」的任务可以回看输出的原因。
 * 因此已结束任务的结果不需要插件再单独缓存一份，直接读取运行内容即可。
 * <p>
 * 控制台文本保存在编辑器文档里，读取需在 EDT 上进行（EDT 上自带读权限），
 * 后台线程（例如 MCP 工具调用）因此会切到 EDT 取一次快照。
 * tab 被关闭、控制台被释放时会返回 {@code null}，表示输出已不可用。
 */
public final class RunContentOutputs {

    private static final Logger LOG = Logger.getInstance(RunContentOutputs.class);

    private RunContentOutputs() {
    }

    /**
     * 该运行内容的输出当前是否可读。
     * <p>
     * 判据是控制台是否还在：运行内容被关闭（或被复用替换）时平台会 dispose 对应的
     * {@link RunContentDescriptor}，控制台引用随之失效，此后再也读不到文本。
     * 供调用方在读取前区分「有输出但为空」与「输出已不可用」。
     */
    public static boolean isOutputAvailable(@Nullable RunContentDescriptor descriptor) {
        return descriptor != null && descriptor.getExecutionConsole() != null;
    }

    /** 读取任务运行内容中保留的全部控制台文本；无可用内容时返回 {@code null}。 */
    public static @Nullable String readOutput(@Nullable RunContentDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }
        ExecutionConsole console;
        try {
            console = descriptor.getExecutionConsole();
        } catch (Throwable e) {
            LOG.info("run content console is not available: " + e.getMessage());
            return null;
        }
        if (!(console instanceof ConsoleViewImpl consoleView)) {
            return null;
        }
        return readText(consoleView);
    }

    private static @Nullable String readText(@NotNull ConsoleViewImpl consoleView) {
        Application application = ApplicationManager.getApplication();
        try {
            if (application.isDispatchThread()) {
                return consoleView.getText();
            }
            String[] holder = new String[1];
            application.invokeAndWait(() -> holder[0] = consoleView.getText(), ModalityState.any());
            return holder[0];
        } catch (Throwable e) {
            // tab 已关闭 / 控制台已释放：输出不可用，交由调用方按「无输出」处理
            LOG.info("console text of run content is not available: " + e.getMessage());
            return null;
        }
    }
}
