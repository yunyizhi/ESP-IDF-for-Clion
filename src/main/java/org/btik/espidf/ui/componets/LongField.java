package org.btik.espidf.ui.componets;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LongField extends JBTextField {

    private final LongValueEditor myValueEditor;

    public LongField() {
        this(null, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public LongField(@Nullable String valueName, long minValue, long maxValue) {
        myValueEditor = new LongValueEditor(this, valueName, minValue);
        myValueEditor.setMinValue(minValue);
        myValueEditor.setMaxValue(maxValue);
    }

    public long getMinValue() {
        return myValueEditor.getMinValue();
    }

    public long getMaxValue() {
        return myValueEditor.getMaxValue();
    }

    public void setMinValue(long minValue) {
        myValueEditor.setMinValue(minValue);
    }

    public void setMaxValue(long maxValue) {
        myValueEditor.setMaxValue(maxValue);
    }

    @SuppressWarnings("unused") // Bean property
    public boolean isCanBeEmpty() {
        return myValueEditor.isCanBeEmpty();
    }

    public void setCanBeEmpty(boolean canBeEmpty) {
        myValueEditor.setCanBeEmpty(canBeEmpty);
    }

    public @NotNull Long getValue() {
        return myValueEditor.getValue();
    }

    public void setValue(@NotNull Long newValue) {
        myValueEditor.setValue(newValue);
    }

    public void setValueName(@Nullable String valueName) {
        myValueEditor.setValueName(valueName);
    }

    public @Nullable String getValueName() {
        return myValueEditor.getValueName();
    }

    public void validateContent() throws ConfigurationException {
        myValueEditor.validateContent();
    }

    public void setDefaultValueText(@NotNull @NlsContexts.StatusText String text) {
        getEmptyText().setText(text);
    }

    public void setDefaultValue(@NotNull Long defaultValue) {
        myValueEditor.setDefaultValue(defaultValue);
    }

    public @NotNull Long getDefaultValue() {
        return myValueEditor.getDefaultValue();
    }

    public void resetToDefault() {
        myValueEditor.setValue(myValueEditor.getDefaultValue());
    }

    public LongValueEditor getValueEditor() {
        return myValueEditor;
    }
}