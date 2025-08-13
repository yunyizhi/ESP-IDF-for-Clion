package org.btik.espidf.ui.componets;

import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.fields.valueEditors.TextFieldValueEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;

public class LongValueEditor extends TextFieldValueEditor<Long> {

    private long minValue;
    private long maxValue;
    private boolean canBeEmpty;

    public LongValueEditor(@NotNull JTextField field, @Nullable String valueName, @NotNull Long defaultValue) {
        super(field, valueName, defaultValue);
    }

    @Override
    public @NotNull Long parseValue(@Nullable String text) throws InvalidDataException {
        try {
            if (StringUtil.isEmpty(text)) {
                if (!canBeEmpty) {
                    throw new InvalidDataException($i18n("esp.idf.integer.field.value.not.a.number"));
                }
                return getDefaultValue();
            }
            long value = Long.parseLong(text);
            if (value < minValue || value > maxValue) {
                throw new InvalidDataException($i18nF("esp.idf.integer.field.value.out.of.range", value, minValue, maxValue));
            }
            return value;
        }
        catch (NumberFormatException nfe) {
            throw new InvalidDataException(($i18nF("esp.idf.integer.field.value.not.a.number", text)));
        }
    }

    @Override
    public String valueToString(@NotNull Long value) {
        if (canBeEmpty && value.equals(getDefaultValue())) {
            return "";
        }
        return String.valueOf(value);
    }

    @Override
    public boolean isValid(@NotNull Long value) {
        return value >= minValue && value <= maxValue;
    }

    public long getMinValue() {
        return minValue;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public void setMinValue(long minValue) {
        this.minValue = minValue;
    }

    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }

    public boolean isCanBeEmpty() {
        return canBeEmpty;
    }

    public void setCanBeEmpty(boolean canBeEmpty) {
        this.canBeEmpty = canBeEmpty;
    }

    @Override
    protected boolean isShowError(@Nullable String errorText) {
        if (StringUtil.isEmpty(getValueText())) {
            return false;
        }
        return super.isShowError(errorText);
    }
}
