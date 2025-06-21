package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import org.apache.commons.collections.CollectionUtils;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigStatus;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2025/6/13 23:04
 */
public class EspIdfMenuConfigPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(EspIdfMenuConfigPanel.class);
    private final Project project;

    private final KconfigTreePanel kconfigTreePanel;

    private KConfServer kconfServer;
    private boolean initOk = false;
    List<ConfModel> confModels;
    Map<String, Object> sdkConfig;


    public EspIdfMenuConfigPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        kconfServer = new KConfServer(project, this::onKConfMsg);
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
        Path menuConfigPath = Path.of(basePath, cmakeBuildDir, $sys("esp.idf.kconfig.menus.dir")).resolve($sys("esp.idf.kconfig.menus.file"));
        if (!menuConfigPath.toFile().exists()) {
            LOG.error(menuConfigPath + " does not exist");
            return;
        }
        confModels = KConfParser.parseKconfig(menuConfigPath);
        Path sdkConfigPath = Path.of(basePath, cmakeBuildDir, $sys("esp.idf.kconfig.menus.dir")).resolve($sys("esp.idf.kconfig.sdk.config.file"));
        if (!sdkConfigPath.toFile().exists()) {
            LOG.error(sdkConfigPath + " does not exist");
            return;
        }
        sdkConfig = KConfParser.parseSdkConfig(sdkConfigPath);
        kconfServer.start();
    }

    private void onKConfMsg(KconfigStatus status) {
        if (!initOk) {
            initOk = true;
            onInitOk(status);
        }
    }

    private void onInitOk(KconfigStatus status) {
        for (ConfModel confModel : confModels) {
            KConfParser.treeEach(confModel, (item)->{
                Map<String, Boolean> visible = status.getVisible();
                String id = item.getId();
                if (visible.containsKey(id)) {
                    if (!visible.get(id)) {
                        item.setVisible(false);
                    }
                }else {
                    item.setVisible(false);
                }
                if (CollectionUtils.isEmpty(item.getChildren())) {
                    // 叶子节点留倒数第二级在树上，真叶子节点作为另一个面板
                    ConfModel parent = item.getParent();
                    if (parent != null) {
                        parent.setLeaf(true);
                    }
                }
            });
        }
        List<DefaultMutableTreeNode> root = confModels.stream()
                .map(KConfParser::buildTree)
                .filter(Objects::nonNull)
                .toList();
        kconfigTreePanel.setRoot(root);
    }


    static class KconfigTreePanel extends JScrollPane {
        List<DefaultMutableTreeNode> root;

        public KconfigTreePanel() {
            viewport.setBorder(null);
            setBorder(BorderFactory.createEmptyBorder());
        }

        public void setRoot(List<DefaultMutableTreeNode> root) {
            this.root = root;
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("Menu Config");
            Tree tree = new Tree(treeNode);
            tree.expandRow(0);
            root.forEach(treeNode::add);
            viewport.setView(tree);
        }
    }

}
