package org.btik.espidf.project.generator.idfenv;


import com.intellij.execution.ExecutionException;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.conf.LastChosenIdfEnv;
import org.btik.espidf.project.generator.GeneratorActions;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.util.I18nMessage;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.btik.espidf.project.generator.idfenv.IdfEnvConf.IDF_ENV_JSON;
import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/11 17:00
 */
public class IdfProjectCreatorActions<T> extends GeneratorActions<T> {
    private String idfToolsPath;
    private IdfEnvConf idfEnvConf;

    public void setIdfToolsPath(String idfToolsPath) {
        this.idfToolsPath = idfToolsPath;
    }

    public void setIdfEnvConf(IdfEnvConf idfEnvConf) {
        this.idfEnvConf = idfEnvConf;
    }

    @Override
    public ValidationResult validate() {
        if (StringUtil.isEmpty(idfToolsPath)) {
            return new ValidationResult($i18n("please.select.idf.path"));
        }
        Path folder = Path.of(idfToolsPath);
        if (!Files.exists(folder)) {
            return new ValidationResult($i18n("please.select.idf.path.not.exist"));
        }
        Path exportSh = folder.resolve(IDF_ENV_JSON);
        if (!Files.exists(exportSh)) {
            return new ValidationResult($i18n("idf.tool.folder.invalid"));
        }
        return ValidationResult.OK;
    }

    @Override
    public void generateProject() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                idfEnvConf.setIdfToolsPath(idfToolsPath);
                IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
                IdfToolConf idfToolConf = environmentService.getSourceToolConf(idfEnvConf.getPath());
                Map<String, String> envOfToolChain = environmentService.getEnvOfToolChain(idfToolConf.getToolchain());
                String toolChainName = idfToolConf.getToolchain().getName();
                generateProject(envOfToolChain, toolChainName);
                IdfSysConfService sysConfService = ApplicationManager.getApplication().getService(IdfSysConfService.class);
                LastChosenIdfEnv lastChosenIdfEnv = new LastChosenIdfEnv();
                lastChosenIdfEnv.setIdfPath(idfEnvConf.getPath());
                lastChosenIdfEnv.setIdfToolsPath(idfToolsPath);
                sysConfService.setLastEnv(lastChosenIdfEnv);
            } catch (RuntimeException | ExecutionException e) {
                I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("idf.cmd.init.project.failed"), e.getMessage(), NotificationType.ERROR).notify(project);
            }
        });
    }
}
