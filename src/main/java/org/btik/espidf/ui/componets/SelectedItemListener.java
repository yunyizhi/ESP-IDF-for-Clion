package org.btik.espidf.ui.componets;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.function.Consumer;

public class SelectedItemListener implements ItemListener {
    private final Consumer<ItemEvent> selectedHook;
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        selectedHook.accept(e);
    }

    public SelectedItemListener(@NotNull Consumer<ItemEvent> selectedHook) {
        this.selectedHook = selectedHook;
    }

    public static SelectedItemListener selectedListener(@NotNull Consumer<ItemEvent> selectedHook){
        return new SelectedItemListener(selectedHook);
    }
}
