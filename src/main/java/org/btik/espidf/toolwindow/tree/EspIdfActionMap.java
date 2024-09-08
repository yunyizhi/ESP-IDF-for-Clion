package org.btik.espidf.toolwindow.tree;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.sh.run.ShConfigurationType;
import com.intellij.sh.run.ShRunConfiguration;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.toolwindow.tree.model.EspIdfTaskActionNode;
import org.btik.espidf.util.I18nMessage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.BiConsumer;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/5/2 12:33
 */
public class EspIdfActionMap {
    private static final HashMap<String, BiConsumer<EspIdfTaskActionNode, Project>> actionMap;

    static {
        actionMap = new HashMap<>();
        actionMap.put("idf.export.console", EspIdfActionMap::exportConsole);
    }

    private static void exportConsole(EspIdfTaskActionNode actionNode, Project project) {
        IdfSysConfService service = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        IdfToolConf idfConfByProject = service.getIdfConfByProject(project);
        if (idfConfByProject == null) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    $i18n("action.exec.toolchain.notfound"),
                    NotificationType.ERROR).notify(project);
            return;
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project)
                .createConfiguration(actionNode.getDisplayName(), ShConfigurationType.class);
        ShRunConfiguration runConfiguration = (ShRunConfiguration) settings.getConfiguration();
        runConfiguration.setExecuteInTerminal(true);
        runConfiguration.setExecuteScriptFile(false);
        Path installPath = Path.of(idfConfByProject.getIdfToolPath());
        if (StringUtil.isEmpty(idfConfByProject.getIdfId())) {
            if (IS_WINDOWS) {
                runConfiguration.setScriptText(". \"" + installPath.resolve($sys("idf.windows.export.ps1")) + '"');
            } else {
                runConfiguration.setScriptText(". \"" + installPath.resolve($sys("idf.unix.export.script")) + '"');
            }
        } else {
            runConfiguration.setScriptText(". \"" + installPath.resolve($sys("idf.window.powershell.init.ps1")) + "\" -IdfId " + idfConfByProject.getIdfId());
        }
        runConfiguration.setScriptWorkingDirectory(basePath);

        ExecutionEnvironmentBuilder builder =
                ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), runConfiguration);
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build());
        }
    }

    public static void exec(EspIdfTaskActionNode actionNode, Project project) {
        BiConsumer<EspIdfTaskActionNode, Project> action = actionMap.get(actionNode.getId());
        if (action == null) {
            I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("action.exec.failed"),
                    $i18n("action.exec.action.notfound"),
                    NotificationType.ERROR).notify(project);
            return;
        }
        action.accept(actionNode, project);
    }

}
