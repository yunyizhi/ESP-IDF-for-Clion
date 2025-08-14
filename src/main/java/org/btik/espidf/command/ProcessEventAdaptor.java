package org.btik.espidf.command;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ProcessEventAdaptor implements ProcessListener {
    private Consumer<@NotNull ProcessEvent> startNotifiedCb;
    private Consumer<@NotNull ProcessEvent> processTerminatedCb;
    private BiConsumer<@NotNull ProcessEvent, Boolean> processWillTerminateCb;
    private BiConsumer<@NotNull ProcessEvent, @NotNull Key<?>> onTextAvailableCb;
    private Runnable processNotStartedCb;

    @Override
    public void startNotified(@NotNull ProcessEvent event) {
        if (startNotifiedCb != null) {
            startNotifiedCb.accept(event);
        }
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        if (processTerminatedCb != null) {
            processTerminatedCb.accept(event);
        }
    }

    @Override
    public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
        if (processWillTerminateCb != null) {
            processWillTerminateCb.accept(event, willBeDestroyed);
        }
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        if (onTextAvailableCb != null) {
            onTextAvailableCb.accept(event, outputType);
        }
    }

    @Override
    public void processNotStarted() {
        if (processNotStartedCb != null) {
            processNotStartedCb.run();
        }
    }

    public ProcessEventAdaptor withStartNotifiedCb(Consumer<@NotNull ProcessEvent> startNotifiedCb) {
        this.startNotifiedCb = startNotifiedCb;
        return this;
    }

    public ProcessEventAdaptor withProcessTerminatedCb(Consumer<@NotNull ProcessEvent> processTerminatedCb) {
        this.processTerminatedCb = processTerminatedCb;
        return this;
    }

    public ProcessEventAdaptor withProcessWillTerminateCb(BiConsumer<@NotNull ProcessEvent, Boolean> processWillTerminateCb) {
        this.processWillTerminateCb = processWillTerminateCb;
        return this;
    }

    public ProcessEventAdaptor withOnTextAvailableCb(BiConsumer<@NotNull ProcessEvent, @NotNull Key<?>> onTextAvailableCb) {
        this.onTextAvailableCb = onTextAvailableCb;
        return this;
    }

    public ProcessEventAdaptor withProcessNotStartedCb(Runnable processNotStartedCb) {
        this.processNotStartedCb = processNotStartedCb;
        return this;
    }
}
