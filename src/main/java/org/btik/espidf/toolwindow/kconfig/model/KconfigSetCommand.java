package org.btik.espidf.toolwindow.kconfig.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class KconfigSetCommand {
    private final int version = 2;

    @SerializedName("set")
    private Map<String,Object> values;

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public int getVersion() {
        return version;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
