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

    String RES_BUNDLE_EXP_START = "${";

    String RES_BUNDLE_EXP_END = "}";
}
