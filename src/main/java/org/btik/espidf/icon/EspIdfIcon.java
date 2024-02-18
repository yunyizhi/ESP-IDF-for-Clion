package org.btik.espidf.icon;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author lustre
 * @since 2024/2/18 9:42
 */
public class EspIdfIcon {
    private static @NotNull Icon load(@NotNull String path) {
        return IconLoader.getIcon(path, EspIdfIcon.class);

    }
  public static final @NotNull Icon IDF_16_16 = load("/org-btik-esp-idf/image/idf16_16.svg");
}
