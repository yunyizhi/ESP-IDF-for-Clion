package org.btik.espidf.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.btik.espidf.ui.componets.KeyBoardListener;
import org.cef.browser.CefBrowser;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import static com.intellij.ui.jcef.JBCefBrowserBase.Properties.NO_CONTEXT_MENU;


public class ComponentsBrowserPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(ComponentsBrowserPanel.class);
    private final Consumer<String> urlLoader;
    private String url;
    JBCefBrowser jbCefBrowser;

    public ComponentsBrowserPanel() {
        super(new BorderLayout());
        if (JBCefApp.isSupported()) {
            jbCefBrowser = new JBCefBrowser();
            add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
            urlLoader = jbCefBrowser::loadURL;
            initBrowser();
        } else {
            add(new JLabel("not Support JBCef"), BorderLayout.CENTER);
            urlLoader = url -> LOG.warn("not loader url");
        }
    }

    private void initBrowser() {
        jbCefBrowser.setProperty(NO_CONTEXT_MENU, false);
        CefBrowser cefBrowser = jbCefBrowser.getCefBrowser();
        addKeyListener(new KeyBoardListener().withKeyReleasedCB(event -> {
            int extendedKeyCode = event.getExtendedKeyCode();
            if (extendedKeyCode == KeyEvent.KEY_LOCATION_LEFT) {
                cefBrowser.goBack();
                return;
            }
            if (extendedKeyCode == KeyEvent.KEY_LOCATION_RIGHT) {
                cefBrowser.goForward();
            }
        }));
    }

    public void loadUrl(String url) {
        this.url = url;
        urlLoader.accept(url);
        if (jbCefBrowser != null) {
            jbCefBrowser.getCefBrowser().reload();
        }
    }
}
