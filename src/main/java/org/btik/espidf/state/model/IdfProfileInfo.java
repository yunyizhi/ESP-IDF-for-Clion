package org.btik.espidf.state.model;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class IdfProfileInfo {

    private String target;
    private String displayName;
    private String buildDir;

    private long descFileChangeTime;

    private long descFileSize;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBuildDir() {
        return buildDir;
    }

    public void setBuildDir(String buildDir) {
        this.buildDir = buildDir;
    }

    public long getDescFileChangeTime() {
        return descFileChangeTime;
    }

    public void setDescFileChangeTime(long descFileChangeTime) {
        this.descFileChangeTime = descFileChangeTime;
    }

    public long getDescFileSize() {
        return descFileSize;
    }

    public void setDescFileSize(long descFileSize) {
        this.descFileSize = descFileSize;
    }

    public boolean fileHasUpdate(@NotNull  File descFile) {
        return descFile.length() != descFileSize || descFile.lastModified() != descFileChangeTime;
    }
}
