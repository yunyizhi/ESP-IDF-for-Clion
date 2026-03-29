package org.btik.espidf.project.generator.toolchain;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Pair;
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
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.jetbrains.cidr.cpp.toolchains.CPPToolSet.Kind.SYSTEM_UNIX_TOOLSET;
import static com.jetbrains.cidr.cpp.toolchains.CPPToolSet.Kind.SYSTEM_WINDOWS_TOOLSET;
import static org.btik.espidf.adapter.Adapter.readEnvironment;
import static org.btik.espidf.util.I18nMessage.$i18nF;
import static org.btik.espidf.util.ListCellRendererAttr.BLUE_ITALIC_SMALL_ATTRIBUTES;
import static org.btik.espidf.util.ListCellRendererAttr.GRAY_ITALIC_SMALL_ATTRIBUTES;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;

public class IdfToolchainCombBox extends ComboBox<IdfToolchain> {

    private static final HashMap<CPPToolchains.Toolchain, Pair<Map<String, String>, IdfToolchain>> toolchainEnvMap = new HashMap<>();

    private static final HashSet<CPPToolchains.Toolchain> envFileNotIdfToolchains = new HashSet<>();

    private final IdfToolchainComboEditor editor;

    public IdfToolchainCombBox() {
        setRenderer(new IdfToolchainListCellRenderer());
        setEditable(false);
        setLightWeightPopupEnabled(true);
        editor = new IdfToolchainComboEditor();
        setEditor(editor);
        addPopupMenuListener(new PopupMenuListenerAdapter() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                Object selectedItem = getSelectedItem();
                IdfToolchain lastSelectedItem = null;
                if (selectedItem instanceof IdfToolchain oldSelected) {
                    lastSelectedItem = oldSelected;
                }
                removeAllItems();
                if (lastSelectedItem != null) {
                    editor.setItem(lastSelectedItem);
                }
                load();
            }


        });

    }

    public void load() {
        final var toolSetKind = IS_WINDOWS ? SYSTEM_WINDOWS_TOOLSET : SYSTEM_UNIX_TOOLSET;
        List<CPPToolchains.Toolchain> envToolchains = ToolChainTool.getFilteredToolchains(
                (toolchain) -> (!toolchainEnvMap.containsKey(toolchain))
                        && (!envFileNotIdfToolchains.contains(toolchain))
                        && StringUtils.isNoneEmpty(toolchain.getEnvironment())
                        && toolSetKind == toolchain.getToolSet().getKind()
        );
        for (CPPToolchains.Toolchain envToolchain : envToolchains) {
            if (toolchainEnvMap.containsKey(envToolchain)) {
                continue;
            }
            Map<String, String> rawEnv = TasksKt.runWithModalProgressBlocking(ModalTaskOwner.component(IdfToolchainCombBox.this),
                    $i18nF("esp.idf.read.envs", envToolchain.getName()),
                    TaskCancellation.nonCancellable(), (scope, continuation) -> {
                        try {
                            return readEnvironment(envToolchain, envToolchain.getEnvironment());
                        } catch (IOException | com.intellij.execution.ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    });
            String idfPath = rawEnv.get("IDF_PATH");
            if (StringUtils.isEmpty(idfPath)) {
                envFileNotIdfToolchains.add(envToolchain);
                continue;
            }
            String versionStr = getVersion(rawEnv);
            if (StringUtils.isEmpty(versionStr)) {
                versionStr = "idf" + rawEnv.get("ESP_IDF_VERSION");
            }

            IdfToolchain idfToolchain = new IdfToolchain(envToolchain.getName(), envToolchain.getEnvironment(), versionStr, idfPath);
            toolchainEnvMap.put(envToolchain, Pair.create(rawEnv, idfToolchain));
        }

        toolchainEnvMap.forEach((toolchain, toolchainInfo) -> addItem(toolchainInfo.second));
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
            panel.getAccessibleContext().setAccessibleName(idfToolchain.name());
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            Color bg = isSelected ? list.getSelectionBackground() : list.getBackground();
            panel.setBackground(bg);

            SimpleColoredComponent primary = new SimpleColoredComponent();
            primary.setOpaque(false); // 透明背景，继承 panel 背景
            primary.setIpad(JBUI.emptyInsets());
            primary.append(idfToolchain.name(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            primary.append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            primary.append(idfToolchain.idfVersion(), BLUE_ITALIC_SMALL_ATTRIBUTES);
            SimpleColoredComponent secondary = new SimpleColoredComponent();
            secondary.setOpaque(false);
            secondary.setIpad(JBUI.emptyInsets()); // 更小内边距
            secondary.append(idfToolchain.idfPath(), GRAY_ITALIC_SMALL_ATTRIBUTES);
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.add(primary);
            textPanel.add(secondary);

            panel.add(textPanel, BorderLayout.CENTER);
            return panel;
        }
    }

    static class IdfToolchainComboEditor extends BasicComboBoxEditor {
        public JTextField getTextField() {
            return editor;
        }
    }
}
