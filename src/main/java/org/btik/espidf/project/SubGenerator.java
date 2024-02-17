package org.btik.espidf.project;


import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author lustre
 * @since 2024/2/11 17:18
 */
public interface SubGenerator<T> {
    String IDF_CMAKE_PROFILE_NAME = "idf";

    String IDF_CMAKE_BUILD_DIR = "build";
    ValidationResult validate();

    void generateProject(Project project, VirtualFile baseDir, T settings, Module module);
}
