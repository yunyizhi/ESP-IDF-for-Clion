package org.btik.espidf.util;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;

public interface ListCellRendererAttr {
    SimpleTextAttributes GRAY_ITALIC_SMALL_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER | SimpleTextAttributes.STYLE_ITALIC, JBColor.GRAY);
    SimpleTextAttributes RED_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED);
    SimpleTextAttributes BLUE_ITALIC_SMALL_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER | SimpleTextAttributes.STYLE_ITALIC, JBColor.BLUE);
}
