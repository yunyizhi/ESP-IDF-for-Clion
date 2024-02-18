package org.btik.espidf.toolwindow.tree.model;

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
        this.displayName = displayName;
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
