package org.btik.espidf.util;

import com.intellij.DynamicBundle;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author lustre
 * @since 2022/10/22 20:48
 */
public class SysConf extends DynamicBundle {
    private static final SysConf INSTANCE = new SysConf("org-btik-esp-idf.conf.systemConf");

    private SysConf(@NotNull String pathToBundle) {
        super(pathToBundle);
    }

    public static String get(String key) {
        return INSTANCE.messageOrDefault(key, key);
    }

    public static String $sys(String key) {
        return INSTANCE.messageOrDefault(key, key);
    }

    public static String getF(String key, @NotNull Object... params) {
        return String.format(get(key), params);
    }

    public static int getInt(String key, int defaultValue) {

        String intStr = INSTANCE.messageOrDefault(key, "");
        if (StringUtil.isEmpty(intStr)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(intStr);
        }catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
