package org.btik.espidf.toolwindow.tasks.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义任务 XML 中的变量组 Profile 集合：匿名 profile（全局导入）与命名 profile。
 * <p>
 * 合并优先级（低 → 高）：匿名 profile → 命名 profile（按命令 {@code profile} 属性书写顺序，后者覆盖）→ 命令内联变量。
 *
 * @author lustre
 */
public final class Profiles {

    private final Map<String, String> globalEnv = new LinkedHashMap<>();
    private final Map<String, String> globalMacros = new LinkedHashMap<>();
    private final Map<String, Profile> named = new LinkedHashMap<>();

    public record Profile(Map<String, String> env, Map<String, String> macros) {
    }

    /** 加入一个匿名 profile：其 env/宏对所有任务全局生效。 */
    public void addGlobal(Map<String, String> env, Map<String, String> macros) {
        if (env != null) {
            globalEnv.putAll(env);
        }
        if (macros != null) {
            globalMacros.putAll(macros);
        }
    }

    /** 加入一个 profile；{@code name} 为空视为匿名。 */
    public void addNamed(String name, Map<String, String> env, Map<String, String> macros) {
        if (name == null || name.isBlank()) {
            addGlobal(env, macros);
            return;
        }
        named.put(name, new Profile(env == null ? Map.of() : env, macros == null ? Map.of() : macros));
    }

    /** 合并环境变量：匿名 → 命名（按引用顺序，后者覆盖）→ 内联（最高优先级）。 */
    public Map<String, String> resolveEnv(String profileAttr, Map<String, String> inlineEnv) {
        Map<String, String> result = new LinkedHashMap<>(globalEnv);
        for (String name : split(profileAttr)) {
            Profile profile = named.get(name);
            if (profile != null) {
                result.putAll(profile.env());
            }
        }
        if (inlineEnv != null) {
            result.putAll(inlineEnv);
        }
        return result;
    }

    /** 合并宏：匿名 → 命名（按引用顺序，后者覆盖）。 */
    public Map<String, String> resolveMacros(String profileAttr) {
        Map<String, String> result = new LinkedHashMap<>(globalMacros);
        for (String name : split(profileAttr)) {
            Profile profile = named.get(name);
            if (profile != null) {
                result.putAll(profile.macros());
            }
        }
        return result;
    }

    private static String[] split(String profileAttr) {
        if (profileAttr == null || profileAttr.isBlank()) {
            return new String[0];
        }
        return profileAttr.trim().split("\\s+");
    }
}
