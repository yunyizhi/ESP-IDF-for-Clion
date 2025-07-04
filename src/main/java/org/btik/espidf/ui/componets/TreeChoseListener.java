package org.btik.espidf.ui.componets;

import javax.swing.event.TreeSelectionEvent;

/**
 * @author lustre
 * @since 2025/6/22 20:18
 */
public interface TreeChoseListener <T>{
    void checkedTree(TreeSelectionEvent e, T userObj);
}
