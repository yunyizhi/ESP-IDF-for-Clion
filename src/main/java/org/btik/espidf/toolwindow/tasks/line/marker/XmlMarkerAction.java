package org.btik.espidf.toolwindow.tasks.line.marker;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.btik.espidf.toolwindow.tasks.TreeNodeCmdExecutor;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskConsoleCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskTreeNode;
import org.btik.espidf.toolwindow.tasks.model.LocalExecNode;
import org.jetbrains.annotations.NotNull;

public class XmlMarkerAction extends AnAction {
    private final Project project;
    private final EspIdfTaskTreeNode node;

    public XmlMarkerAction(EspIdfTaskTreeNode espIdfTaskTreeNode, @NotNull Project project) {
        super(espIdfTaskTreeNode.getDisplayName(), espIdfTaskTreeNode.getToolTip(), AllIcons.Actions.Execute);
        this.node = espIdfTaskTreeNode;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (node instanceof EspIdfTaskConsoleCommandNode consoleCommandNode) {
            TreeNodeCmdExecutor.execute(consoleCommandNode, project);
        } else if  (node instanceof EspIdfTaskCommandNode commandNode) {
            TreeNodeCmdExecutor.execute(commandNode, project);
        } else if  (node instanceof LocalExecNode localExecNode) {
            TreeNodeCmdExecutor.execute(localExecNode, project);
        }
    }

    public EspIdfTaskTreeNode getNode() {
        return node;
    }
}
