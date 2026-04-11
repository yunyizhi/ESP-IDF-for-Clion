package org.btik.espidf.project.generator.toolchain;

import com.intellij.execution.ExecutionException;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import org.btik.espidf.project.generator.GeneratorActions;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.util.I18nMessage;

import java.util.Map;

import static org.btik.espidf.util.I18nMessage.$i18n;

public class IdfProjectCreatorActions<T> extends GeneratorActions<T> {
    @Override
    public ValidationResult validate() {
        return ValidationResult.OK;
    }

    private IdfToolchain idfToolchain;

    @Override
    public void generateProject() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
                Map<String, String> envOfToolChain = environmentService.setCache(idfToolchain.getToolchain(), idfToolchain.getEnv());
                String toolChainName = idfToolchain.getToolchain().getName();
                generateProject(envOfToolChain, toolChainName);
            } catch (RuntimeException | ExecutionException e) {
                I18nMessage.NOTIFICATION_GROUP.createNotification($i18n("idf.cmd.init.project.failed"), e.getMessage(), NotificationType.ERROR).notify(project);
            }
        });
    }

    public void setIdfToolchain(IdfToolchain idfToolchain) {
        this.idfToolchain = idfToolchain;
    }
}
