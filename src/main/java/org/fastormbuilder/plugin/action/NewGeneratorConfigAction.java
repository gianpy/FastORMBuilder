package org.fastormbuilder.plugin.action;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class NewGeneratorConfigAction extends CreateFileFromTemplateAction implements DumbAware {
    public NewGeneratorConfigAction() {
        super("MyBatis Generator Config", "Creates a new MyBatis Generator configuration", AllIcons.FileTypes.Xml);
    }

    @Override
    protected void buildDialog(Project project, PsiDirectory dir, CreateFileFromTemplateDialog.Builder builder) {
        builder.setTitle("New MyBatis Generator Config")
                .addKind("MyBatis Generator Config", AllIcons.FileTypes.Xml, "MyBatisGenerator.xml");
    }

    @Override
    protected String getActionName(PsiDirectory dir, @NotNull String name, String templateName) {
        return "MyBatis Generator Config";
    }

    @Override
    protected PsiFile createFile(String name, String templateName, PsiDirectory dir) {
        FileTemplate template = FileTemplateManager.getInstance(dir.getProject()).getJ2eeTemplate(templateName);
        return createFileFromTemplate(name, template, dir);
    }
}
