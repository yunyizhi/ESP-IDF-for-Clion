package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCategory;
import com.intellij.mcpserver.McpToolDescriptor;
import com.intellij.mcpserver.McpToolSchema;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.coroutines.Continuation;
import kotlinx.serialization.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.toolwindow.settings.SerialPortLoader;
import org.btik.espidf.toolwindow.settings.model.SerialPortInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.I18nMessage.$i18nF;

/**
 * 列出当前机器上可用的串口，供 MCP 客户端为烧录 / 监视选择 ESPPORT。
 * 串口是机器级资源，与具体项目无关，故不需要 project 参数。
 */
public class EspIdfSerialPortsMcpTool implements McpTool {

    private static final McpToolCategory CATEGORY =
            new McpToolCategory("ESP-IDF Tasks", "esp.idf.tasks", false, false);

    private static final JsonObject EMPTY_JSON = new JsonObject(new LinkedHashMap<>());

    private final McpToolDescriptor descriptor;

    public EspIdfSerialPortsMcpTool() {
        McpToolSchema schema = McpToolSchema.Companion.ofPropertiesMap(
                new LinkedHashMap<>(), Set.of(), new LinkedHashMap<>(), McpToolSchema.DEFAULT_DEFINITIONS_PATH);
        this.descriptor = new McpToolDescriptor(
                "espidf_list_serial_ports",
                $i18n("espidf.mcp.serial.ports.name"),
                $i18n("espidf.mcp.serial.ports.desc"),
                CATEGORY,
                "espidf_list_serial_ports",
                schema,
                schema,
                new ToolAnnotations());
    }

    @Override
    public @NotNull McpToolDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Object call(@NotNull JsonObject input,
                       @Nullable Continuation<? super McpToolCallResult> continuation) {
        try {
            List<SerialPortInfo> ports = SerialPortLoader.getSerialPortInfo();
            StringBuilder sb = new StringBuilder();
            sb.append($i18n("espidf.mcp.serial.ports.header")).append("\n\n");
            if (ports.isEmpty()) {
                sb.append($i18n("espidf.mcp.serial.ports.empty")).append('\n');
                return McpToolCallResult.Companion.text(sb.toString(), EMPTY_JSON);
            }
            for (SerialPortInfo p : ports) {
                sb.append("- ").append(p.getComPort()).append('\n');
                appendLine(sb, "descriptivePortName", p.getDescriptivePortName());
                appendLine(sb, "portDescription", p.getPortDescription());
                if (p.getVendorId() != -1) {
                    appendLine(sb, "vendorId", String.format("0x%04x", p.getVendorId()));
                    if (p.getProductId() != -1) {
                        appendLine(sb, "productId", String.format("0x%04x", p.getProductId()));
                    }
                    appendLine(sb, "vendorName", p.getVendorName());
                    appendLine(sb, "productName", p.getProductName());
                }
            }
            return McpToolCallResult.Companion.text(sb.toString(), EMPTY_JSON);
        } catch (Throwable e) {
            return McpToolCallResult.Companion.error(
                    $i18nF("espidf.mcp.serial.ports.failed", e.getMessage()), EMPTY_JSON);
        }
    }

    private static void appendLine(StringBuilder sb, String key, String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        sb.append("    ").append(key).append('=').append(value).append('\n');
    }
}
