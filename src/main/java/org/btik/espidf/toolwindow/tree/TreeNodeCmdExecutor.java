package org.btik.espidf.toolwindow.tree;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.environment.IdfEnvironmentService;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskCommandNode;
import org.btik.espidf.util.CmdTaskExecutor;

import java.nio.charset.Charset;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;

/**
 * @author lustre
 * @since 2024/2/18 18:51
 */
public class TreeNodeCmdExecutor {
    public static void execute(EspIdfTaskCommandNode commandNode, Project project) {
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(IS_WINDOWS ? "cmd.exe" : "/bin/bash");
        commandLine.setWorkDirectory(project.getBasePath());
        commandLine.withEnvironment(environmentService.getEnvironments());
        commandLine.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        commandLine.addParameters("/c",
                IS_WINDOWS ? "idf.py.exe" : "idf.py",
                commandNode.getCommand());

        try {
            CmdTaskExecutor.execute(project, new IdfConsoleRunProfile(commandNode.getDisplayName(),
                    EspIdfIcon.IDF_16_16, commandLine), null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
