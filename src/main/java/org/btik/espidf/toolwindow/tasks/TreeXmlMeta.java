package org.btik.espidf.toolwindow.tasks;

/**
 * @author lustre
 * @since 2024/2/18 15:17
 */
public interface TreeXmlMeta {
    String TREE_ROOT = "tree";

    String FOLDER_TAG = "folder";
    // attrs
    String ID = "id";
    String NAME = "name";

    String VALUE = "value";

    String CONSOLE_FILTER = "console-filter";

    String USE_MONITOR = "use-monitor";

    String REQUEST_PORT = "request-port";

    String ICON = "icon";

    String TOOL_TIP = "toolTip";

    String MCP = "mcp";


    String COMMAND_TAG = "command";

    String CONSOLE_COMMAND = "console-command";

    String RAW_COMMAND = "raw-command";

    String ACTION = "action";

    String WIN_VALUE = "win-value";

    String UNIX_VALUE = "unix-value";

    String USE_TERMINAL = "in-terminal";

    String RES_BUNDLE_EXP_START = "${";

    String RES_BUNDLE_EXP_END = "}";

    String ESP_TASKS_ROOT = "esp-tasks";

    String LOCAL_EXEC = "exec";

    String EXEC_PATH = "path";

    String EXEC_ARGS = "args";

    String EXEC_ENCODING = "encoding";

    String EXEC_WITH_IDF_ENV = "idf-env";

    /**
     * 拓展环境变量，可作为行内属性，也可作为子标签，每行一个 key=value
     */
    String ENV = "env";

    /**
     * 变量组 profile 标签：无 name 时匿名（全局导入），带 name 时由任务的 profile 属性引用
     */
    String PROFILE = "profile";

    /**
     * profile 内的拓展环境变量标签（多行 key=value），可用 ${env:key} 展开
     */
    String ENVS = "envs";

    /**
     * profile 内的替换宏标签（多行 key=value），可用 ${v:key} 展开
     */
    String MACROS = "macros";

    String ESP_CUSTOM_TASKS_XML = "esp_custom_tasks.xml";

    String ESP_CUSTOM_TASKS_XML_WIN_TEMPLATE = "esp_custom_tasks_windows.xml";

    String ESP_CUSTOM_TASKS_XML_UNIX_TEMPLATE = "esp_custom_tasks_unix_like.xml";

}
