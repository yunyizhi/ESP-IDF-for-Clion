package org.btik.espidf.ui.componets;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

public class MouseHooks implements MouseListener {
    private Consumer<MouseEvent> clickedCB;
    private Consumer<MouseEvent> pressedCB;
    private Consumer<MouseEvent> releasedCB;
    private Consumer<MouseEvent> enteredCB;
    private Consumer<MouseEvent> exitedCB;

    @Override
    public void mouseClicked(MouseEvent e) {
        if (clickedCB == null) {
            return;
        }
        clickedCB.accept(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (pressedCB == null) {
            return;
        }
        pressedCB.accept(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (releasedCB == null) {
            return;
        }
        releasedCB.accept(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (enteredCB == null) {
            return;
        }
        enteredCB.accept(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (exitedCB == null) {
            return;
        }
        exitedCB.accept(e);
    }

    public static MouseHooks mouseClicked(Consumer<MouseEvent> clickedCB) {
        return new MouseHooks().withClickedCB(clickedCB);
    }

    public static MouseHooks mouseExited(Consumer<MouseEvent> exitedCB) {
        return new MouseHooks().withExitedCB(exitedCB);
    }

    public MouseHooks withClickedCB(Consumer<MouseEvent> clickedCB) {
        this.clickedCB = clickedCB;
        return this;
    }

    public MouseHooks withPressedCB(Consumer<MouseEvent> pressedCB) {
        this.pressedCB = pressedCB;
        return this;
    }

    public MouseHooks withReleasedCB(Consumer<MouseEvent> releasedCB) {
        this.releasedCB = releasedCB;
        return this;
    }

    public MouseHooks withEnteredCB(Consumer<MouseEvent> enteredCB) {
        this.enteredCB = enteredCB;
        return this;
    }

    public MouseHooks withExitedCB(Consumer<MouseEvent> exitedCB) {
        this.exitedCB = exitedCB;
        return this;
    }

}
