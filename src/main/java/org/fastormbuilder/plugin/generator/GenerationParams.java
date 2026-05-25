package org.fastormbuilder.plugin.generator;

import com.intellij.util.xmlb.annotations.Transient;
import org.fastormbuilder.plugin.model.ConnectionProfile;
import org.fastormbuilder.plugin.model.TableSpec;
import org.mybatis.generator.config.JDBCConnectionConfiguration;
import org.mybatis.generator.config.JavaClientGeneratorConfiguration;
import org.mybatis.generator.config.JavaModelGeneratorConfiguration;
import org.mybatis.generator.config.SqlMapGeneratorConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GenerationParams implements Cloneable {
    private String targetRuntime;
    private String beginningDelimiter = "`";
    private String endingDelimiter = "`";
    private Boolean trimStrings = true;
    private Boolean databaseRemark = true;
    private String driverLibrary;

    @Transient
    private Defaults defaultParameters;
    @Transient
    private Map<String, ? extends List<String>> historyMap;
    @Transient
    private ConnectionProfile connectionProfile;

    private JDBCConnectionConfiguration jdbcConfig = new JDBCConnectionConfiguration();
    private JavaModelGeneratorConfiguration javaModelConfig = new JavaModelGeneratorConfiguration();
    private JavaClientGeneratorConfiguration javaClientConfig = new JavaClientGeneratorConfiguration();
    private SqlMapGeneratorConfiguration sqlMapConfig = new SqlMapGeneratorConfiguration();
    private TableConfigWrapper defaultTableConfig = new TableConfigWrapper();
    private Map<String, PluginEntry> selectedPlugins = new LinkedHashMap<>();
    private List<TableSpec> selectedTables;

    public String getTargetRuntime() {
        return targetRuntime;
    }

    public void setTargetRuntime(String targetRuntime) {
        this.targetRuntime = targetRuntime;
    }

    @Transient
    public Defaults getDefaultParameters() {
        return defaultParameters;
    }

    public void setDefaultParameters(Defaults defaultParameters) {
        this.defaultParameters = defaultParameters;
    }

    @Transient
    public Map<String, ? extends List<String>> getHistoryMap() {
        return historyMap;
    }

    public void setHistoryMap(Map<String, ? extends List<String>> historyMap) {
        this.historyMap = historyMap;
    }

    @Transient
    public ConnectionProfile getConnectionProfile() {
        return connectionProfile;
    }

    public void setConnectionProfile(ConnectionProfile connectionProfile) {
        this.connectionProfile = connectionProfile;
    }

    public String getBeginningDelimiter() {
        return beginningDelimiter;
    }

    public void setBeginningDelimiter(String beginningDelimiter) {
        this.beginningDelimiter = beginningDelimiter;
    }

    public String getEndingDelimiter() {
        return endingDelimiter;
    }

    public void setEndingDelimiter(String endingDelimiter) {
        this.endingDelimiter = endingDelimiter;
    }

    public Boolean getTrimStrings() {
        return trimStrings;
    }

    public void setTrimStrings(Boolean trimStrings) {
        this.trimStrings = trimStrings;
    }

    public Boolean getDatabaseRemark() {
        return databaseRemark;
    }

    public void setDatabaseRemark(Boolean databaseRemark) {
        this.databaseRemark = databaseRemark;
    }

    @Transient
    public String getDriverLibrary() {
        return driverLibrary;
    }

    public void setDriverLibrary(String driverLibrary) {
        this.driverLibrary = driverLibrary;
    }

    @Transient
    public JDBCConnectionConfiguration getJdbcConfig() {
        return jdbcConfig;
    }

    public void setJdbcConfig(JDBCConnectionConfiguration jdbcConfig) {
        this.jdbcConfig = jdbcConfig;
    }

    public JavaModelGeneratorConfiguration getJavaModelConfig() {
        return javaModelConfig;
    }

    public void setJavaModelConfig(JavaModelGeneratorConfiguration javaModelConfig) {
        this.javaModelConfig = javaModelConfig;
    }

    public JavaClientGeneratorConfiguration getJavaClientConfig() {
        return javaClientConfig;
    }

    public void setJavaClientConfig(JavaClientGeneratorConfiguration javaClientConfig) {
        this.javaClientConfig = javaClientConfig;
    }

    public SqlMapGeneratorConfiguration getSqlMapConfig() {
        return sqlMapConfig;
    }

    public void setSqlMapConfig(SqlMapGeneratorConfiguration sqlMapConfig) {
        this.sqlMapConfig = sqlMapConfig;
    }

    public TableConfigWrapper getDefaultTableConfig() {
        return defaultTableConfig;
    }

    public void setDefaultTableConfig(TableConfigWrapper defaultTableConfig) {
        this.defaultTableConfig = defaultTableConfig;
    }

    public Map<String, PluginEntry> getSelectedPlugins() {
        return selectedPlugins;
    }

    public void setSelectedPlugins(Map<String, PluginEntry> selectedPlugins) {
        this.selectedPlugins = selectedPlugins;
    }

    public List<TableSpec> getSelectedTables() {
        return selectedTables;
    }

    public void setSelectedTables(List<TableSpec> selectedTables) {
        this.selectedTables = selectedTables;
    }

    @Override
    public GenerationParams clone() {
        try {
            return (GenerationParams) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
