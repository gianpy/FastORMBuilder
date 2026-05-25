package org.fastormbuilder.plugin.model;

import org.mybatis.generator.config.ColumnRenamingRule;
import java.util.List;

public class TableSpec {
    private String database;
    private String tableName;
    private String tableComment;
    private String domainName;
    private String keyColumn;
    private List<ColumnSpec> customColumns;
    private ColumnRenamingRule columnRenamingRule;

    public TableSpec() {}

    public TableSpec(String database, String tableName, String tableComment) {
        this.database = database;
        this.tableName = tableName;
        this.tableComment = tableComment;
    }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getTableComment() { return tableComment; }
    public void setTableComment(String tableComment) { this.tableComment = tableComment; }
    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    public String getKeyColumn() { return keyColumn; }
    public void setKeyColumn(String keyColumn) { this.keyColumn = keyColumn; }
    public List<ColumnSpec> getCustomColumns() { return customColumns; }
    public void setCustomColumns(List<ColumnSpec> customColumns) { this.customColumns = customColumns; }
    public ColumnRenamingRule getColumnRenamingRule() { return columnRenamingRule; }
    public void setColumnRenamingRule(ColumnRenamingRule columnRenamingRule) { this.columnRenamingRule = columnRenamingRule; }
}
