package org.btik.espidf.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public static Map<String,String> diffWithSystem(Map<String,String> env) {
        Map<String, String> sysEnv = System.getenv();
        Map<String, String> resultEnv = new HashMap<>();
        env.forEach((key, value) ->{
            String sysValue = sysEnv.get(key);
            if(sysValue == null || Objects.equals(sysValue, value)){
                resultEnv.put(key, value);
            }
        });
        return resultEnv;
    }
}
