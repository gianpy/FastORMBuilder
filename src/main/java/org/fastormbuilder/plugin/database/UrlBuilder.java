package org.fastormbuilder.plugin.database;

import org.fastormbuilder.plugin.model.ConnectionProfile;
import org.fastormbuilder.plugin.util.TextUtils;

public class UrlBuilder {
    private final ConnectionProfile profile;

    public UrlBuilder(ConnectionProfile profile) { this.profile = profile; }

    public String buildUrl() {
        if (TextUtils.hasValue(profile.getUrl())) return profile.getUrl();
        return profile.getDriverType().getUrlPattern()
                .replace("${host}", profile.getHost())
                .replace("${port}", String.valueOf(profile.getPort()))
                .replace("${db}", profile.getDatabase());
    }
}
