package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpCallInfoKt;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolSchema;
import com.intellij.openapi.project.Project;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 所有 MCP 工具的公共工具类：统一构建输入/输出 {@link McpToolSchema}，以及读取入参、解析 Project。
 * <p>
 * 工具侧只需通过 {@link #schema()} 声明「参数名称、类型、描述、默认值（可为空）」，
 * 由本类拼装成 {@link McpToolSchema}，无需再重复书写 JSON 属性结构与
 * {@code ofPropertiesMap} 样板代码。
 */
public final class McpSchemaUtils {

    /** 通用空 JsonObject，用于无结构化输出的返回。 */
    public static final JsonObject EMPTY_JSON = new JsonObject(new LinkedHashMap<>());

    private McpSchemaUtils() {
    }

    /** 开始构建一个 Schema。 */
    public static SchemaBuilder schema() {
        return new SchemaBuilder();
    }

    /** 构建无参（属性为空）的 Schema。 */
    public static McpToolSchema emptySchema() {
        return schema().build();
    }

    // ---------------- 类型属性 ----------------

    public static JsonElement stringProperty(String description) {
        return stringProperty(description, null);
    }

    public static JsonElement stringProperty(String description, @Nullable String defaultValue) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("type", JsonElementKt.JsonPrimitive("string"));
        m.put("description", JsonElementKt.JsonPrimitive(description));
        if (defaultValue != null) {
            m.put("default", JsonElementKt.JsonPrimitive(defaultValue));
        }
        return new JsonObject(m);
    }

    public static JsonElement integerProperty(String description) {
        return integerProperty(description, null);
    }

    public static JsonElement integerProperty(String description, @Nullable Number defaultValue) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("type", JsonElementKt.JsonPrimitive("integer"));
        m.put("description", JsonElementKt.JsonPrimitive(description));
        if (defaultValue != null) {
            m.put("default", JsonElementKt.JsonPrimitive(defaultValue));
        }
        return new JsonObject(m);
    }

    public static JsonElement booleanProperty(String description) {
        return booleanProperty(description, null);
    }

    public static JsonElement booleanProperty(String description, @Nullable Boolean defaultValue) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("type", JsonElementKt.JsonPrimitive("boolean"));
        m.put("description", JsonElementKt.JsonPrimitive(description));
        if (defaultValue != null) {
            m.put("default", JsonElementKt.JsonPrimitive(defaultValue));
        }
        return new JsonObject(m);
    }

    public static JsonElement arrayProperty(String description) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        m.put("type", JsonElementKt.JsonPrimitive("array"));
        Map<String, JsonElement> items = new LinkedHashMap<>();
        items.put("type", JsonElementKt.JsonPrimitive("object"));
        m.put("items", new JsonObject(items));
        m.put("description", JsonElementKt.JsonPrimitive(description));
        return new JsonObject(m);
    }

    // ---------------- 入参读取 ----------------

    /** 解析当前 MCP 调用上下文中的 Project，无则返回 {@code null}。 */
    public static @Nullable Project resolveProject(@Nullable Continuation<? super McpToolCallResult> continuation) {
        return McpCallInfoKt.getProjectOrNull(
                continuation != null ? continuation.getContext()
                        : kotlin.coroutines.EmptyCoroutineContext.INSTANCE);
    }

    public static @Nullable String readString(@NotNull JsonObject input, @NotNull String key) {
        JsonElement e = input.get(key);
        if (!(e instanceof JsonPrimitive p) || !p.isString()) {
            return null;
        }
        return p.getContent();
    }

    public static @Nullable Integer readInt(@NotNull JsonObject input, @NotNull String key) {
        JsonElement e = input.get(key);
        if (!(e instanceof JsonPrimitive p)) {
            return null;
        }
        String content = p.getContent();
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(content.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static @Nullable Long readLong(@NotNull JsonObject input, @NotNull String key) {
        JsonElement e = input.get(key);
        if (!(e instanceof JsonPrimitive p)) {
            return null;
        }
        try {
            return Long.parseLong(p.getContent().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean readBool(@NotNull JsonObject input, @NotNull String key) {
        JsonElement e = input.get(key);
        if (!(e instanceof JsonPrimitive p)) {
            return false;
        }
        return Boolean.parseBoolean(p.getContent().trim());
    }

    /**
     * 声明式 Schema 构建器：调用方只关注参数名称、类型、描述、默认值（可为空），
     * 无需关心底层 JSON 结构与 {@code ofPropertiesMap} 参数。
     */
    public static final class SchemaBuilder {

        private final Map<String, JsonElement> properties = new LinkedHashMap<>();
        private final Set<String> required = new LinkedHashSet<>();

        private SchemaBuilder() {
        }

        public SchemaBuilder string(String name, String description) {
            return string(name, description, null);
        }

        public SchemaBuilder string(String name, String description, @Nullable String defaultValue) {
            properties.put(name, stringProperty(description, defaultValue));
            return this;
        }

        public SchemaBuilder integer(String name, String description) {
            return integer(name, description, null);
        }

        public SchemaBuilder integer(String name, String description, @Nullable Number defaultValue) {
            properties.put(name, integerProperty(description, defaultValue));
            return this;
        }

        public SchemaBuilder bool(String name, String description) {
            return bool(name, description, null);
        }

        public SchemaBuilder bool(String name, String description, @Nullable Boolean defaultValue) {
            properties.put(name, booleanProperty(description, defaultValue));
            return this;
        }

        public SchemaBuilder array(String name, String description) {
            properties.put(name, arrayProperty(description));
            return this;
        }

        /** 追加已构造好的属性（特殊类型时使用）。 */
        public SchemaBuilder property(String name, JsonElement property) {
            properties.put(name, property);
            return this;
        }

        /** 标记必填参数。 */
        public SchemaBuilder required(String... names) {
            for (String name : names) {
                if (name != null) {
                    required.add(name);
                }
            }
            return this;
        }

        public McpToolSchema build() {
            return McpToolSchema.Companion.ofPropertiesMap(
                    properties, Set.copyOf(required), new LinkedHashMap<>(), McpToolSchema.DEFAULT_DEFINITIONS_PATH);
        }
    }
}
