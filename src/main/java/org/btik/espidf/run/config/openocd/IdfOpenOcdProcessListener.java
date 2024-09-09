package org.btik.espidf.run.config.openocd;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import org.jetbrains.annotations.NotNull;

public class IdfOpenOcdProcessListener implements ProcessListener {

    private ProcessHandler processHandler;

    @Override
    public void startNotified(@NotNull ProcessEvent event) {
        this.processHandler = event.getProcessHandler();
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        processHandler.removeProcessListener(this);
        this.processHandler = null;
    }

    public boolean isAlive() {
        if (processHandler == null) {
            return false;
        }
        if (processHandler instanceof OSProcessHandler) {
            return ((OSProcessHandler) processHandler).getProcess().isAlive();
        }
        return processHandler.getExitCode() != null;
    }

    public void destroy() {
        if (!isAlive()) {
            return;
        }
        processHandler.destroyProcess();
    }
}
