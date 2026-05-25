package org.fastormbuilder.plugin.generator.plugins;

import org.mybatis.generator.api.PluginAdapter;
import java.util.List;

public class NamingPlugin extends PluginAdapter {
    public static final String DOMAIN_NAME = "{domainName}";

    @Override public boolean validate(List<String> warnings) { return true; }

    public static class Config {
        public String mapperTypePattern;
        public String exampleTypePattern;
        public String sqlFileNamePattern;
    }
}
