package org.btik.espidf.toolwindow.kconfig.model;

import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * @author lustre
 * @since 2025/6/21 0:09
 */
public class KconfigStatus {

    private boolean isError;

    private List<String> error;

    private String version;

    private Map<String, long []> ranges;

    private Map<String,Boolean> visible;

    private Map<String,Object> values;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, long[]> getRanges() {
        return ranges;
    }

    public void setRanges(Map<String, long[]> ranges) {
        this.ranges = ranges;
    }

    public Map<String, Boolean> getVisible() {
        return visible;
    }

    public void setVisible(Map<String, Boolean> visible) {
        this.visible = visible;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public boolean isError() {
        return isError;
    }

    public List<String> getError() {
        return error;
    }

    public void setError(List<String> error) {
        this.error = error;
        this.isError = CollectionUtils.isNotEmpty(error);
    }
}
