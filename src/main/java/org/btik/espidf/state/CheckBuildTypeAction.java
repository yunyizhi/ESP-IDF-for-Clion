package org.btik.espidf.state;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.command.ProcessEventAdaptor;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.run.config.model.DebugConfigModel;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.EnvironmentVarUtil;
import org.btik.espidf.util.EspIdfProjectUtil;
import org.btik.espidf.util.SysConf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;

/**
 * @author lustre
 * @since 2025/7/24 15:38
 */
public class CheckBuildTypeAction extends AnAction implements DumbAware {

    private final String chipTarget;
    public final Runnable callback;

    private final boolean preview;
    public CheckBuildTypeAction(@Nullable @NlsActions.ActionText String text, @NotNull String chipTarget, boolean previewSelected, Runnable callback) {
        super(text);
        this.chipTarget = chipTarget;
        this.preview = previewSelected;
        this.callback = callback;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        Project project = e.getProject();
        if (project == null) {
            return;
        }
        DebugConfigModel debugConfigModel = EspIdfProjectUtil.syncProjectDesc(project);
        if (debugConfigModel != null && Objects.equals(debugConfigModel.getTarget(), chipTarget)) {
            return;
        }
        Map<String, String> envs = project.getService(IdfEnvironmentService.class).getEnvironments();
        PtyCommandLine commandLine = new PtyCommandLine();
        commandLine.setExePath(EnvironmentVarUtil.findIdfFullPath(envs));
        commandLine.setWorkDirectory(project.getBasePath());
        commandLine.withEnvironment(envs);
        commandLine.setCharset(Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8")));
        if (preview) {
            commandLine.addParameters("--preview");
        }
        commandLine.addParameters(
                "-B", project.getService(IdfProjectConfigService.class).getCmakeBuildDir(),
                "set-target", chipTarget
        );
        if (IS_WINDOWS) {
            commandLine.withInitialColumns(SysConf.getInt("esp.idf.pyt.cmd.cols", 255));
        }
        try {
            IdfConsoleRunProfile idfConsoleRunProfile = new IdfConsoleRunProfile($i18n("idf.set.project.target"),
                    EspIdfIcon.IDF_16_16, commandLine);
            idfConsoleRunProfile.setUseOutFilter(true);
            CmdTaskExecutor.execute(project, idfConsoleRunProfile,
                    new ProcessEventAdaptor().withProcessTerminatedCb((event) -> ApplicationManager.getApplication().invokeLater(callback)));
        } catch (ExecutionException exception) {
            throw new RuntimeException(exception);
        }
    }
}
