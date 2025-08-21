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


    String COMMAND_TAG = "command";

    String CONSOLE_COMMAND = "console-command";

    String RAW_COMMAND = "raw-command";

    String ACTION = "action";

    String WIN_VALUE = "win-value";

    String UNIX_VALUE = "unix-value";

    String USE_TERMINAL = "use-terminal";

    String RES_BUNDLE_EXP_START = "${";

    String RES_BUNDLE_EXP_END = "}";

    String ESP_TASKS_ROOT = "esp-tasks";

    String LOCAL_EXEC = "exec";

    String EXEC_PATH = "path";

    String EXEC_ARGS = "args";

    String EXEC_WITH_IDF_ENV = "idf-env";

}
