package org.btik.espidf.run.config.gdbinit;


import org.jetbrains.annotations.NotNull;

public record GdbInitProfileInfo(String path, String target, String buildDir, String profileName) {
    @Override
    public @NotNull String toString() {
        return path;
    }
}
