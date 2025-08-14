package org.btik.espidf.ui.componets;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class LongTextField extends JTextField {

    private long minValue = Long.MIN_VALUE;
    private long maxValue = Long.MAX_VALUE;


    public LongTextField() {
        this(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public LongTextField(int columns, long minValue, long maxValue) {
        super(columns);
        setRange(minValue, maxValue);
        setupDocumentFilter();
    }

    public LongTextField(long minValue, long maxValue) {
        this(10, minValue, maxValue); // 默认10列
    }

    public void setRange(long minValue, long maxValue) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue cannot greater than maxValue");
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
    }


    public long getLongValue() throws NumberFormatException {
        String text = getText().trim();
        if (text.isEmpty()) {
            throw new NumberFormatException("text is empty");
        }
        long value = Long.parseLong(text);
        if (value >= minValue && value <= maxValue) {
            return value;
        }
        throw new NumberFormatException("out of range");
    }


    public void setLongValue(long value) {
        if (value < minValue || value > maxValue) {
            throw new IllegalArgumentException(
                    String.format("value %d out of range [%d, %d]", value, minValue, maxValue));
        }
        setText(String.valueOf(value));
    }

    public long getMinValue() {
        return minValue;
    }

    public long getMaxValue() {
        return maxValue;
    }

    private void setupDocumentFilter() {
        AbstractDocument doc = (AbstractDocument) getDocument();
        doc.setDocumentFilter(new LongDocumentFilter());
    }

    private class LongDocumentFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String string,
                                 AttributeSet attr) throws BadLocationException {
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newText = currentText.substring(0, offset) + string + currentText.substring(offset);
            if (isValidInput(newText)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text,
                            AttributeSet attrs) throws BadLocationException {
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newText = currentText.substring(0, offset) + text + currentText.substring(offset + length);
            if (isValidInput(newText)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newText = currentText.substring(0, offset) + currentText.substring(offset + length);
            if (isValidInput(newText)) {
                super.remove(fb, offset, length);
            }
        }

        private boolean isValidInput(String text) {
            if (text.isEmpty()) {
                return true; // 允许空值
            }

            if (!text.matches("-?\\d*")) {
                return false;
            }

            if ("-".equals(text)) {
                return minValue < 0;
            }
            return true;
        }
    }
}
