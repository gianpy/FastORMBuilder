package org.fastormbuilder.plugin.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.fastormbuilder.plugin.ui.FastBuilderToolWindowPanel;
import org.jetbrains.annotations.NotNull;

public class OpenSettingsAction extends DumbAwareAction {
    private static final String ACTION_ID = "FastORMBuilder.Settings";

    public static AnAction getInstance() {
        return ActionManager.getInstance().getAction(ACTION_ID);
    }

    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(FastBuilderToolWindowPanel.WINDOW_ID);
        if (tw != null) {
            tw.show();
        }
    }
}
