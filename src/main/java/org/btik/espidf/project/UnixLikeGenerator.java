package org.btik.espidf.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.btik.espidf.command.IdfConsoleRunProfile;
import org.btik.espidf.conf.IdfToolConf;
import org.btik.espidf.icon.EspIdfIcon;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.I18nMessage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;

import static org.btik.espidf.adapter.Adapter.readEnvironment;
import static org.btik.espidf.util.I18nMessage.$i18n;

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
        File file = new File(idfFrameworkPath);
        if (!file.exists()) {
            return new ValidationResult($i18n("please.select.idf.path.not.exist"));
        }
        return ValidationResult.OK;
    }

    @Override
    public void generateProject() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
                IdfToolConf idfToolConf = environmentService.getUnixToolConf(idfFrameworkPath);
                final IdfToolConf idfToolConf1 = idfToolConf;
                Map<String, String> readEnvironment = ApplicationManager.getApplication()
                        .executeOnPooledThread(() -> readEnvironment(idfToolConf1)).get();
                String toolChainName = idfToolConf.getToolchain().getName();
                generateProject(readEnvironment, toolChainName);
            } catch (java.util.concurrent.ExecutionException | InterruptedException | ExecutionException e) {
                I18nMessage.NOTIFICATION_GROUP.createNotification(I18nMessage.getMsg("idf.cmd.init.project.failed"),
                        e.getMessage(), NotificationType.ERROR).notify(project);
                throw new RuntimeException(e);
            }
        });
    }
}
