package org.btik.espidf.toolwindow.kconfig.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author lustre
 * @since 2025/6/14 14:54
 */
public enum KconfigType {
    @SerializedName("bool") BOOL,
    @SerializedName("choice") CHOICE,
    @SerializedName("hex") HEX,
    @SerializedName("int") INT,
    @SerializedName("menu") MENU,
    @SerializedName("string") STRING
}
