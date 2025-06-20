package org.btik.espidf.toolwindow.kconfig.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lustre
 * @since 2025/6/14 14:54
 */
public enum KconfigType {
    BOOL("bool"),
    CHOICE("choice"),
    HEX("hex"),

    INT("int"),
    MENU("menu"),
    STRING("string");

    private final String nameStr;

    private static final Map<String, KconfigType> map = new HashMap<>();

    static {
        for (KconfigType value : values()) {
            map.put(value.nameStr, value);
        }
    }

    public KconfigType strValueOf(String str) {
        return map.get(str);
    }

    public String getNameStr() {
        return nameStr;
    }

    KconfigType(String nameStr) {
        this.nameStr = nameStr;
    }
}
