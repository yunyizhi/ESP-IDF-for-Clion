package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.icons.AllIcons;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.apache.commons.collections.CollectionUtils;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigStatus;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.util.SysConf.$sys;
import static org.btik.espidf.util.UIUtils.createConstraints;

/**
 * @author lustre
 * @since 2025/6/13 23:04
 */
public class EspIdfMenuConfigPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(EspIdfMenuConfigPanel.class);
    private final Project project;
    private final JButton startConfServer = new JButton();
    private final JBTextField searchInputBox = new JBTextField();
    private final JButton search = new JButton();
    private final JButton discard = new JButton();
    private final JButton reset = new JButton();
    private final JButton save = new JButton();

    private final KconfigTreePanel kconfigTreePanel;
    private final KconfigContentPanel contentPanel;

    private final KConfServer kconfServer;
    private boolean initOk = false;
    List<ConfModel> confModels;
    Map<String, Object> sdkConfig;


    public EspIdfMenuConfigPanel(Project project) {
        super(new BorderLayout());
        setBorder(null);
        this.project = project;
        initToolBar();
        kconfServer = new KConfServer(project, this::onKConfMsg);
        kconfigTreePanel = new KconfigTreePanel();
        kconfigTreePanel.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        kconfigTreePanel.setPreferredSize(new Dimension(350, Integer.MAX_VALUE));

        add(kconfigTreePanel, BorderLayout.WEST);
        CardLayout cardLayout = new CardLayout();
        JPanel contentCards = new JPanel(cardLayout);
        contentPanel = new KconfigContentPanel(contentCards, cardLayout);
        add(contentPanel, BorderLayout.CENTER);
        kconfigTreePanel.addTreeSelectionListener(this::onTreeCheck);
    }

    private void initToolBar() {
        JPanel toolBar = new JPanel(new GridLayoutManager(1, 6, JBUI.insets(16, 16, 0, 16), -1, -1));
        int index = 0;
        toolBar.add(startConfServer, createConstraints(0, index++));
        startConfServer.setMinimumSize(new Dimension(0, 0));
        startConfServer.setIcon(AllIcons.Actions.Execute);
        GridConstraints searchBoxConstraint = createConstraints(0, index++);
        searchBoxConstraint.setFill(GridConstraints.FILL_HORIZONTAL);
        searchBoxConstraint.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        toolBar.add(searchInputBox, searchBoxConstraint);
        toolBar.add(search,  createConstraints(0, index++));
        search.setText("Search");
        toolBar.add(discard,  createConstraints(0, index++));
        discard.setText("Discard");
        toolBar.add(reset,  createConstraints(0, index++));
        reset.setText("Reset");
        toolBar.add(save,  createConstraints(0, index));
        save.setText("Save");
        toolBar.setBorder(null);
        add(toolBar, BorderLayout.NORTH);
        startConfServer.addActionListener(e -> {
            startConfServer.setEnabled(false);
            if (!initOk) {
                startConfServer.setIcon(AllIcons.Run.Stop);
                loadPage();
            }else {
                startConfServer.setIcon(AllIcons.Actions.Execute);
            }
        });
    }

    private void onTreeCheck(TreeSelectionEvent e, ConfModel confModel) {
        if (CollectionUtils.isEmpty(confModel.getChildren())) {
            return;
        }
        contentPanel.showCard(confModel);
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
            KConfParser.treeEach(confModel, (item) -> {
                Map<String, Boolean> visible = status.getVisible();
                String id = item.getId();
                if (visible.containsKey(id)) {
                    if (!visible.get(id)) {
                        item.setVisible(false);
                    }
                } else {
                    item.setVisible(false);
                }
                if (CollectionUtils.isEmpty(item.getChildren())) {
                    item.setLeaf(true);
                }
            });
        }
        List<DefaultMutableTreeNode> root = confModels.stream()
                .map(KConfParser::buildTree)
                .filter(Objects::nonNull)
                .toList();
        ApplicationManager.getApplication().invokeLater(() -> kconfigTreePanel.setRoot(root));

    }

    static class KconfigContentPanel extends JScrollPane {
        private CardLayout cardLayout;
        private JPanel contentCards;
        private HashMap<String, Component> viewMap = new HashMap<>();

        public KconfigContentPanel(JPanel view, CardLayout cardLayout) {
            super(view);
            this.contentCards = view;
            this.cardLayout = cardLayout;
            getVerticalScrollBar().setUnitIncrement(16);
        }

        public void addToCard(Component comp, Object constraints) {
            contentCards.add(comp, constraints);
        }

        public void showCard(ConfModel confModel) {
            if (!viewMap.containsKey(confModel.getId())) {
                // todo 创建新的组件
            }
            cardLayout.show(contentCards, confModel.getId());
        }

    }


    static class KconfigTreePanel extends JScrollPane {
        List<DefaultMutableTreeNode> root;
        private final Tree tree;
        private final DefaultMutableTreeNode rootNode;

        public KconfigTreePanel() {
            viewport.setBorder(null);
            setBorder(BorderFactory.createEmptyBorder());
            rootNode = new DefaultMutableTreeNode("Menu Config");
            tree = new Tree(rootNode);
        }

        public void setRoot(List<DefaultMutableTreeNode> root) {
            this.root = root;
            tree.expandRow(0);
            root.forEach(rootNode::add);
            viewport.setView(tree);
        }

        public void addTreeSelectionListener(TreeChoseListener<ConfModel> treeChoseListener) {
            tree.addTreeSelectionListener(e -> {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (selectedNode == null) {
                    return;
                }
                Object userObject = selectedNode.getUserObject();
                if (userObject instanceof ConfModel confModel) {
                    treeChoseListener.checkedTree(e, confModel);
                }
            });
        }
    }

}
