package org.btik.espidf.util;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.conf.IdfProjectConfig;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.service.IdfEnvironmentService.*;
import static org.btik.espidf.service.IdfProjectConfigService.PORT_CONF_AUTO;
import static org.btik.espidf.util.I18nMessage.$i18n;
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

    public static boolean checkIdfPyNotFound(String idfPyPath, Project project) {
        if (StringUtil.isNotEmpty(idfPyPath)) {
            return false;
        }
        I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("idf.py.not.found"),
                $i18n("idf.py.not.found.info"), NotificationType.ERROR).notify(project);
        return true;

    }

    public static String findIdfFullPath(Map<String, String> env) {
        String path = env.get("PATH");
        if (path == null) {
            path = env.get("Path");
        }
        String idfFullPath = findIdfFullPath(path);
        if (StringUtils.isEmpty(idfFullPath)) {
            String idfPath = env.get("IDF_PATH");
            Path idfPy = Path.of(idfPath, "tools", OsUtil.Const.IDF_EXE);
            File file = idfPy.toFile();
            if (file.exists()) {
                return safe2String(file, File::getPath);
            }
        }
        return idfFullPath;
    }

    public static String findIdfFullPath(String path) {
        File idfPyFile = PathEnvironmentVariableUtil.findInPath(OsUtil.getIdfExe(), path, null);
        if ((idfPyFile == null || !idfPyFile.exists()) && OsUtil.IS_WINDOWS) {
            idfPyFile = PathEnvironmentVariableUtil.findInPath(OsUtil.Const.IDF_EXE, path, null);
        }
        return safe2String(idfPyFile, File::getPath);
    }

    public static Map<String, String> getEnvsWithProjectSettings(@NotNull Project project) {
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        IdfProjectConfig projectConfig = project.getService(IdfProjectConfigService.class).getProjectConfig();
        Map<String, String> projectEnvs = buildProjectSettingToEnvs(projectConfig);
        environmentService.putTo(projectEnvs);
        return projectEnvs;
    }

    public static Map<String, String> buildProjectSettingToEnvs(@NotNull IdfProjectConfig projectConfig) {
        if (projectConfig.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> envs = new HashMap<>();
        String monitorBaud = projectConfig.getMonitorBaud();
        if (StringUtil.isNotEmpty(monitorBaud) && !monitorBaud.equals(PORT_CONF_AUTO)) {
            envs.put(IDF_MONITOR_BAUD, monitorBaud);
        }
        String uploadBaud = projectConfig.getUploadBaud();
        if (StringUtil.isNotEmpty(uploadBaud) && !uploadBaud.equals(PORT_CONF_AUTO)) {
            envs.put(ESP_BAUD, uploadBaud);
        }
        String port = projectConfig.getPort();
        if (StringUtil.isNotEmpty(port)) {
            envs.put(ESP_PORT, port);
        }
        return envs;
    }
}
