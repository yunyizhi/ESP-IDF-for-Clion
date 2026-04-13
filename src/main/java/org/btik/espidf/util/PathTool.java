package org.btik.espidf.util;

import java.io.File;
import java.nio.file.Path;

public class PathTool {
    public static Path normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return Path.of("");
        }

        Path resolvedPath = Path.of(path);

        if (path.startsWith("~")) {
            String userHome = System.getProperty("user.home");
            String remainingPath = path.substring(1);
            if (remainingPath.isEmpty() || remainingPath.startsWith(File.separator)) {
                resolvedPath = Path.of(userHome + remainingPath);
            } else {
                resolvedPath = Path.of(userHome, remainingPath);
            }
        }

        try {
            resolvedPath = resolvedPath.toAbsolutePath().normalize();
        } catch (Exception e) {
            resolvedPath = resolvedPath.toAbsolutePath();
        }

        return resolvedPath;
    }
}
