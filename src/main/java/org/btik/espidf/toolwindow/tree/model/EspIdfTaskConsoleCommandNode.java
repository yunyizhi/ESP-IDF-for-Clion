package org.btik.espidf.toolwindow.tree.model;

/**
 * 和普通的启动进程区别在与有终端的环境，如果是TUI操作和快捷键响应需要控制台
 * {@link #inTerminal} 使用在终端中执行则是使用ide内置终端执行执行完毕也会留下终端,默认为true。
 *
 * @author lustre
 * @since 2024/2/19 19:00
 */
public class EspIdfTaskConsoleCommandNode extends EspIdfTaskTreeNode {
    private String command;

    private boolean inTerminal;

    public EspIdfTaskConsoleCommandNode(String displayName, String command, boolean inTerminal) {
        super(displayName);
        this.command = command;
        this.inTerminal = inTerminal;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isInTerminal() {
        return inTerminal;
    }

    public void setInTerminal(boolean inTerminal) {
        this.inTerminal = inTerminal;
    }
}
