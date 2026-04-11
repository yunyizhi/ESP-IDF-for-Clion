package org.btik.espidf.project.generator.idfenv;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.project.generator.GeneratorActions;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.util.I18nMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2024/2/11 16:57
 */
public class WindowsGeneratorActions<T> extends GeneratorActions<T> {
    private final Logger LOG = Logger.getInstance(WindowsGeneratorActions.class);
    private String installPath;

    private String idfId;

    private IdfEnvType envType = IdfEnvType.IDF_TOOL;

    public void setIdfToolsPath(String text) {
        installPath = text;
    }


    public void setIdfId(String idfId) {
        this.idfId = idfId;
    }

    @Override
    public ValidationResult validate() {
        if (envType == IdfEnvType.IDF_TOOL) {
            if (StringUtil.isEmpty(installPath)) {
                return new ValidationResult($i18n("please.select.idf.tools.path.for.idf"));
            }
            if (StringUtil.isEmpty(idfId)) {
                return new ValidationResult($i18n("please.select.idf.id"));
            }
            Path folder = Path.of(installPath);
            if (!Files.exists(folder)) {
                return new ValidationResult($i18n("please.select.idf.path.not.exist"));
            }
            Path initBat = folder.resolve($sys("idf.windows.command.init.bat"));
            if (!Files.exists(initBat)) {
                return new ValidationResult($i18n("idf.tool.folder.invalid"));
            }
        } else {
            if (StringUtil.isEmpty(installPath)) {
                return new ValidationResult($i18n("please.select.idf.path"));
            }
            Path folder = Path.of(installPath);
            if (!Files.exists(folder)) {
                return new ValidationResult($i18n("please.select.idf.path.not.exist"));
            }
            Path exportBat = folder.resolve($sys("idf.windows.export.bat"));
            if (!Files.exists(exportBat)) {
                return new ValidationResult($i18n("idf.folder.invalid"));
            }
        }

        return ValidationResult.OK;
    }

    @Override
    public void generateProject() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
                IdfToolConf idfToolConf;
                if (envType == IdfEnvType.IDF_TOOL) {
                    idfToolConf = environmentService.getWinToolConf(installPath, idfId);
                } else {
                    idfToolConf = environmentService.getSourceToolConf(installPath);
                }

                Map<String, String> envOfToolChain = environmentService.getEnvOfToolChain(idfToolConf.getToolchain());
                String toolChainName = idfToolConf.getToolchain().getName();
                generateProject(envOfToolChain, toolChainName);
            } catch (Exception e) {
                LOG.error(e);
                I18nMessage.NOTIFICATION_GROUP.createNotification(I18nMessage.getMsg("idf.cmd.init.project.failed"),
                        e.getMessage(), NotificationType.ERROR).notify(project);
            }
        });
    }

    public void setEnvType(IdfEnvType envType) {
        this.envType = envType;
    }
}
