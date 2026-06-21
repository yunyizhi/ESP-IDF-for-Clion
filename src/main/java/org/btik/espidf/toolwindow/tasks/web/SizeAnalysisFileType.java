package org.btik.espidf.toolwindow.tasks.web;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author lustre
 * @since 2025/6/20
 */
public class SizeAnalysisFileType implements FileType {
    public static final SizeAnalysisFileType INSTANCE = new SizeAnalysisFileType();
    public static final String DEFAULT_EXTENSION = "espidf-size";

    @NotNull
    @Override
    public String getName() {
        return "ESP-IDF Size Analysis";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "ESP-IDF size analysis report";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean isBinary() {
        return false;
    }
}
