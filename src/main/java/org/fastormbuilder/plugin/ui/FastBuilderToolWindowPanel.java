package org.fastormbuilder.plugin.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefApp;

import javax.swing.*;
import java.awt.*;

public class FastBuilderToolWindowPanel extends JPanel implements Disposable {
    public static final String WINDOW_ID = "FastORM Builder";
    private FastBuilderWebPanel webPanel;

    public FastBuilderToolWindowPanel(Project project) {
        super(new BorderLayout());
        if (JBCefApp.isSupported()) {
            webPanel = new FastBuilderWebPanel(project);
            add(webPanel, BorderLayout.CENTER);
        } else {
            add(new JLabel("JCEF is not supported in this environment. Please use a JetBrains Runtime with JCEF."), BorderLayout.CENTER);
        }
    }

    @Override
    public void dispose() {
        if (webPanel != null) {
            webPanel.dispose();
        }
    }
}
