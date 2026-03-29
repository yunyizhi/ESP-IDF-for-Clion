package org.btik.espidf.project.generator.toolchain;

import org.jetbrains.annotations.NotNull;

public record IdfToolchain(String name, String envFile, String idfVersion, String idfPath) {

    @Override
    public @NotNull String toString() {
        return name;
    }
}
