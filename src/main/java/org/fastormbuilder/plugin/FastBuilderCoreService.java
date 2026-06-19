package org.fastormbuilder.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.fastormbuilder.plugin.database.JdbcDataSourceProvider;
import org.fastormbuilder.plugin.generator.Defaults;
import org.fastormbuilder.plugin.generator.GenerationParams;
import org.fastormbuilder.plugin.model.*;
import org.fastormbuilder.plugin.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FastBuilderCoreService {
    private static final Logger log = LoggerFactory.getLogger(FastBuilderCoreService.class);
    private final FastBuilderProjectSettings settings;
    private final org.fastormbuilder.plugin.storage.FastBuilderStorage storage;

    public static FastBuilderCoreService getInstance(Project project) {
        return project.getService(FastBuilderCoreService.class);
    }

    public FastBuilderCoreService(Project project) {
        this.settings = project != null ? FastBuilderProjectSettings.getInstance(project) : null;
        this.storage = project != null ? org.fastormbuilder.plugin.storage.FastBuilderStorage.getInstance(project) : null;
    }

    public org.fastormbuilder.plugin.storage.FastBuilderStorage getStorage() {
        return storage;
    }

    public void saveConnections(List<ConnectionProfile> list) {
        settings.saveConnections(list);
        // Also save passwords to local encrypted storage
        if (storage != null) {
            for (ConnectionProfile c : list) {
                if (c.getPassword() != null && !c.getPassword().isEmpty()) {
                    storage.savePassword(c.getId(), c.getPassword());
                }
            }
        }
    }

    public String getPassword(ConnectionProfile profile) {
        // Try PasswordSafe first, then fallback to local encrypted storage
        String pw = settings.getPassword(profile);
        if (pw != null && !pw.isEmpty()) return pw;
        if (storage != null) {
            pw = storage.getPassword(profile.getId());
        }
        return pw;
    }

    public void testConnection(ConnectionProfile profile) throws SQLException {
        try (Connection conn = JdbcDataSourceProvider.getInstance().create(profile).getConnection()) { /* ok */ }
    }

    public List<ConnectionProfile> getConnections() {
        return settings.getData().getConnectionInfoList();
    }

    public List<ConnectionProfile> getConnectionsWithPassword() {
        List<ConnectionProfile> result = new ArrayList<>();
        for (ConnectionProfile c : getConnections()) {
            ConnectionProfile dto = c.clone();
            populatePassword(dto);
            result.add(dto);
        }
        return result;
    }

    private void populatePassword(ConnectionProfile profile) {
        try {
            String pw = settings.getPassword(profile);
            if (pw != null) profile.setPassword(pw);
        } catch (RuntimeException e) {
            log.warn("Password retrieval failed", e);
        }
    }

    public ConnectionProfile getConnectionWithPassword(String connId) throws SQLException {
        for (ConnectionProfile c : getConnections()) {
            if (c.getId().equals(connId)) {
                ConnectionProfile dto = c.clone();
                populatePassword(dto);
                return dto;
            }
        }
        throw new SQLException("Connection not found — please add it first");
    }

    public List<DbNode> fetchSchemas(String connId) throws SQLException {
        List<DbNode> list = new ArrayList<>();
        ConnectionProfile profile = getConnectionWithPassword(connId);
        try (Connection conn = JdbcDataSourceProvider.getInstance().create(profile).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            DriverType dt = profile.getDriverType();

            if (dt == DriverType.PostgreSQL) {
                // PostgreSQL: list schemas in current database
                ResultSet rs = meta.getSchemas();
                while (rs.next()) {
                    String s = rs.getString("TABLE_SCHEM");
                    if (TextUtils.hasValue(s) && !s.startsWith("pg_") && !"information_schema".equals(s)) {
                        list.add(DbNode.of(DbNode.NodeType.SCHEMA, s));
                    }
                }
            } else if (dt == DriverType.Oracle_SID || dt == DriverType.Oracle_Service) {
                // Oracle: list schemas
                ResultSet rs = meta.getSchemas();
                while (rs.next()) {
                    String s = rs.getString(1);
                    if (TextUtils.hasValue(s)) list.add(DbNode.of(DbNode.NodeType.SCHEMA, s));
                }
            } else if (dt != null && dt.isFileBased()) {
                list.add(DbNode.of(DbNode.NodeType.SCHEMA, "Default"));
            } else {
                // MySQL, MariaDB, Custom: list catalogs (databases)
                ResultSet rs = meta.getCatalogs();
                while (rs.next()) {
                    String cat = rs.getString(1);
                    if (TextUtils.hasValue(cat)) list.add(DbNode.of(DbNode.NodeType.SCHEMA, cat));
                }
            }
        }
        return list;
    }

    public List<DbNode> fetchTables(String connId, String schema) throws SQLException {
        List<DbNode> list = new ArrayList<>();
        ConnectionProfile profile = getConnectionWithPassword(connId);
        try (Connection conn = JdbcDataSourceProvider.getInstance().create(profile).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = null, schemaArg = null;
            DriverType dt = profile.getDriverType();

            if (dt == DriverType.PostgreSQL) {
                schemaArg = schema;
            } else if (dt == DriverType.Oracle_SID || dt == DriverType.Oracle_Service) {
                schemaArg = schema;
            } else if (dt != null && dt.isFileBased()) {
                // no filter
            } else {
                // MySQL/MariaDB: schema is actually a catalog (database)
                catalog = schema;
            }
            String[] types = DriverType.DuckDB.equals(dt) ? null : new String[]{"TABLE", "VIEW"};
            ResultSet rs = meta.getTables(catalog, schemaArg, null, types);
            while (rs.next()) {
                String tableType = rs.getString("TABLE_TYPE");
                DbNode.NodeType nodeType = "VIEW".equalsIgnoreCase(tableType) ? DbNode.NodeType.VIEW : DbNode.NodeType.TABLE;
                list.add(DbNode.of(nodeType, rs.getString("TABLE_NAME"), rs.getString("REMARKS"), null));
            }
        }
        return list;
    }

    public List<ColumnSpec> fetchColumns(ConnectionProfile profile, TableSpec table) throws SQLException {
        List<ColumnSpec> list = new ArrayList<>();
        try (Connection conn = JdbcDataSourceProvider.getInstance().create(profile).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String schema = conn.getClass().getName().startsWith("oracle") ? table.getDatabase() : null;
            ResultSet rs = meta.getColumns(table.getDatabase(), schema, table.getTableName(), null);
            while (rs.next()) {
                ColumnSpec col = new ColumnSpec();
                col.setColumnName(rs.getString("COLUMN_NAME"));
                col.setColumnType(rs.getString("TYPE_NAME"));
                col.setComment(rs.getString("REMARKS"));
                list.add(col);
            }
        }
        return list;
    }

    public void stashParams(GenerationParams params) {
        settings.getData().setLastGenerationParams(params);
        settings.saveTableSpec(params.getSelectedTables());
        settings.addHistory(PackageCategory.JAVA_MODEL_PACKAGE.toString(), params.getJavaModelConfig().getTargetPackage());
        settings.addHistory(PackageCategory.JAVA_CLIENT_PACKAGE.toString(), params.getJavaClientConfig().getTargetPackage());
        settings.addHistory(PackageCategory.SQL_MAP_PACKAGE.toString(), params.getSqlMapConfig().getTargetPackage());
    }

    public GenerationParams getLastParams() {
        return settings.getData().getLastGenerationParams();
    }

    public TableSpec getLastTableSpec(TableSpec param) {
        return settings.getTableSpec(param);
    }

    public Defaults getDefaults() {
        Defaults instance = new Defaults();
        XmlSerializerUtil.copyBean(settings.getData().getDefaultParameters(), instance);
        return instance;
    }

    public void saveDefaults(Defaults defaults) {
        settings.getData().setDefaultParameters(defaults);
    }

    public Map<String, List<String>> getHistoryMap() {
        return settings.getData().getHistoryMap();
    }

    public void clearHistory() {
        settings.clearHistory();
    }
}
