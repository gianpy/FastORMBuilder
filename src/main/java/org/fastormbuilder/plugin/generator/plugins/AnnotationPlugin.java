package org.fastormbuilder.plugin.generator.plugins;

import org.mybatis.generator.api.PluginAdapter;
import java.util.List;

public class AnnotationPlugin extends PluginAdapter {
    @Override public boolean validate(List<String> warnings) { return true; }

    public static class Config {
        public String customAnnotationType;
        public Config() {}
        public Config(String type) { this.customAnnotationType = type; }
    }
}
