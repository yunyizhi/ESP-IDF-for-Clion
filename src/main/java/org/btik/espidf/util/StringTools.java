package org.btik.espidf.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

/**
 * @author lustre
 * @since 2024/3/2 12:29
 */
public class StringTools {

    public static String safeNull(String string) {
        return string == null ? "" : string;
    }

    public static <T> String safe2String(T obj, @Nullable Function<T, String> converter) {
        if (obj == null) {
            return "";
        }
        if (converter == null) {
            return obj.toString();
        }
        return converter.apply(obj);
    }

    public static boolean appendNotEmpty(@NotNull  StringBuilder stringBuilder, String string) {
        if (StringUtils.isNotEmpty(string)) {
            stringBuilder.append(string);
            return true;
        }
        return false;
    }
}
