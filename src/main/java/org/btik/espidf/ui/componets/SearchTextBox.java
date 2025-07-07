package org.btik.espidf.ui.componets;

import com.intellij.ui.SearchTextField;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyListener;

/**
 * @author lustre
 * @since 2025/7/7 22:57
 */
public class SearchTextBox extends SearchTextField {
    private Runnable clearCallback;

    public SearchTextBox() {
    }

    public SearchTextBox(boolean historyPopupEnabled) {
        super(historyPopupEnabled);
    }

    public SearchTextBox(@NonNls String historyPropertyName) {
        super(historyPropertyName);
    }

    public SearchTextBox(boolean historyPopupEnabled, @Nullable String historyPropertyName) {
        super(historyPopupEnabled, historyPropertyName);
    }

    public SearchTextBox(boolean historyPopupEnabled, boolean clearActionEnabled, @Nullable String historyPropertyName) {
        super(historyPopupEnabled, clearActionEnabled, historyPropertyName);
    }

    @Override
    protected void onFieldCleared() {
        if (clearCallback != null) {
            clearCallback.run();
        }
    }

    public SearchTextBox withClearCallback(Runnable clearCallback) {
        this.clearCallback = clearCallback;
        return this;
    }

    public SearchTextBox withKeyboardListener(final KeyListener listener) {
        addKeyboardListener(listener);
        return this;
    }


}
