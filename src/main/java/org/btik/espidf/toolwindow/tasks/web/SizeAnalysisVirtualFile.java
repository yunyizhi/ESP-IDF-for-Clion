package org.btik.espidf.toolwindow.tasks.web;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author lustre
 * @since 2025/6/21
 */
public class SizeAnalysisVirtualFile extends VirtualFile {
    private final String name;
    private final byte[] content;
    private final FileType fileType;
    private final boolean writable;
    private final String basePath;

    public SizeAnalysisVirtualFile(String name, String url, @Nullable String basePath) {
        this.name = name;
        this.content = url.getBytes(StandardCharsets.UTF_8);
        this.fileType = SizeAnalysisFileType.INSTANCE;
        this.writable = false;
        this.basePath = basePath;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull FileType getFileType() {
        return fileType;
    }

    @Override
    public @Nullable VirtualFile getParent() {
        return null;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile[] getChildren() {
        return EMPTY_ARRAY;
    }

    @Override
    public @NotNull OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new IOException("read-only virtual file");
    }

    @Override
    public byte @NotNull [] contentsToByteArray() throws IOException {
        return content;
    }

    @Override
    public @NotNull InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public long getLength() {
        return content.length;
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {
    }

    @Override
    public boolean isWritable() {
        return writable;
    }

    @Override
    public @NotNull VirtualFileSystem getFileSystem() {
        return SizeAnalysisFileSystem.getInstance();
    }

    @Override
    public @NonNls @NotNull String getPath() {
        return this.basePath + File.separator + this.name;
    }

    public long getModificationStamp() {
        return 0L;
    }
}
