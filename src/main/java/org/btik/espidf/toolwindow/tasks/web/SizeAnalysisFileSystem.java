package org.btik.espidf.toolwindow.tasks.web;

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author lustre
 * @since 2025/6/21
 */
public class SizeAnalysisFileSystem extends DeprecatedVirtualFileSystem {
    private static final SizeAnalysisFileSystem INSTANCE = new SizeAnalysisFileSystem();
    private static final String PROTOCOL = "espidf-size";

    public static SizeAnalysisFileSystem getInstance() {
        return INSTANCE;
    }

    @Override
    public @NotNull String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public @Nullable VirtualFile findFileByPath(@NotNull String path) {
        return null;
    }

    @Override
    public void refresh(boolean asynchronous) {
    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(@NotNull String path) {
        return null;
    }

    @Override
    protected void deleteFile(Object requestor, @NotNull VirtualFile vFile) {
    }

    @Override
    protected void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) {
    }

    @Override
    protected void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) {
    }

    @Override
    public @NotNull VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualFile copyFile(Object requestor, @NotNull VirtualFile virtualFile, @NotNull VirtualFile newParent, @NotNull String copyName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }


}
