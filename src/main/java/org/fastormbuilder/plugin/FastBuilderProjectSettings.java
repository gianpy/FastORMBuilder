package org.fastormbuilder.plugin;

import org.fastormbuilder.plugin.model.ConnectionProfile;
import org.fastormbuilder.plugin.model.ProjectData;
import org.fastormbuilder.plugin.model.TableSpec;
import org.fastormbuilder.plugin.util.TextUtils;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = FastBuilderProjectSettings.STATE_NAME, storages = @Storage(FastBuilderProjectSettings.STORAGE_FILE))
public class FastBuilderProjectSettings implements PersistentStateComponent<ProjectData> {
    static final String STATE_NAME = "FastBuilder.project.settings";
    static final String STORAGE_FILE = "fastbuilder.xml";

    private ProjectData data = new ProjectData();

    public static FastBuilderProjectSettings getInstance(Project project) {
        return ServiceManager.getService(project, FastBuilderProjectSettings.class);
    }

    public ProjectData getData() { return data; }

    public void saveConnections(List<ConnectionProfile> list) {
        for (ConnectionProfile conn : list) {
            CredentialAttributes attr = credentialAttr(conn);
            PasswordSafe.getInstance().set(attr, new Credentials(attr.getUserName(), conn.getPassword()));
        }
        data.setConnectionInfoList(list);
    }

    public String getPassword(ConnectionProfile conn) {
        return PasswordSafe.getInstance().getPassword(credentialAttr(conn));
    }

    private CredentialAttributes credentialAttr(ConnectionProfile conn) {
        String svc = String.format("FastBuilderConn_%s", conn.getId());
        return new CredentialAttributes(svc, conn.getUserName(), this.getClass(), false);
    }

    public void saveTableSpec(List<TableSpec> tables) {
        for (TableSpec t : tables) data.getTableInfoMap().put(t.getDatabase() + "#" + t.getTableName(), t);
    }

    public TableSpec getTableSpec(TableSpec param) {
        return data.getTableInfoMap().get(param.getDatabase() + "#" + param.getTableName());
    }

    @Nullable @Override public ProjectData getState() { return data; }

    @Override
    public void loadState(@NotNull ProjectData state) { XmlSerializerUtil.copyBean(state, this.data); }

    public void clearHistory() { data.getHistoryMap().clear(); }

    public void addHistory(String category, String value) {
        if (TextUtils.isBlank(value)) return;
        List<String> list = data.getHistoryMap().computeIfAbsent(category, k -> new ArrayList<>());
        list.remove(value);
        list.add(0, value);
        int max = data.getDefaultParameters().getHistorySize();
        while (list.size() > max) list.remove(list.size() - 1);
    }
}
