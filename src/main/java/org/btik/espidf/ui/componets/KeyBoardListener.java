package org.btik.espidf.ui.componets;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

/**
 * @author lustre
 * @since 2025/7/4 0:20
 */
public class KeyBoardListener implements KeyListener {

    private Consumer<KeyEvent> keyTypedCB;
    private Consumer<KeyEvent> keyPressedCB;
    private Consumer<KeyEvent> keyReleasedCB;

    @Override
    public void keyTyped(KeyEvent e) {
        if (keyTypedCB != null) {
            keyTypedCB.accept(e);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (keyPressedCB != null) {
            keyPressedCB.accept(e);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (keyReleasedCB != null) {
            keyReleasedCB.accept(e);
        }
    }

    public KeyBoardListener withKeyTypedCB(Consumer<KeyEvent> keyTypedCB) {
        this.keyTypedCB = keyTypedCB;
        return this;
    }

    public KeyBoardListener withKeyPressedCB(Consumer<KeyEvent> keyPressedCB) {
        this.keyPressedCB = keyPressedCB;
        return this;
    }

    public KeyBoardListener withKeyReleasedCB(Consumer<KeyEvent> keyReleasedCB) {
        this.keyReleasedCB = keyReleasedCB;
        return this;
    }
}
