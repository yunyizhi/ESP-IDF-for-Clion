package org.btik.espidf.toolwindow.tasks.model;

/**
 * @author lustre
 * @since 2024/2/18 14:42
 */
public class EspIdfTaskCommandNode extends EspIdfTaskTreeNode{
    private String command;
    private boolean outFilter;

    public EspIdfTaskCommandNode(String displayName, String command, boolean outFilter) {
        super(displayName);
        this.command = command;
        this.outFilter = outFilter;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isOutFilter() {
        return outFilter;
    }

    public void setOutFilter(boolean outFilter) {
        this.outFilter = outFilter;
    }
}


