package org.btik.espidf.project.generator.toolchain;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.intellij.platform.ide.progress.TaskCancellation;
import com.intellij.platform.ide.progress.TasksKt;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.util.CmdTaskExecutor;
import org.btik.espidf.util.EnvironmentVarUtil;
import org.btik.espidf.util.ToolChainTool;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.jetbrains.cidr.cpp.toolchains.CPPToolSet.Kind.SYSTEM_UNIX_TOOLSET;
import static com.jetbrains.cidr.cpp.toolchains.CPPToolSet.Kind.SYSTEM_WINDOWS_TOOLSET;
import static org.btik.espidf.service.IdfEnvironmentService.*;
import static org.btik.espidf.util.ListCellRendererAttr.BLUE_ITALIC_SMALL_ATTRIBUTES;
import static org.btik.espidf.util.ListCellRendererAttr.GRAY_ITALIC_SMALL_ATTRIBUTES;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;
import static org.btik.espidf.util.ToolChainTool.toolChainEnvByComp;

public class IdfToolchainCombBox extends ComboBox<IdfToolchain> {

    private static final HashMap<CPPToolchains.Toolchain, IdfToolchain> toolchainEnvMap = new HashMap<>();

    private static final HashSet<CPPToolchains.Toolchain> envFileNotIdfToolchains = new HashSet<>();

    public IdfToolchainCombBox() {
        setRenderer(new IdfToolchainListCellRenderer());
        setEditable(false);
        setLightWeightPopupEnabled(true);
        addPopupMenuListener(new PopupMenuListenerAdapter() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                Object selectedItem = getSelectedItem();
                IdfToolchain lastSelectedItem = null;
                if (selectedItem instanceof IdfToolchain oldSelected) {
                    lastSelectedItem = oldSelected;
                }
                load();
                if (lastSelectedItem != null) {
                    setSelectedItem(lastSelectedItem);
                }
            }
        });

    }

    private void syncMapToItems() {
        removeAllItems();
        toolchainEnvMap.forEach((toolchain, toolchainInfo) -> addItem(toolchainInfo));
    }

    public void load() {
        final var toolSetKind = IS_WINDOWS ? SYSTEM_WINDOWS_TOOLSET : SYSTEM_UNIX_TOOLSET;
        List<CPPToolchains.Toolchain> envToolchains = ToolChainTool.getFilteredToolchains(
                (toolchain) -> (!toolchainEnvMap.containsKey(toolchain))
                        && (!envFileNotIdfToolchains.contains(toolchain))
                        && StringUtils.isNotEmpty(toolchain.getEnvironment())
                        && toolSetKind == toolchain.getToolSet().getKind()
        );
        for (CPPToolchains.Toolchain envToolchain : envToolchains) {
            loadToolchain(envToolchain);
        }
        syncMapToItems();
    }

    private IdfToolchain loadToolchain(CPPToolchains.Toolchain toolchain) {
        if (toolchainEnvMap.containsKey(toolchain)) {
            return null;
        }
        Map<String, String> rawEnv = toolChainEnvByComp(toolchain, IdfToolchainCombBox.this);
        String idfPath = rawEnv.get(IDF_PATH);
        if (StringUtils.isEmpty(idfPath)) {
            envFileNotIdfToolchains.add(toolchain);
            return null;
        }

        String versionStr = getVersion(rawEnv);
        if (StringUtils.isEmpty(versionStr)) {
            versionStr = "idf" + rawEnv.get(ESP_IDF_VERSION);
        }
        String idfToolsPath = rawEnv.get(IDF_TOOLS_PATH);
        IdfToolchain idfToolchain = new IdfToolchain(toolchain, versionStr, idfPath, idfToolsPath, rawEnv);
        String adfPath = rawEnv.get(ADF_PATH);
        if (StringUtils.isNotEmpty(adfPath)) {
            idfToolchain.setAdfPath(adfPath);
        }
        toolchainEnvMap.put(toolchain, idfToolchain);
        return idfToolchain;
    }

    public void setSelectedToolchain(String lastEnvFile) {
        CPPToolchains.Toolchain toolChain = ToolChainTool.findToolchainByEnvFile(lastEnvFile);
        selectToolchain(toolChain);
    }

    public void selectToolchain(CPPToolchains.Toolchain toolchain) {
        if (toolchain == null) {
            return;
        }
        IdfToolchain idfToolchain = loadToolchain(toolchain);
        syncMapToItems();
        setSelectedItem(idfToolchain);
    }

    private String getVersion(Map<String, String> environments) {
        GeneralCommandLine readVersion = new GeneralCommandLine();
        readVersion.withEnvironment(environments);
        readVersion.setExePath(EnvironmentVarUtil.findIdfFullPath(environments));
        readVersion.addParameters("--version");
        return TasksKt.runWithModalProgressBlocking(ModalTaskOwner.component(this), "Read Version", TaskCancellation.nonCancellable(),
                (scope, continuation) -> CmdTaskExecutor.exeGetStdOut(readVersion, 60 * 1000));

    }

    static class IdfToolchainListCellRenderer implements ListCellRenderer<IdfToolchain> {

        @Override
        public Component getListCellRendererComponent(JList<? extends IdfToolchain> list, IdfToolchain idfToolchain, int index, boolean isSelected, boolean cellHasFocus) {
            if (idfToolchain == null) {
                return new JPanel(new BorderLayout());
            }
            JPanel panel = new JPanel(new BorderLayout());
            panel.getAccessibleContext().setAccessibleName(idfToolchain.getName());
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            Color bg = isSelected ? list.getSelectionBackground() : list.getBackground();
            panel.setBackground(bg);

            SimpleColoredComponent primary = new SimpleColoredComponent();
            primary.setOpaque(false); // 透明背景，继承 panel 背景
            primary.setIpad(JBUI.emptyInsets());
            primary.append(idfToolchain.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            primary.append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            primary.append(idfToolchain.getIdfVersion(), BLUE_ITALIC_SMALL_ATTRIBUTES);
            SimpleColoredComponent secondary = new SimpleColoredComponent();
            secondary.setOpaque(false);
            secondary.setIpad(JBUI.emptyInsets()); // 更小内边距
            String path = idfToolchain.getAdfPath(); // 优先展示 ADF 路径
            if (StringUtils.isEmpty(path)) {
                path = idfToolchain.getIdfPath();
            }
            secondary.append(path, GRAY_ITALIC_SMALL_ATTRIBUTES);
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(primary);
            textPanel.add(secondary);

            panel.add(textPanel, BorderLayout.CENTER);
            return panel;
        }
    }
}
