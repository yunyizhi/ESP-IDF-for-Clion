package org.btik.espidf.util;

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
}
