package org.btik.espidf.project;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.btik.espidf.util.I18nMessage;

/**
 * @author lustre
 * @since 2024/2/11 17:00
 */
public class UnixLikeGenerator<T> implements SubGenerator<T>{

    private String idfFrameworkPath;

    public void setIdfFrameworkPath(String idfFrameworkPath) {
        this.idfFrameworkPath = idfFrameworkPath;
    }
    @Override
    public ValidationResult validate() {
        if (StringUtil.isEmpty(idfFrameworkPath)) {
            return new ValidationResult(I18nMessage.$i18n("please.select.idf.tools.path.for.idf"));
        }
        return ValidationResult.OK;
    }

    @Override
    public void generateProject(Project project, VirtualFile baseDir, T settings, Module module) {

    }
}
