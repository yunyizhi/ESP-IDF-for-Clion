package org.btik.espidf.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.tools.ToolProcessAdapter;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.jetbrains.annotations.NotNull;

/**
 * @author lustre
 * @since 2024/2/18 9:16
 */
public class CmdTaskExecutor {
    public static void execute(@NotNull Project project,
                               IdfConsoleRunProfile idfConsoleRunProfile, ProcessListener processListener)
            throws ExecutionException {

        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(
                project, DefaultRunExecutor.getRunExecutorInstance(),
                idfConsoleRunProfile).build();
        environment.setExecutionId(ExecutionEnvironment.getNextUnusedExecutionId());
        environment.getRunner().execute(environment);
        OSProcessHandler handler = new OSProcessHandler(idfConsoleRunProfile.getCommandLine());
        handler.addProcessListener(new ToolProcessAdapter(project, false, "Cmd TaskExecutor"));
        if (processListener != null) {
            handler.addProcessListener(processListener);
        }
        handler.startNotify();
    }
}
