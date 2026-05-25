package org.fastormbuilder.plugin.model;

public class DbNode {
    public enum NodeType {CONNECTION, SCHEMA, TABLE}

    private final String connId;
    private final String name;
    private final String comment;
    private final NodeType type;
    private final String iconPath;

    public static DbNode of(NodeType type, String name) {
        return new DbNode(type, name, null, null, null);
    }

    public static DbNode of(NodeType type, String name, String comment, String connId) {
        return new DbNode(type, name, comment, connId, null);
    }

    public static DbNode of(NodeType type, String name, String comment, String connId, String iconPath) {
        return new DbNode(type, name, comment, connId, iconPath);
    }

    private DbNode(NodeType type, String name, String comment, String connId, String iconPath) {
        this.type = type;
        this.name = name;
        this.comment = comment;
        this.connId = connId;
        this.iconPath = iconPath;
    }

    public String getConnId() {
        return connId;
    }

    public String getName() {
        return name;
    }

    public NodeType getType() {
        return type;
    }

    public String getComment() {
        return comment;
    }

    public String getIconPath() {
        return iconPath;
    }
}
