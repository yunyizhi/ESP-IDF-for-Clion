package org.btik.espidf.util;

/**
 * @author lustre
 * @since 2024/3/2 12:29
 */
public class StringUtils {

    public static String safeNull(String string) {
        return string == null ? "null" : string;
    }
}
