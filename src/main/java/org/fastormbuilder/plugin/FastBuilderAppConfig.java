package org.fastormbuilder.plugin;

import org.fastormbuilder.plugin.model.AppConfig;
import org.fastormbuilder.plugin.util.TextUtils;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@State(name = FastBuilderAppConfig.STATE_NAME, storages = @Storage(FastBuilderAppConfig.STORAGE_FILE))
public class FastBuilderAppConfig implements PersistentStateComponent<AppConfig> {
    static final String STATE_NAME = "FastORMBuilder.application.config";
    static final String STORAGE_FILE = "fastbuilder.xml";

    private AppConfig config = new AppConfig();

    public static FastBuilderAppConfig getInstance() {
        return ServiceManager.getService(FastBuilderAppConfig.class);
    }

    @Nullable @Override public AppConfig getState() { return config; }

    @Override
    public void loadState(@NotNull AppConfig state) { XmlSerializerUtil.copyBean(state, this.config); }

    public String getDeviceId() {
        String id = config.getDeviceId();
        if (!TextUtils.hasValue(id)) {
            id = UUID.randomUUID().toString();
            config.setDeviceId(id);
        }
        return id;
    }
}
