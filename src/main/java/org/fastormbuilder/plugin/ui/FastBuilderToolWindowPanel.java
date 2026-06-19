package org.fastormbuilder.plugin.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;

public class FastBuilderToolWindowPanel extends JPanel implements Disposable {
    public static final String WINDOW_ID = "FastORM Builder";
    private FastBuilderSwingPanel swingPanel;

    public FastBuilderToolWindowPanel(Project project) {
        super(new BorderLayout());
        swingPanel = new FastBuilderSwingPanel(project);
        add(swingPanel, BorderLayout.CENTER);
    }

    @Override
    public void dispose() {
        if (swingPanel != null) {
            swingPanel.dispose();
        }
    }
}
