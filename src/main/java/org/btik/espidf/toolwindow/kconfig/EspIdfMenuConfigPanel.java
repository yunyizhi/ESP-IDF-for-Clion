package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.icons.AllIcons;
import org.apache.commons.collections.CollectionUtils;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigStatus;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static org.btik.espidf.toolwindow.kconfig.model.KconfigType.BOOL;
import static org.btik.espidf.toolwindow.kconfig.model.KconfigType.CHOICE;
import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;

/**
 * @author lustre
 * @since 2025/6/13 23:04
 */
public class EspIdfMenuConfigPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(EspIdfMenuConfigPanel.class);
    private final Project project;
    private final SearchTextField searchInputBox = new SearchTextField();

    private final KconfigTreePanel kconfigTreePanel;
    private final KconfigContentPanel contentPanel;

    private final KConfServer kconfServer;
    private boolean initOk = false;
    private final ConfModel treeRootModel = new ConfModel();
    List<ConfModel> confModels;
    Map<String, Object> sdkConfig;


    public EspIdfMenuConfigPanel(Project project) {
        super(new BorderLayout());
        treeRootModel.setId("EspIdfMenuConfigPanelTreeRoot");
        treeRootModel.setName($i18n("esp.idf.tool.window.sdk.config.root.name"));
        treeRootModel.setTitle($i18n("esp.idf.tool.window.sdk.config.root.name"));
        treeRootModel.setType(KconfigType.MENU);
        treeRootModel.setVisible(true);

        setBorder(null);
        this.project = project;
        initToolBar();
        kconfServer = new KConfServer(project, this::onKConfMsg);
        kconfigTreePanel = new KconfigTreePanel(treeRootModel);
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
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ActionToolbar runToolBar = getRunToolbar();
        runToolBar.setTargetComponent(toolBar);
        toolBar.add(runToolBar.getComponent());
        toolBar.add(searchInputBox);
        ActionToolbar actionToolbar = getActionToolbar(toolBar);
        toolBar.add(actionToolbar.getComponent());
        toolBar.setBorder(null);
        add(toolBar, BorderLayout.NORTH);
        searchInputBox.addKeyboardListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    kconfigTreePanel.filterTree(searchInputBox.getText());
                }
            }
        });
    }

    private static @NotNull ActionToolbar getActionToolbar(JPanel toolBar) {
        var actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = new DefaultActionGroup(new AnAction(AllIcons.General.Reset) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        }, new AnAction(AllIcons.Actions.MenuSaveall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        });
        ActionToolbar actionToolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true);
        actionToolbar.setTargetComponent(toolBar);
        return actionToolbar;
    }

    private @NotNull ActionToolbar getRunToolbar() {
        var actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = new DefaultActionGroup(new AnAction("Start Conf Server", "Start conf server", AllIcons.Actions.Execute) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                if (!initOk) {
                    presentation.setIcon(AllIcons.Run.Stop);
                    presentation.setText("Stop Conf Server");
                    loadPage();
                } else {
                    presentation.setIcon(AllIcons.Actions.Execute);
                    presentation.setText("Start Conf Server");
                }
            }
        });
        return actionManager.createActionToolbar(ActionPlaces.TOOLWINDOW_TOOLBAR_BAR, actionGroup, true);
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
        treeRootModel.setChildren(confModels);
        for (ConfModel confModel : confModels) {
            confModel.setParent(treeRootModel);
            KConfParser.eachWithParent(confModel, (parent, child) -> child.setParent(parent));
        }
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
        Map<String, Boolean> visible = status.getVisible();
        Map<String, Object> values = status.getValues();
        for (ConfModel confModel : confModels) {
            KConfParser.treeEach(confModel, (item) -> {

                String id = item.getId();
                if (visible.containsKey(id)) {
                    if (!visible.get(id)) {
                        item.setVisible(false);
                    }
                } else {
                    item.setVisible(false);
                }
                if (values.containsKey(id)) {
                    item.setValue(values.get(id));
                }
                if (item.getType() == CHOICE || item.isMenuconfig() || CollectionUtils.isEmpty(item.getChildren())) {
                    setAsMenuItem(item);
                }
            });
        }
        List<DefaultMutableTreeNode> root = confModels.stream()
                .map(KConfParser::buildTree)
                .filter(Objects::nonNull)
                .toList();
        ApplicationManager.getApplication().invokeLater(() -> kconfigTreePanel.setRoot(root));

    }

    private static void setAsMenuItem(ConfModel item) {
        item.setAsMenuPanelItem(true);
        ConfModel parent = item.getParent();
        if (parent != null) {
            parent.setHasPanelItem(true);
            if (parent.getType() == BOOL) {
                setAsMenuItem(parent);
            }
        }
    }

    static class KconfigContentPanel extends JScrollPane {
        private final CardLayout cardLayout;
        private final JPanel contentCards;
        private final HashMap<String, Component> viewMap = new HashMap<>();
        static final String EMPTY = "empty";

        public KconfigContentPanel(JPanel view, CardLayout cardLayout) {
            super(view);
            this.contentCards = view;
            this.cardLayout = cardLayout;
            getVerticalScrollBar().setUnitIncrement(16);
            contentCards.add(new JPanel(), EMPTY);
        }

        public void addToCard(Component comp, Object constraints) {
            contentCards.add(comp, constraints);
        }

        private void showEmpty(){
            cardLayout.show(contentCards, EMPTY);
            getViewport().setViewPosition(new Point(0, 0));
        }

        public void showCard(ConfModel confModel) {
            if (!confModel.isHasPanelItem()) {
                showEmpty();
                return;
            }
            if (!viewMap.containsKey(confModel.getId())) {
                Component kConfPanel = KConfPanelFactory.createKConfPanel(confModel);
                if (kConfPanel == null) {
                    showEmpty();
                    return;
                }
                viewMap.put(confModel.getId(), kConfPanel);
                addToCard(kConfPanel, confModel.getId());
            }
            cardLayout.show(contentCards, confModel.getId());
            getViewport().setViewPosition(new Point(0, 0));
        }

    }


    static class KconfigTreePanel extends JScrollPane {
        List<DefaultMutableTreeNode> root;
        List<ConfModel> rootModels = new ArrayList<>();
        private final Tree tree;
        private final DefaultMutableTreeNode rootNode;
        private final ConfModel treeRootModel;
        private final TreeModel defaultTreeModel;
        public KconfigTreePanel(ConfModel treeRootModel) {
            this.treeRootModel = treeRootModel;
            viewport.setBorder(null);
            setBorder(BorderFactory.createEmptyBorder());
            rootNode = new DefaultMutableTreeNode(treeRootModel);
            tree = new Tree(rootNode);
            defaultTreeModel = tree.getModel();
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }

        public void setRoot(List<DefaultMutableTreeNode> root) {
            this.root = root;
            tree.expandRow(0);
            root.forEach((item) -> {
                rootNode.add(item);
                rootModels.add((ConfModel) item.getUserObject());
            });
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
        public void filterTree(String keyword) {

            if (keyword == null || keyword.isEmpty()) {
                tree.setModel(defaultTreeModel);
                return;
            }
            ConfModel targetModel = new ConfModel();
            treeRootModel.copyTo(targetModel);
            boolean hasMatches = filterSubtree(treeRootModel, targetModel, keyword);
            if (hasMatches) {
                tree.setModel(new DefaultTreeModel(KConfParser.buildTree(targetModel)));
                tree.expandRow(0);
            } else {
                DefaultMutableTreeNode emptyRoot = new DefaultMutableTreeNode("无匹配项");
                tree.setModel(new DefaultTreeModel(emptyRoot));
            }
        }

        private boolean filterSubtree(ConfModel sourceModel, ConfModel targetModel, String keyword) {
            if (!sourceModel.isVisible()) {
                return false;
            }
            boolean isMatched = isMatch(sourceModel, keyword);
            boolean hasChildrenMatched = false;
            List<ConfModel> newChildren = new ArrayList<>();

            if (sourceModel.getChildren() != null) {
                for (ConfModel child : sourceModel.getChildren()) {
                    ConfModel newChild = new ConfModel();
                    newChild.setParent(targetModel);

                    boolean childMatched = filterSubtree(child, newChild, keyword);

                    if (childMatched) {
                        newChildren.add(newChild);
                        hasChildrenMatched = true;
                    }
                }
            }

            if (isMatched || hasChildrenMatched) {

                sourceModel.copyTo(targetModel);


                if (!newChildren.isEmpty()) {
                    targetModel.setChildren(newChildren);
                } else {
                    targetModel.setChildren(null);
                }

                return true;
            }

            return false;
        }

        private boolean isMatch(ConfModel model, String keyword) {
            if (keyword == null || keyword.isEmpty()) return true;

            String lowerKeyword = keyword.toLowerCase();
            return (model.getId() != null && model.getId().toLowerCase().contains(lowerKeyword)) ||
                    (model.getName() != null && model.getName().toLowerCase().contains(lowerKeyword)) ||
                    (model.getTitle() != null && model.getTitle().toLowerCase().contains(lowerKeyword));
        }
    }

}
