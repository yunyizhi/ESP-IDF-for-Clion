package org.btik.espidf.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lustre
 * @since 2024/2/13 16:32
 */
public class EnvironmentVarUtil {
    public static Map<String,String> parseEnv(String text) {
        String[] lines = text.split("\n");
        var env = new HashMap<String, String>();
        for (String line : lines) {
            if (!line.isEmpty()) {
                int pos = line.indexOf('=');
                if (pos <= 0) {
                    throw new RuntimeException("malformed:" + line);
                }
                env.put(line.substring(0, pos), line.substring(pos + 1));
            }
        }
        return env;
    }
}
