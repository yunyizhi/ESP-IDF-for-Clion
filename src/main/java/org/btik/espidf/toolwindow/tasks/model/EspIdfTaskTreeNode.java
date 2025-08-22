package org.btik.espidf.toolwindow.tasks.model;

import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.toolwindow.tasks.EspIdfTaskTreeFactory;

import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * @author lustre
 * @since 2024/2/18 14:15
 */
public class EspIdfTaskTreeNode {
    private final String displayName;

    protected String icon;

    protected String id;

    protected String toolTip;

    public EspIdfTaskTreeNode(String displayName) {
        String i18nName = EspIdfTaskTreeFactory.getI18n(displayName);
        if (StringUtils.isEmpty(i18nName)) {
            i18nName = $i18n("esp.idf.nameless.node");
        }
        this.displayName = i18nName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToolTip() {
        return toolTip;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
