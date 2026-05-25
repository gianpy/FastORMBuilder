package org.fastormbuilder.plugin.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import org.fastormbuilder.plugin.generator.GeneratorRunner;
import org.fastormbuilder.plugin.generator.callback.IndicatorCallback;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class RunGeneratorAction extends AnAction {
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (!(psiFile instanceof XmlFile)) return;
        VirtualFile vFile = psiFile.getVirtualFile();
        FileDocumentManager fdm = FileDocumentManager.getInstance();
        Document doc = fdm.getDocument(vFile);
        if (doc != null) fdm.saveDocument(doc);

        Properties props = new Properties();
        props.setProperty("CURRENT_DIR", new File(vFile.getPath()).getParent());
        if (event.getProject() != null) props.setProperty("PROJECT_DIR", event.getProject().getBasePath());

        new Task.Backgroundable(event.getProject(), "FastORM Builder") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    List<String> warnings = GeneratorRunner.runWithConfig(vFile.getPath(), props, new IndicatorCallback(indicator));
                    String msg = "Generation complete." + (warnings.isEmpty() ? "" : "\n" + String.join("\n", warnings));
                    Notifier.getInstance().info(msg, event.getProject());
                    VirtualFile dir = ProjectUtil.guessProjectDir(event.getProject());
                    if (dir != null) VfsUtil.markDirtyAndRefresh(true, true, true, dir);
                } catch (Exception e) {
                    Notifier.getInstance().error(String.valueOf(e.getMessage()), event.getProject());
                }
            }
        }.queue();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (!(psiFile instanceof XmlFile)) event.getPresentation().setVisible(false);
    }
}
