package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.toolwindow.kconfig.model.KconfigSetCommand;
import org.btik.espidf.toolwindow.kconfig.model.KconfigType;
import org.btik.espidf.ui.componets.HexTextField;
import org.btik.espidf.ui.componets.InsertPanel;
import org.btik.espidf.ui.componets.LongTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static org.btik.espidf.toolwindow.kconfig.model.KconfigType.*;

public class KConfPanelFactory {
    static class PanelItem {

        JComponent itemWrapper;
        JComponent focusPointer;

        public PanelItem(JComponent itemWrapper, JComponent focusPointer) {
            this.itemWrapper = itemWrapper;
            this.focusPointer = focusPointer;
        }
    }

    interface ItemCreator {
        PanelItem create(ConfModel confModel, Consumer<KconfigSetCommand> commandSender);
    }

    private static final HashMap<KconfigType, ItemCreator> creators = new HashMap<>();

    static {
        creators.put(BOOL, KConfPanelFactory::boolItemCreator);
        creators.put(CHOICE, KConfPanelFactory::selectItemCreator);
        creators.put(HEX, KConfPanelFactory::hexCreator);
        creators.put(INT, KConfPanelFactory::intCreator);
        creators.put(STRING, KConfPanelFactory::stringCreator);
        creators.put(MENU, KConfPanelFactory::menuItemCreator);
    }

    public static Component createKConfPanel(ConfModel confModel, Consumer<KconfigSetCommand> commandSender, HashMap<String, JComponent> confItemComponentMap) {
        List<ConfModel> children = confModel.getChildren();
        List<ConfModel> filteredList = children.stream()
                .filter(ConfModel::isVisible)
                .filter(ConfModel::isAsMenuPanelItem)
                .toList();
        if (filteredList.isEmpty()) {
            return null;
        }
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        addTitle(panel, confModel, confItemComponentMap);
        for (ConfModel child : filteredList) {
            KconfigType type = child.getType();
            ItemCreator itemCreator = creators.get(type);
            if (itemCreator == null) {
                System.out.println(child.getId());
                continue;
            }
            PanelItem item = itemCreator.create(child, commandSender);
            item.itemWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(item.itemWrapper);
            confItemComponentMap.put(child.getId(), item.focusPointer);
        }
        return panel;
    }

    private static void addTitle(JPanel panel, ConfModel confModel, HashMap<String, JComponent> confItemComponentMap) {
        JLabel jLabel = new JLabel(confModel.getTitle());

        jLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        addComponent(wrapper, jLabel, 0, 0);
        wrapper.setMaximumSize(wrapper.getPreferredSize());
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        confItemComponentMap.put(confModel.getId(), jLabel);
        panel.add(wrapper);
    }

