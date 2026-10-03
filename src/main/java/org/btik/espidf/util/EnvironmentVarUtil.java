package org.btik.espidf.util;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.intellij.platform.ide.progress.TaskCancellation;
import com.intellij.platform.ide.progress.TasksKt;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.btik.espidf.service.IdfEnvironmentService.*;
import static org.btik.espidf.service.IdfProjectConfigService.PORT_CONF_AUTO;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.util.StringTools.safe2String;

/**
 * @author lustre
 * @since 2024/2/13 16:32
 */
public class EnvironmentVarUtil {

    /** 拓展变量占位符：${env:key} 取环境变量，${v:key} 取替换宏 */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(env|v):([^}]*)}");

    public static Map<String, String> parseEnv(String text) {
        var env = new HashMap<String, String>();
        for (String rawLine : text.split("\r?\n")) {
            // 子标签内容通常带缩进，逐行 trim 并跳过空行，避免缩进污染变量名/空行触发解析失败
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            int pos = line.indexOf('=');
            if (pos <= 0) {
                throw new RuntimeException("malformed:" + rawLine);
            }
            env.put(line.substring(0, pos).trim(), line.substring(pos + 1));
        }
        return env;
    }

    /**
     * 展开文本中的 {@code ${env:key}} 与 {@code ${v:key}} 占位符：
     * {@code env} 从环境变量表取值，{@code v} 从宏表取值；未命中或 key 为空时替换为空串。
     * 其余形如 {@code ${other:...}} 或没有闭合大括号的内容保持不变。
     *
     * @param text   待展开文本，为 {@code null} 时返回 {@code null}
     * @param env    环境变量表，可为 {@code null}
     * @param macros 宏表，可为 {@code null}
     */
    public static String substitute(String text, Map<String, String> env, Map<String, String> macros) {
        return replacePlaceholders(text, env, macros, PlaceholderMode.FULL);
    }

    /**
     * 仅展开当前已知的占位符：{@code ${env:key}} 未命中时**保留原样**，供执行期用任务的真实运行环境再展开；
     * {@code ${v:key}} 未命中仍替换为空串（宏在解析期已全部确定）。
     *
     * @param text   待展开文本，为 {@code null} 时返回 {@code null}
     * @param env    已知环境变量表（profile + 内联），可为 {@code null}
     * @param macros 宏表，可为 {@code null}
     */
    public static String substitutePartial(String text, Map<String, String> env, Map<String, String> macros) {
        return replacePlaceholders(text, env, macros, PlaceholderMode.PARTIAL);
    }

    /** 占位符替换策略 */
    private enum PlaceholderMode {
        /** 全量替换：未命中（含宏）替换为空串 */
        FULL,
        /** 解析期：未命中的 {@code ${env:}} 保留占位符待执行期展开，未命中宏替换为空串 */
        PARTIAL,
        /** 仅处理 {@code ${env:}}（未命中替换为空串）；{@code ${v:}} 等一律原样保留 */
        ENV_ONLY
    }

    private static String replacePlaceholders(String text, Map<String, String> env, Map<String, String> macros,
                                              PlaceholderMode mode) {
        if (text == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            boolean fromEnv = "env".equals(matcher.group(1));
            if (!fromEnv && mode == PlaceholderMode.ENV_ONLY) {
                // 环境变量值不处理宏：原样保留，明确表示该处不做解析
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            String key = matcher.group(2).trim();
            Map<String, String> source = fromEnv ? env : macros;
            String value = (source == null || key.isEmpty()) ? null : source.get(key);
            String replacement;
            if (value != null) {
                replacement = value;
            } else if (mode == PlaceholderMode.PARTIAL && fromEnv) {
                // 环境变量执行期才确定，保留占位符交给执行器用最终环境展开
                replacement = matcher.group();
            } else {
                replacement = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 展开拓展环境变量的值：用基础环境展开值里的 {@code ${env:key}}，
     * 以支持 {@code PATH=/xxx:${env:PATH}} 这类追加写法（引用的是被本任务覆盖前的原值）。
     * <p>
     * 环境变量值**只处理 {@code ${env:key}}**：未命中替换为空串；{@code ${v:key}} 等宏在此**不解析、原样保留**，
     * 以免出现“看似替换了但结果是错的”的困惑。
     *
     * @param extra 待展开的拓展环境变量，可为 {@code null}
     * @param base  基础环境（尚未叠加 extra），用于解析其中的 {@code ${env:key}}
     */
    public static Map<String, String> resolveEnvValues(Map<String, String> extra, Map<String, String> base) {
        if (extra == null || extra.isEmpty()) {
            return Map.of();
        }
        Map<String, String> resolved = new HashMap<>(extra.size());
        extra.forEach((key, value) -> resolved.put(key, replacePlaceholders(value, base, null, PlaceholderMode.ENV_ONLY)));
        return resolved;
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
            if (StringUtils.isEmpty(idfPath)) {
                return null;
            }
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

    public static String findGitFullPath(Map<String, String> env) {
        String path = env.get("PATH");
        if (path == null) {
            path = env.get("Path");
        }
        File gitFile = PathEnvironmentVariableUtil.findInPath(OsUtil.getGitExe(), path, null);
        return safe2String(gitFile, File::getPath);
    }

    public static String getIdfVersionByIdfPy(Map<String, String> env, ModalTaskOwner owner, String toolchainName) {
        String idfPy = findIdfFullPath(env);
        if (StringUtils.isEmpty(idfPy)) {
            return null;
        }
        GeneralCommandLine readVersion = new GeneralCommandLine()
                .withEnvironment(env)
                .withExePath(idfPy)
                .withParameters("--version");
        return TasksKt.runWithModalProgressBlocking(owner, $i18nF("esp.idf.read.version", toolchainName),
                TaskCancellation.nonCancellable(), (scope, continuation) ->
                        CmdTaskExecutor.exeGetStdOut(readVersion, 60 * 1000));
    }

    public static String getIdfVersionByGit(Map<String, String> env, ModalTaskOwner owner, String toolchainName) {
        String git = findGitFullPath(env);
        if (StringUtils.isEmpty(git)) {
            return null;
        }
        String idfPath = env.get(IDF_PATH);
        GeneralCommandLine readVersion = new GeneralCommandLine()
                .withEnvironment(env)
                .withExePath(git)
                .withParameters("--git-dir", Path.of(idfPath, ".git").toString())
                .withParameters("--work-tree", idfPath)
                .withParameters("describe", "--tags", "--dirty", "--match", "v*.*");
        return TasksKt.runWithModalProgressBlocking(owner, $i18nF("esp.idf.read.version", toolchainName),
                TaskCancellation.nonCancellable(), (scope, continuation) ->
                        CmdTaskExecutor.exeGetStdOut(readVersion, 60 * 1000));
    }

    public static String getIdfVersion(Map<String, String> env, ModalTaskOwner owner, String toolchainName) {
        String version = getIdfVersionByGit(env, owner, toolchainName);
        if (StringUtils.isNotEmpty(version)) {
            return version;
        }
        version = getIdfVersionByIdfPy(env, owner, toolchainName);
        if (StringUtils.isEmpty(version)) {
            version = "idf" + env.get(ESP_IDF_VERSION);
        }
        return version;
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
