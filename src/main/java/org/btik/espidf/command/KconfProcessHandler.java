package org.btik.espidf.command;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author lustre
 * @since 2025/6/28 16:00
 */
public class KconfProcessHandler extends KillableColoredProcessHandler {
    private static final int MAX_TIMEOUT_MS = 3000;
    private static final int CHECK_INTERVAL_MS = 200;
    private static final Logger LOG = Logger.getInstance(KconfProcessHandler.class);

    public KconfProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
        super(commandLine);
    }

    protected KconfProcessHandler(@NotNull Process process, @NotNull GeneralCommandLine commandLine) {
        super(process, commandLine);
    }

    public KconfProcessHandler(@NotNull Process process, String commandLine) {
        super(process, commandLine);
    }

    public KconfProcessHandler(@NotNull Process process, String commandLine, @NotNull Charset charset) {
        super(process, commandLine, charset);
    }

    public KconfProcessHandler(@NotNull Process process, String commandLine, @NotNull Charset charset, @Nullable Set<File> filesToDelete) {
        super(process, commandLine, charset, filesToDelete);
    }

    @Override
    protected BaseOutputReader.@NotNull Options readerOptions() {
        return BaseOutputReader.Options.forMostlySilentProcess();
    }

    @Override
    protected boolean destroyProcessGracefully() {
        boolean result = false;
        try {
            Process process = getProcess();
            process.getOutputStream().close();
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < MAX_TIMEOUT_MS) {
                if (!process.isAlive()) {
                    return true;
                }
                TimeUnit.MILLISECONDS.sleep(CHECK_INTERVAL_MS);
            }
            LOG.warn("Process did not exit within " + MAX_TIMEOUT_MS + "ms, forcing termination");
            return true;
        } catch (IOException e) {
            LOG.error(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            LOG.warn("Thread interrupted during graceful shutdown");
        } finally {
            if (getProcess().isAlive()) {
                result = super.destroyProcessGracefully();
            }
        }
        return result;
    }
}
