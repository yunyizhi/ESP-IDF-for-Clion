package org.btik.espidf.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.I18nMessage;
import org.btik.espidf.util.OsUtil;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;

import static org.btik.espidf.adapter.Adapter.readEnvironment;
import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/11 16:57
 */
public class WindowsGenerator<T> extends SubGenerator<T> {
    private final Logger LOG = Logger.getInstance(WindowsGenerator.class);
    private String idfToolPath;

    private String idfId;

    public void setIdfToolsPath(String text) {
        idfToolPath = text;
    }


    public void setIdfId(String idfId) {
        this.idfId = idfId;
    }

    @Override
    public ValidationResult validate() {
        if (StringUtil.isEmpty(idfToolPath)) {
            return new ValidationResult(I18nMessage.$i18n("please.select.idf.tools.path.for.idf"));
        }
        if (StringUtil.isEmpty(idfId)) {
            return new ValidationResult(I18nMessage.$i18n("please.select.idf.id"));
        }
        return ValidationResult.OK;
    }

    @Override
    public void generateProject() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
                IdfToolConf idfToolConf = environmentService.getWinToolConf(idfToolPath, idfId);
                Map<String, String> readEnvironment = readEnvironment(idfToolConf);
                String toolChainName = idfToolConf.getToolchain().getName();
                generateProject(readEnvironment, toolChainName);
            } catch (Exception e) {
                LOG.error(e);
                I18nMessage.NOTIFICATION_GROUP.createNotification(I18nMessage.getMsg("idf.cmd.init.project.failed"),
                        e.getMessage(), NotificationType.ERROR).notify(project);
            }
        });
    }
}
