package org.btik.espidf.project;

import com.intellij.execution.ExecutionException;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.util.I18nMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.btik.espidf.adapter.Adapter.readEnvironment;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/11 17:00
 */
public class UnixLikeGenerator<T> extends SubGenerator<T> {
    private String idfFrameworkPath;

    public void setIdfFrameworkPath(String idfFrameworkPath) {
        this.idfFrameworkPath = idfFrameworkPath;
    }

    @Override
    public ValidationResult validate() {
        if (StringUtil.isEmpty(idfFrameworkPath)) {
            return new ValidationResult($i18n("please.select.idf.path"));
        }
        Path folder = Path.of(idfFrameworkPath);
        if (!Files.exists(folder)){
            return new ValidationResult($i18n("please.select.idf.path.not.exist"));
        }
        Path exportSh = folder.resolve($sys("idf.unix.export.script"));
        if (!Files.exists(exportSh)){
            return new ValidationResult($i18n("idf.folder.invalid"));
        }
        return ValidationResult.OK;
    }

    @Override
    public void generateProject() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
                IdfToolConf idfToolConf = environmentService.getSourceToolConf(idfFrameworkPath);
                final IdfToolConf idfToolConf1 = idfToolConf;
                Map<String, String> readEnvironment = ApplicationManager.getApplication()
                        .executeOnPooledThread(() -> readEnvironment(idfToolConf1)).get();
                String toolChainName = idfToolConf.getToolchain().getName();
                generateProject(readEnvironment, toolChainName);
            } catch (java.util.concurrent.ExecutionException | InterruptedException | ExecutionException e) {
                I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("idf.cmd.init.project.failed"),
                        e.getMessage(), NotificationType.ERROR).notify(project);
                throw new RuntimeException(e);
            }
        });
    }
}
