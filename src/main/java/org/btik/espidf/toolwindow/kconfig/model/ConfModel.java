package org.btik.espidf.toolwindow.kconfig.model;

import java.util.Arrays;
import java.util.List;

/**
 * @author lustre
 * @since 2025/6/14 14:16
 */
public class ConfModel {
    private KconfigType type;
    private String name;
    private String title;
    private String help;
    private String id;
    private boolean isMenuconfig;
    private int[] range;
    private List<ConfModel> children;
    /**
     * 原始叶子节点放入右侧面板，留在树上叶子节点为json叶子节点父节点
     * */
    private boolean isLeaf;

    private boolean visible = true;

    private ConfModel parent;

    public KconfigType getType() {
        return type;
    }

    public void setType(KconfigType type) {
        this.type = type;
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

    public int[] getRange() {
        return range;
    }

    public void setRange(int[] range) {
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

    @Override
    public String toString() {
        return title == null ? name : title;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }
}
