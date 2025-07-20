package org.btik.espidf.toolwindow.tasks.model;

/**
 * @author lustre
 * @since 2024/2/18 14:42
 */
public class EspIdfTaskCommandNode extends EspIdfTaskTreeNode{
    private boolean useMonitor = false;
    private boolean requestPort = false;
    private String command;
    private boolean outFilter;

    public EspIdfTaskCommandNode(String displayName, String command, boolean outFilter) {
        super(displayName);
        this.command = command;
        this.outFilter = outFilter;
    }


    public boolean isUseMonitor() {
        return useMonitor;
    }

    public void setUseMonitor(boolean useMonitor) {
        this.useMonitor = useMonitor;
    }

    public boolean isRequestPort() {
        return requestPort;
    }

    public void setRequestPort(boolean requestPort) {
        this.requestPort = requestPort;
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


