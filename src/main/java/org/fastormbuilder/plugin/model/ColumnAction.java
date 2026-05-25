package org.fastormbuilder.plugin.model;

public enum ColumnAction {
    DEFAULT("Default"), OVERRIDE("Override"), IGNORE("Ignore");

    private final String label;

    ColumnAction(String label) { this.label = label; }
    public String getLabel() { return label; }

    public static class Wrapper {
        public final ColumnAction target;
        public Wrapper(ColumnAction action) { target = action; }
        public String toString() { return target.getLabel(); }
    }
}
