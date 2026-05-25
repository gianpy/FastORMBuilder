package org.fastormbuilder.plugin.model;

import com.intellij.util.xmlb.annotations.Transient;

public class ColumnSpec {
    private ColumnAction action;
    private String columnName;
    @Transient private String columnType;
    @Transient private String comment;
    private String javaProperty;
    private String javaType;
    private String typeHandler;

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public String getColumnType() { return columnType; }
    public void setColumnType(String columnType) { this.columnType = columnType; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public ColumnAction getAction() { return action; }
    public void setAction(ColumnAction action) { this.action = action; }
    public String getJavaProperty() { return javaProperty; }
    public void setJavaProperty(String javaProperty) { this.javaProperty = javaProperty; }
    public String getJavaType() { return javaType; }
    public void setJavaType(String javaType) { this.javaType = javaType; }
    public String getTypeHandler() { return typeHandler; }
    public void setTypeHandler(String typeHandler) { this.typeHandler = typeHandler; }
}
