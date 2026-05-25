package org.fastormbuilder.plugin;

import com.intellij.icons.AllIcons;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import org.fastormbuilder.plugin.util.UiHelper;

public class FastBuilderTemplateProvider implements FileTemplateGroupDescriptorFactory {
    @Override
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor("FastBuilder", UiHelper.icon("/icons/fastBuilderAction.svg"));
        group.addTemplate(new FileTemplateDescriptor("MyBatisGenerator.xml", AllIcons.FileTypes.Xml));
        return group;
    }
}
