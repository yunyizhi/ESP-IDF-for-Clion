package org.btik.espidf.util;

import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class XmlPsiTool {

    /**
    * @param xmlTag 父标签
     * @param tagName 子标签名称
     * @param keepFirstNewline 是否在首行添加换行符
    *
    * */
    public static String getSubTagTrimmedText(@NotNull XmlTag xmlTag, @NotNull String tagName, boolean keepFirstNewline) {
        XmlTag subTag = xmlTag.findFirstSubTag(tagName);
        if (subTag == null) {
            return null;
        }
        String trimmedText = subTag.getValue().getTrimmedText();
        if (keepFirstNewline && StringUtils.isNotEmpty(trimmedText)) {
            return System.lineSeparator() + trimmedText;
        }
        return trimmedText;
    }

    public static String getAttribute(@NotNull XmlTag xmlTag,@NotNull String name) {
        XmlAttribute value = xmlTag.getAttribute(name);
        if (value == null) {
            return null;
        }
        return value.getDisplayValue();
    }

    public static boolean getBoolAttribute(@NotNull XmlTag xmlTag,@NotNull String name) {
        return getBoolAttribute(xmlTag, name, false);
    }

    public static boolean getBoolAttribute(@NotNull XmlTag xmlTag, @NotNull String name, boolean defaultValue) {
        XmlAttribute value = xmlTag.getAttribute(name);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.getValue());
    }
}
