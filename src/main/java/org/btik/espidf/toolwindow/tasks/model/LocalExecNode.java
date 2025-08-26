package org.btik.espidf.toolwindow.tasks.model;

public class LocalExecNode extends EspIdfTaskTreeNode {
    private  String path;
    private  String args;
    private boolean useIdfEnv = false;
    private boolean useTerminal = false;
    private String encoding = "UTF-8";

    public LocalExecNode(String displayName, String path, String args) {
        super(displayName);
        this.path = path;
        this.args = args;
    }

    public boolean isUseIdfEnv() {
        return useIdfEnv;
    }

    public void setUseIdfEnv(boolean useIdfEnv) {
        this.useIdfEnv = useIdfEnv;
    }

    public boolean isUseTerminal() {
        return useTerminal;
    }

    public void setUseTerminal(boolean useTerminal) {
        this.useTerminal = useTerminal;
    }

    public String getPath() {
        return path;
    }

    public String getArgs() {
        return args;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}

