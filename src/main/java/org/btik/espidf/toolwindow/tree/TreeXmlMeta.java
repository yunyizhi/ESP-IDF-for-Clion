package org.btik.espidf.toolwindow.tree;

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

    String ICON = "icon";

    String TOOL_TIP = "toolTip";


    String COMMAND_TAG = "command";

    String TERMINAL_COMMAND = "terminal-command";

    String RAW_COMMAND = "raw-command";

    String ACTION = "action";

    String WIN_VALUE = "win-value";

    String UNIX_VALUE = "unix-value";

    String RES_BUNDLE_EXP_START = "${";

    String RES_BUNDLE_EXP_END = "}";
}
