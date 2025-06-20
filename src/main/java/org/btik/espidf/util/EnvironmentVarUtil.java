package org.btik.espidf.util;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.util.StringTools.safe2String;

/**
 * @author lustre
 * @since 2024/2/13 16:32
 */
public class EnvironmentVarUtil {
    public static Map<String, String> parseEnv(String text) {
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

    public static Map<String, String> diffWithSystem(Map<String, String> env) {
        Map<String, String> sysEnv = System.getenv();
        Map<String, String> resultEnv = new HashMap<>();
        env.forEach((key, value) -> {
            String sysValue = sysEnv.get(key);
            if (sysValue == null || !Objects.equals(sysValue, value)) {
                resultEnv.put(key, value);
            }
        });
        return resultEnv;
    }
    public static String findIdfFullPath(Map<String, String> env) {
        String path = env.get("PATH");
        if (path == null) {
            path = env.get("Path");
        }
        return findIdfFullPath(path);
    }

    public static String findIdfFullPath(String path) {
        File idfPyFile = PathEnvironmentVariableUtil.findInPath(OsUtil.getIdfExe(), path, null);
        if ((idfPyFile == null || !idfPyFile.exists()) && OsUtil.IS_WINDOWS) {
            idfPyFile = PathEnvironmentVariableUtil.findInPath(OsUtil.Const.IDF_EXE, path, null);
        }
        return safe2String(idfPyFile, File::getPath);
    }
}
