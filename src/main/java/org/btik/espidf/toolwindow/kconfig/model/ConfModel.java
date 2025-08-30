package org.btik.espidf.toolwindow.kconfig.model;

import com.google.gson.annotations.SerializedName;
import org.btik.espidf.util.TreeBean;

import java.util.Arrays;
import java.util.List;

/**
 * @author lustre
 * @since 2025/6/14 14:16
 */
public class ConfModel implements TreeBean<ConfModel> {
    private KconfigType type;
    private KconfigType redefinedType;
    private String name;
    private String title;
    private String help;
    private String id;
    @SerializedName("is_menuconfig")
    private boolean isMenuconfig;
    private long[] range;
    private List<ConfModel> children;

    private Object value;
    private boolean asMenuPanelItem;

    private boolean visible = true;

    private boolean hasPanelItem = false;

    private ConfModel parent;

    public KconfigType getType() {
        return type;
    }

    public void setType(KconfigType type) {
        this.type = type;
    }

    public KconfigType getRedefinedType() {
        return redefinedType == null ? type : redefinedType;
    }

    public void setRedefinedType(KconfigType redefinedType) {
        this.redefinedType = redefinedType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long[] getRange() {
        return range;
    }

    public void setRange(long[] range) {
        this.range = range;
    }

    public boolean isMenuconfig() {
        return isMenuconfig;
    }

    public void setMenuconfig(boolean menuconfig) {
        isMenuconfig = menuconfig;
    }

    public List<ConfModel> getChildren() {
        return children;
    }

    public void setChildren(List<ConfModel> children) {
        this.children = children;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public ConfModel getParent() {
        return parent;
    }

    public void setParent(ConfModel parent) {
        this.parent = parent;
    }

    public boolean isHasPanelItem() {
        return hasPanelItem;
    }

    public void setHasPanelItem(boolean hasPanelItem) {
        this.hasPanelItem = hasPanelItem;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isPanelItem(){
        return switch (getRedefinedType()) {
            case BOOL, CHOICE, STRING, INT, HEX -> true;
            default -> false;
        };
    }

    public void cutChain() {
        if (children != null) {
            for (ConfModel confModel : children) {
                confModel.setParent(null);
            }
            setChildren(null);
        }
    }

    public String dump() {
        return "ConfModel{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", help='" + help + '\'' +
                ", id='" + id + '\'' +
                ", range=" + Arrays.toString(range) +
                ", isMenuconfig=" + isMenuconfig +
                ", children=" + children +
                '}';
    }


    public ConfModel getFirstChild(){
        return children == null || children.isEmpty() ? null : children.get(0);
    }

    @Override
    public String toString() {
        return title == null ? name : title;
    }

    public boolean isAsMenuPanelItem() {
        return asMenuPanelItem;
    }

    public void setAsMenuPanelItem(boolean asMenuPanelItem) {
        this.asMenuPanelItem = asMenuPanelItem;
    }
    public boolean isTreeNode() {
        return  getRedefinedType() != KconfigType.CHOICE_ITEM;
    }
}
