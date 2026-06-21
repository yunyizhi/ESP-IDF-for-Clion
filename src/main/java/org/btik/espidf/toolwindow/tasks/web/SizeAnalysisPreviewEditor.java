package org.btik.espidf.toolwindow.tasks.web;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author lustre
 * @since 2025/6/20
 */
public class SizeAnalysisPreviewEditor extends UserDataHolderBase implements FileEditor {
    private static final Logger LOG = Logger.getInstance(SizeAnalysisPreviewEditor.class);

    private final Project project;
    private final VirtualFile file;
    private JBCefBrowser browser;
    private JPanel panel;

    public SizeAnalysisPreviewEditor(Project project, VirtualFile file) {
        this.project = project;
        this.file = file;
        createComponent();
        loadUrlFromFile();
    }

    private void createComponent() {
        panel = new JPanel(new BorderLayout());
        if (JBCefApp.isSupported()) {
            browser = new JBCefBrowser();
            panel.add(browser.getComponent(), BorderLayout.CENTER);
        } else {
            panel.add(new JLabel("JCEF not supported", SwingConstants.CENTER), BorderLayout.CENTER);
        }
    }

    private void loadUrlFromFile() {
        if (browser != null && file != null) {
            try {
                String url = new String(file.contentsToByteArray(), StandardCharsets.UTF_8).trim();
                if (!url.isEmpty()) {
                    browser.loadURL(url);
                }
            } catch (IOException e) {
                LOG.warn("Failed to read URL from file", e);
            }
        }
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return browser != null ? browser.getComponent() : null;
    }

    @Override
    public @NotNull String getName() {
        String name = file.getName();
        return name.replace("." + SizeAnalysisFileType.DEFAULT_EXTENSION, "");
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void dispose() {
        if (browser != null) {
            browser.dispose();
        }
    }
}

