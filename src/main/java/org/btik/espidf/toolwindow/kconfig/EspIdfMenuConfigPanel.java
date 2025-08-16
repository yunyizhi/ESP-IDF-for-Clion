package org.btik.espidf.toolwindow.kconfig;

import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.icons.AllIcons;
import org.apache.commons.collections.CollectionUtils;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigSetCommand;
import org.btik.espidf.toolwindow.kconfig.model.KconfigStatus;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;
import org.btik.espidf.util.TreeUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;

import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static org.btik.espidf.util.I18nMessage.$i18n;
import static org.btik.espidf.util.SysConf.$sys;
import static org.btik.espidf.util.SysConf.$sysInt;
import static org.btik.espidf.util.UIUtils.setWidth;

/**
 * @author lustre
 * @since 2025/6/13 23:04
 */
public class EspIdfMenuConfigPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(EspIdfMenuConfigPanel.class);
    private final Project project;
    private final SearchTextBox searchInputBox = new SearchTextBox();
    private final KconfServerAction kconfServerAction = new KconfServerAction();
    private final KconfigTreePanel kconfigTreePanel;
    private final KconfigContentPanel contentPanel;

    private final KConfServer kconfServer;
    private boolean initOk = false;
    private final ConfModel treeRootModel = new ConfModel();
    List<ConfModel> confModels;
    HashMap<String, ConfModel> confModelMap = new HashMap<>();
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

        kconfServer = new KConfServer(project, this::onKConfMsg);

        kconfigTreePanel = new KconfigTreePanel(treeRootModel, this::sendCmd);
        kconfigTreePanel.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        kconfigTreePanel.setPreferredSize(new Dimension(350, Integer.MAX_VALUE));

        add(kconfigTreePanel, BorderLayout.WEST);
        CardLayout cardLayout = new CardLayout();
        JPanel contentCards = new JPanel(cardLayout);
        contentPanel = new KconfigContentPanel(contentCards, cardLayout, this::sendCmd);
        add(contentPanel, BorderLayout.CENTER);
        kconfigTreePanel.addTreeSelectionListener(this::onTreeCheck);
        kconfServer.setOnStopCallback(() -> {
            initOk = false;
            ApplicationManager.getApplication().invokeLater(() -> {
                kconfServerAction.setStatus(false);
                contentPanel.clear();
                kconfigTreePanel.clear();
                treeRootModel.cutChain();
                contentPanel.updateUI();
            });
        });
        initToolBar();
    }

    private void sendCmd(KconfigSetCommand kconfigSetCommand) {
        Gson gson = new Gson();
        String json = gson.toJson(kconfigSetCommand);
        kconfServer.sendCommand(json);
    }


    private void initToolBar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ActionToolbar runToolBar = getRunToolbar();
        runToolBar.setTargetComponent(toolBar);
        toolBar.add(runToolBar.getComponent());
        setWidth(searchInputBox, 300);
        toolBar.add(searchInputBox);
        ActionToolbar actionToolbar = getActionToolbar(toolBar);
        toolBar.add(actionToolbar.getComponent());
        toolBar.setBorder(null);
        add(toolBar, BorderLayout.NORTH);
        searchInputBox.setMaxSearchCount($sysInt("esp.idf.kconfig.search.count", 50));
        searchInputBox.init(treeRootModel, kconfigTreePanel::jumpTo);
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
        kconfServerAction.setCallback(() -> {
            if (!kconfServerAction.isRunning()) {
                kconfigTreePanel.setVisible(true);
                contentPanel.setVisible(true);
                kconfServerAction.setStatus(true);
                loadPage();
            } else {
                kconfServer.stop();
            }
        });
        ActionGroup actionGroup = new DefaultActionGroup(kconfServerAction);
        return actionManager.createActionToolbar(ActionPlaces.TOOLWINDOW_TOOLBAR_BAR, actionGroup, true);
    }


    private void onTreeCheck(TreeSelectionEvent e, ConfModel confModel) {
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
            TreeUtils.treeEach(confModel, (item) -> {
                confModelMap.put(item.getId(), item);
                if (item.getType() == KconfigType.BOOL && CollectionUtils.isNotEmpty(item.getChildren())) {
                    item.setRedefinedType(KconfigType.ENABLE_SWITCH);
                }
            });
        }
        for (ConfModel confModel : confModels) {
            confModel.setParent(treeRootModel);
            TreeUtils.eachWithParent(confModel, (parent, child) -> {
                child.setParent(parent);
                if (parent.getType() == KconfigType.CHOICE) {
                    child.setRedefinedType(KconfigType.CHOICE_ITEM);
                }
            });
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
            kconfServerAction.setStatus(initOk);
        } else {
            if (status.isError()) {
                LOG.warn(status.getError().toString());
                return;
            }
            Map<String, Object> values = status.getValues();
            values.forEach((key, value) -> {
                ConfModel confModel = confModelMap.get(key);
                if (confModel == null) {
                    return;
                }
                confModel.setValue(value);
            });
            ApplicationManager.getApplication().invokeLater(() -> {
                kconfigTreePanel.onConfigNodesChange(status, confModelMap);
                contentPanel.onConfigNodesChange(status, confModelMap);
            });
        }
    }

    private void onInitOk(KconfigStatus status) {
        Map<String, Boolean> visible = status.getVisible();
        Map<String, Object> values = status.getValues();
        for (ConfModel confModel : confModels) {
            TreeUtils.treeEach(confModel, (item) -> {

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
                if (item.isPanelItem()) {
                    setAsMenuItem(item);
                }
            });
        }
        ApplicationManager.getApplication().invokeLater(kconfigTreePanel::onConfigNodesInit);

    }

    private static void setAsMenuItem(ConfModel item) {
        item.setAsMenuPanelItem(true);
        ConfModel parent = item.getParent();
        if (parent != null) {
            parent.setHasPanelItem(true);
        }
    }

}
