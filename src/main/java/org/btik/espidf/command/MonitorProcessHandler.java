package org.btik.espidf.command;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author lustre
 * @since 2025/7/19 22:45
 */
public class MonitorProcessHandler extends KillableColoredProcessHandler {
    private static final byte GROUP_SEPARATOR = 0x1d; // Ctrl+]
    private static final Logger LOG = Logger.getInstance(MonitorProcessHandler.class);
    private static final int MAX_TIMEOUT_MS = 5000;
    private static final int CHECK_INTERVAL_MS = 200;

    public MonitorProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
        super(commandLine);
    }

    @Override
    protected BaseOutputReader.@NotNull Options readerOptions() {
        return BaseOutputReader.Options.forMostlySilentProcess();
    }

    @Override
    protected boolean destroyProcessGracefully() {
        Process process = getProcess();
        boolean result = false;
        try (OutputStream outputStream = process.getOutputStream()) {
            outputStream.write(GROUP_SEPARATOR);
            outputStream.flush();


            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < MAX_TIMEOUT_MS) {
                if (!process.isAlive()) {
                    return true;
                }
                TimeUnit.MILLISECONDS.sleep(CHECK_INTERVAL_MS);
            }

            LOG.warn("Process did not exit within " + MAX_TIMEOUT_MS + "ms, forcing termination");
        } catch (IOException e) {
            LOG.error("Failed to send termination signal", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            LOG.warn("Thread interrupted during graceful shutdown");
        } finally {
            if (process.isAlive()) {
                result = super.destroyProcessGracefully();
            }
        }
        return result;
    }
}