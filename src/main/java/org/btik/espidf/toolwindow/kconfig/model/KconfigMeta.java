package org.btik.espidf.toolwindow.kconfig.model;

/**
 * @author lustre
 * @since 2025/6/21 0:56
 */
public interface KconfigMeta {
    String ERROR = "error";

    String SAVE_DEFAULT = """
            {"version":2,"save":null}""";

    String LOAD_DEFAULT = """
            {"version":2,"load":null}""";
}