    private static void addComponent(JPanel panel, Component component, int gridx, int gridy) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(2, 10, 0, 0); // 内边距
        panel.add(component, gbc);
    }

    private static PanelItem boolItemCreator(ConfModel confModel, Consumer<KconfigSetCommand> commandSender) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JCheckBox comp = new JCheckBox(confModel.getTitle());
        comp.setToolTipText(confModel.getHelp());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(2, 10, 0, 0); // 内边距
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        wrapper.add(comp, gbc);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        Object value = confModel.getValue();
        if (value instanceof Boolean) {
            comp.setSelected((Boolean) value);
        }
        comp.addActionListener(e -> {
            KconfigSetCommand kconfigSetCommand = new KconfigSetCommand();
            kconfigSetCommand.setValues(Map.of(confModel.getId(), comp.isSelected()));
            commandSender.accept(kconfigSetCommand);
        });
        return new PanelItem(wrapper, comp);
    }

    private static PanelItem selectItemCreator(ConfModel confModel, Consumer<KconfigSetCommand> commandSender) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JLabel label = new JLabel(confModel.getTitle() + ":");
        label.setToolTipText(confModel.getHelp());

        ComboBox<ConfModel> comboBox = new ComboBox<>();
        List<ConfModel> children = confModel.getChildren();
        FontMetrics fm = comboBox.getFontMetrics(comboBox.getFont());
        int width = 200;
        final int paddingAndLogo = 50;
        for (int i = 0; i < children.size(); i++) {
            ConfModel child = children.get(i);
            if (child.getRedefinedType() == CHOICE_ITEM) {
                comboBox.addItem(child);
                Object value = child.getValue();
                if (value instanceof Boolean checked && checked) {
                    comboBox.setSelectedIndex(i);
                }
                width = Math.max(fm.stringWidth(child.toString()) + paddingAndLogo, width);
            }
        }
        comboBox.setPreferredSize(new Dimension(width, 30));
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        addComponent(wrapper, label, 0, 0);
        addComponent(wrapper, comboBox, 0, 1);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        comboBox.addItemListener((e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            Object item = e.getItem();
            if (!(item instanceof ConfModel selectedMode)){
                return;
            }
            KconfigSetCommand kconfigSetCommand = new KconfigSetCommand();
            kconfigSetCommand.setValues(Map.of(selectedMode.getId(), true));
            commandSender.accept(kconfigSetCommand);
        }));
        return new PanelItem(wrapper, comboBox);
    }

    private static PanelItem hexCreator(ConfModel confModel, Consumer<KconfigSetCommand> commandSender) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JLabel label = new JLabel(confModel.getTitle() + ":");
        label.setToolTipText(confModel.getHelp());

        HexTextField textField = new HexTextField();
        textField.setPreferredSize(new Dimension(200, 30));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        addComponent(wrapper, label, 0, 0);
        addComponent(wrapper, textField, 0, 1);
        Object value = confModel.getValue();
        String strValue;
        if (value instanceof Integer intValue) {
            strValue = "0x%x".formatted(intValue);
            textField.setText(strValue);
        } else if (value instanceof Long longValue) {
            strValue = "0x%x".formatted(longValue);
            textField.setText(strValue);
        } else {
            strValue = null;
        }
        wrapper.setMaximumSize(wrapper.getPreferredSize());
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (!textField.isValid()) {
                    textField.setText(strValue == null ? "" : strValue);
                    return;
                }
                String text = textField.getText();
                if (Objects.equals(strValue, text)) {
                    return;
                }
                KconfigSetCommand kconfigSetCommand = new KconfigSetCommand();
                kconfigSetCommand.setValues(Map.of(confModel.getId(), text));
                commandSender.accept(kconfigSetCommand);
            }
        });
        return new PanelItem(wrapper, textField);
    }

    private static PanelItem intCreator(ConfModel confModel, Consumer<KconfigSetCommand> commandSender) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JLabel label = new JLabel(confModel.getTitle() + ":");
        label.setToolTipText(confModel.getHelp());

        LongTextField longField = new LongTextField();
        longField.setPreferredSize(new Dimension(200, 30));
        longField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        Object value = confModel.getValue();
        Long longVal;
        if (value instanceof Integer integer) {
            longVal = (long) integer;
            longField.setLongValue(longVal);
        } else if (value instanceof Long long_) {
            longVal = long_;
            longField.setLongValue(longVal);
        } else {
            longVal = null;
        }
        long[] range = confModel.getRange();
        if (range != null) {
            longField.setRange(range[0], range[1]);
            longField.setToolTipText("range [%d, %d]".formatted(range[0], range[1]));
        }
        addComponent(wrapper, label, 0, 0);
        addComponent(wrapper, longField, 0, 1);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        longField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                try {
                    long longValue = longField.getLongValue();
                    if (Objects.equals(longValue, longVal)) {
                        return;
                    }
                    KconfigSetCommand kconfigSetCommand = new KconfigSetCommand();
                    kconfigSetCommand.setValues(Map.of(confModel.getId(), longValue));
                    commandSender.accept(kconfigSetCommand);
                } catch (NumberFormatException ignored) {
                    if (value instanceof Integer || value instanceof Long) {
                        longField.setText(String.valueOf(value));
                    }
                }
            }
        });
        return new PanelItem(wrapper, longField);
    }

    private static PanelItem stringCreator(ConfModel confModel, Consumer<KconfigSetCommand> commandSender) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JLabel label = new JLabel(confModel.getTitle() + ":");
        label.setToolTipText(confModel.getHelp());

        JBTextField textField = new JBTextField();
        textField.setPreferredSize(new Dimension(200, 30));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        Object value = confModel.getValue();
        if (value instanceof String) {
            textField.setText((String) value);
        }
        addComponent(wrapper, label, 0, 0);
        addComponent(wrapper, textField, 0, 1);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                String text = textField.getText();
                if (Objects.equals(String.valueOf(value), text)) {
                    return;
                }
                KconfigSetCommand kconfigSetCommand = new KconfigSetCommand();
                kconfigSetCommand.setValues(Map.of(confModel.getId(), text));
                commandSender.accept(kconfigSetCommand);
            }
        });
        return new PanelItem(wrapper, textField);
    }

    private static PanelItem menuItemCreator(ConfModel confModel, Consumer<KconfigSetCommand> commandSender) {
        InsertPanel wrapper = new InsertPanel(new GridBagLayout());
        wrapper.setInsets(JBUI.insetsTop(8));
        JCheckBox comp = new JCheckBox(confModel.getTitle());
        comp.setToolTipText(confModel.getHelp());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(2, 10, 0, 0); // 内边距
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        wrapper.add(comp, gbc);

        wrapper.setMaximumSize(wrapper.getPreferredSize());
        return new PanelItem(wrapper, comp);
    }
}