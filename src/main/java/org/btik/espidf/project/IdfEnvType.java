package org.btik.espidf.project;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/4/28 1:56
 */
public enum IdfEnvType {
    IDF_TOOL("idf.env.type.package"),
    IDF_FRAMEWORK("idf.env.type.source");

    IdfEnvType(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    private final String i18nKey;

    @Override
    public String toString() {
        return $i18n(i18nKey);
    }
}
