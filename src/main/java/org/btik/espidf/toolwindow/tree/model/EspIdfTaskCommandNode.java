package org.btik.espidf.toolwindow.tree.model;

/**
 * @author lustre
 * @since 2024/2/18 14:42
 */
public class EspIdfTaskCommandNode extends EspIdfTaskTreeNode{
    private String command;
    public EspIdfTaskCommandNode(String displayName, String command) {
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


