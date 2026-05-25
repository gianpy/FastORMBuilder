package org.fastormbuilder.plugin.generator;

public class PluginEntry {
    private Object pluginConfig;
    public PluginEntry() {}
    public PluginEntry(Object pluginConfig) { this.pluginConfig = pluginConfig; }
    public Object getPluginConfig() { return pluginConfig; }
    public void setPluginConfig(Object pluginConfig) { this.pluginConfig = pluginConfig; }
}
