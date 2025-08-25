package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.awt.RelativePoint;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.btik.espidf.ui.componets.KeyBoardListener;
import org.btik.espidf.util.TreeUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author lustre
 * @since 2025/7/7 22:57
 */
public class SearchTextBox extends SearchTextField {
    private Consumer<ConfModel> jumpTreeFunc;

    private ListPopup popup;

    private boolean popupClosed = false;
    private ConfModel treeRootModel;
    private int lastIndex = 0;
    private int maxSearchCount = 10;

    private final Project project;

    public SearchTextBox(Project project) {
        this.project = project;
    }

    @Override
    protected void onFieldCleared() {
        disposePopup();
    }

    private void disposePopup() {
        ListPopup popup = this.popup;
        if (popup != null && !popup.isDisposed()) {
            popup.dispose();
        }
        this.popup = null;
    }

    private LinkedHashSet<ConfModel> search() {
        LinkedHashSet<ConfModel> results = new LinkedHashSet<>();
        String text = getText();
        String lowerKeyword = text.toLowerCase();
        TreeUtils.treeEachWithBreak(treeRootModel, (model) -> {
            if (results.size() >= maxSearchCount) {
                return false;
            }
            boolean isMatch = isMatch(model, lowerKeyword);
            if (isMatch) {
                results.add(model);
            }
            return results.size() < maxSearchCount;
        });
        return results;
    }

    private void showSearchResult(Collection<ConfModel> result) {
        BaseListPopupStep<ConfModel> baseListPopupStep = new BaseListPopupStep<>("Search Result",
                result.stream().toList());
        ListPopup popup = JBPopupFactory.getInstance().createListPopup(project, baseListPopupStep,
                (listCellRenderer -> new SearchResultCellRenderer())
        );
        popupClosed = false;
        popup.setRequestFocus(false);
        this.popup = popup;
        lastIndex = 0;
        RelativePoint pos = RelativePoint.getSouthWestOf(this);
        popup.showInScreenCoordinates(this, pos.getScreenPoint());
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                popupClosed = true;
                if (!event.isOk()) {
                    return;
                }
                List<ConfModel> values = baseListPopupStep.getValues();
                if (values.isEmpty()) {
                    return;
                }
                ConfModel confModel = values.get(lastIndex);
                jumpTreeFunc.accept(confModel);
            }
        });
        popup.addListSelectionListener(e -> lastIndex = e.getLastIndex());
    }

    private boolean isMatch(ConfModel model, String keyword) {
        if (keyword == null || keyword.isEmpty()) return false;
        if (!model.isVisible()) {
            return false;
        }
        return (model.getId() != null && model.getId().toLowerCase().contains(keyword)) ||
                (model.getName() != null && model.getName().toLowerCase().contains(keyword)) ||
                (model.getTitle() != null && model.getTitle().toLowerCase().contains(keyword));
    }

    public void init(@NotNull ConfModel treeRootModel, @NotNull Consumer<ConfModel> jumpTreeFunc) {
        this.jumpTreeFunc = jumpTreeFunc;
        this.treeRootModel = treeRootModel;
        addKeyboardListener(new KeyBoardListener().withKeyPressedCB((e) -> {
            if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                return;
            }
            if (!popupClosed) {
                return;
            }
            showSearchResult(search());
        }));
        addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                showSearchResult(search());
            }
        });
    }

    public void setMaxSearchCount(int maxSearchCount) {
        this.maxSearchCount = maxSearchCount;
    }
}
