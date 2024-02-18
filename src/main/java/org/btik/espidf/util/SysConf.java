package org.btik.espidf.util;

import com.intellij.DynamicBundle;
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
        return INSTANCE.getResourceBundle().getString(key);
    }

    public static String $sys(String key){
        return INSTANCE.getResourceBundle().getString(key);
    }

    public static String getF(String key, @NotNull Object... params) {
        return String.format(get(key), params);
    }


}
