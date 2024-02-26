package org.btik.espidf.toolwindow.tree.model;

/**
 * @author lustre
 * @since 2024/2/19 19:00
 */
public class RawCommandNode extends EspIdfTaskTreeNode {
    private String command;

    public RawCommandNode(String displayName, String command) {
        super(displayName);
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
