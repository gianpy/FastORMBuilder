package org.fastormbuilder.plugin.generator.plugins.selectwithlock;

import org.fastormbuilder.plugin.generator.annotation.PluginConfig;

public class LockQueryConfig {
    public static final String BY_PRIMARY_KEY_WITH_LOCK_OVERRIDE = "byPrimaryKeyWithLockOverride";
    public static final String BY_EXAMPLE_WITH_LOCK_OVERRIDE = "byExampleWithLockOverride";

    @PluginConfig(configKey = BY_PRIMARY_KEY_WITH_LOCK_OVERRIDE, defaultValue = "selectByPrimaryKeyWithLock")
    public String byPrimaryKeyWithLockOverride;

    @PluginConfig(configKey = BY_EXAMPLE_WITH_LOCK_OVERRIDE, defaultValue = "selectByExampleWithLock")
    public String byExampleWithLockOverride;

    public boolean byPrimaryKeyWithLockEnabled;
    public boolean byExampleWithLockEnabled;
}
