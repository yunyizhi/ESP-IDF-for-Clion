package org.btik.espidf.toolwindow.kconfig;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.treeStructure.Tree;
import org.apache.commons.collections.CollectionUtils;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.common.NodeModel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2025/6/13 23:04
 */
public class EspIdfMenuConfigPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(EspIdfMenuConfigPanel.class);
    private final Project project;

    private final KconfigTreePanel kconfigTreePanel;

    public EspIdfMenuConfigPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        kconfigTreePanel = new KconfigTreePanel();
        add(kconfigTreePanel, BorderLayout.CENTER);
        loadPage();
    }

    private void loadPage() {
        String basePath = project.getBasePath();
        if (basePath == null) {
            LOG.error("Base path is null");
            return;
        }
        IdfProjectConfigService projectConfigService = project.getService(IdfProjectConfigService.class);
        String cmakeBuildDir = projectConfigService.getCmakeBuildDir();
        Path path = Path.of(basePath, cmakeBuildDir, $sys("esp.idf.kconfig.menus.dir")).resolve($sys("esp.idf.kconfig.menus.file"));
        if (!path.toFile().exists()) {
            LOG.error("File does not exist");
            return;
        }
        Gson gson = new Gson();
        try {
            JsonElement jsonElement = gson.fromJson(new JsonReader(new FileReader(path.toFile())), JsonElement.class);
            List<DefaultMutableTreeNode> root = parseKconfig(jsonElement, gson);
            kconfigTreePanel.setRoot(root);
        } catch (FileNotFoundException e) {
            LOG.error("File does not exist");
        }
    }

    private List<DefaultMutableTreeNode> parseKconfig(JsonElement jsonElement, Gson gson) {
        List<DefaultMutableTreeNode> root = new ArrayList<>();
        if (jsonElement.isJsonArray()) {
            JsonArray asJsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : asJsonArray) {

                JsonObject asJsonObject = element.getAsJsonObject();
                ConfModel confModel = gson.fromJson(asJsonObject, ConfModel.class);
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(confModel);
                if (StringUtil.isEmpty(confModel.getTitle())) {
                    continue;
                }
                root.add(treeNode);
                NodeModel<ConfModel> rootNodeModel = new NodeModel<>(treeNode, confModel);
                LinkedList<NodeModel<ConfModel>> queue = new LinkedList<>();
                queue.add(rootNodeModel);

                while (!queue.isEmpty()) {
                    NodeModel<ConfModel> nodeModel = queue.removeFirst();
                    List<ConfModel> children = nodeModel.getModel().getChildren();
                    if (CollectionUtils.isEmpty(children)) {
                        continue;
                    }
                    for (ConfModel child : children) {
                        DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(child);
                        queue.add(new NodeModel<>(childTreeNode, child));
                        nodeModel.getNode().add(childTreeNode);
                    }
                }

            }
        }
        return root;
    }

    static class KconfigTreePanel extends JScrollPane {
        List<DefaultMutableTreeNode> root;

        public KconfigTreePanel() {
            viewport.setBorder(null);
            setBorder(BorderFactory.createEmptyBorder());
        }

        public void setRoot(List<DefaultMutableTreeNode> root) {
            this.root = root;
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("1111");
            Tree tree = new Tree(treeNode);
            root.forEach(treeNode::add);
            viewport.setView(tree);
        }
    }

}
