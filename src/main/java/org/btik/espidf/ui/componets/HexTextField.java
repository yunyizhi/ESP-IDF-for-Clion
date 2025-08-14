package org.btik.espidf.ui.componets;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class HexTextField extends JTextField {

    public HexTextField() {
        this(10);
    }

    public HexTextField(int columns) {
        super(columns);
        setupDocumentFilter();
    }

    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }
        String text = getText();
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("0[xX][0-9a-fA-F]+");
    }


    private void setupDocumentFilter() {
        AbstractDocument doc = (AbstractDocument) getDocument();
        doc.setDocumentFilter(new HexDocumentFilter());
    }

    private static class HexDocumentFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String string,
                                 AttributeSet attr) throws BadLocationException {
            // 获取当前文本
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());

            String allowedChars = "0123456789abcdefABCDEFxX";
            StringBuilder validInsert = new StringBuilder();
            for (char c : string.toCharArray()) {
                if (allowedChars.indexOf(c) != -1) {
                    validInsert.append(c);
                }
            }

            if (!validInsert.isEmpty()) {
                String newText = currentText.substring(0, offset) + validInsert + currentText.substring(offset);
                if (isValidFormat(newText)) {
                    super.insertString(fb, offset, validInsert.toString(), attr);
                }
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text,
                            AttributeSet attrs) throws BadLocationException {
            // 与 insertString 逻辑类似
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String allowedChars = "0123456789abcdefABCDEFxX";
            StringBuilder validReplace = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (allowedChars.indexOf(c) != -1) {
                    validReplace.append(c);
                }
            }

            if (!validReplace.isEmpty()) {
                String newText = currentText.substring(0, offset) + validReplace + currentText.substring(offset + length);
                if (isValidFormat(newText)) {
                    super.replace(fb, offset, length, validReplace.toString(), attrs);
                }
            } else {
                String newText = currentText.substring(0, offset) + currentText.substring(offset + length);
                if (isValidFormat(newText)) {
                    super.remove(fb, offset, length);
                }
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newText = currentText.substring(0, offset) + currentText.substring(offset + length);
            if (isValidFormat(newText)) {
                super.remove(fb, offset, length);
            }
        }

        private boolean isValidFormat(String text) {
            if (text.isEmpty()) {
                return true;
            }

            return text.matches("(0[xX])?[0-9a-fA-F]*");
        }
    }
}
