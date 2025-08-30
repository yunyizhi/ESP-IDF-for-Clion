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
    @SerializedName("string") STRING,
    // bool类型做CHOICE子项时其实是下拉菜单
    CHOICE_ITEM,
    /*
    * bool类型含子项时 其值会使能其子项
    * 或者menu isMenuconfig 为true的情况也属于使能开关的场景
    * */
    ENABLE_SWITCH,
}
