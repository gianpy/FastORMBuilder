package org.fastormbuilder.plugin.generator;

import org.fastormbuilder.plugin.model.ConnectionProfile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class JpaEntityGenerator {

    private final ConnectionProfile connection;
    private final String entityPkg;
    private final String repoPkg;
    private final String targetProject;
    private final boolean useLombok;
    private final boolean generateRelations;

    public JpaEntityGenerator(ConnectionProfile connection, String entityPkg, String repoPkg,
                              String targetProject, boolean useLombok, boolean generateRelations) {
        this.connection = connection;
        this.entityPkg = entityPkg;
        this.repoPkg = repoPkg;
        this.targetProject = targetProject;
        this.useLombok = useLombok;
        this.generateRelations = generateRelations;
    }

    public List<String> generate(List<String> tables, String schema) throws Exception {
        List<String> warnings = new ArrayList<>();
        String url = new org.fastormbuilder.plugin.database.UrlBuilder(connection).buildUrl();
        String driver = connection.getDriverType() != null ? connection.getDriverType().getDriverClass() : "";
        Class.forName(driver);

        try (Connection conn = DriverManager.getConnection(url, connection.getUserName(), connection.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();

            // Collect FK info for relations
            Map<String, List<FkInfo>> fkMap = new HashMap<>();
            if (generateRelations) {
                for (String table : tables) {
                    try (ResultSet fks = meta.getImportedKeys(null, schema.isEmpty() ? null : schema, table)) {
                        while (fks.next()) {
                            fkMap.computeIfAbsent(table, k -> new ArrayList<>()).add(new FkInfo(
                                    fks.getString("FKCOLUMN_NAME"),
                                    fks.getString("PKTABLE_NAME"),
                                    fks.getString("PKCOLUMN_NAME")
                            ));
                        }
                    }
                }
            }

            for (String table : tables) {
                generateEntity(conn, meta, schema, table, fkMap.getOrDefault(table, Collections.emptyList()));
                if (repoPkg != null && !repoPkg.isEmpty()) {
                    generateRepository(table, meta, schema, conn);
                }
            }
        }
        return warnings;
    }

    private void generateEntity(Connection conn, DatabaseMetaData meta, String schema, String table, List<FkInfo> fks) throws Exception {
        String className = toCamelCase(table, true);
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(entityPkg).append(";\n\n");

        // Imports
        sb.append("import jakarta.persistence.*;\n");
        if (useLombok) {
            sb.append("import lombok.Data;\n");
        }
        Set<String> extraImports = new LinkedHashSet<>();

        // Collect columns
        List<ColInfo> columns = new ArrayList<>();
        Set<String> pkColumns = new HashSet<>();
        try (ResultSet pkRs = meta.getPrimaryKeys(null, schema.isEmpty() ? null : schema, table)) {
            while (pkRs.next()) pkColumns.add(pkRs.getString("COLUMN_NAME"));
        }
        try (ResultSet rs = meta.getColumns(null, schema.isEmpty() ? null : schema, table, null)) {
            while (rs.next()) {
                columns.add(new ColInfo(
                        rs.getString("COLUMN_NAME"),
                        rs.getInt("DATA_TYPE"),
                        rs.getString("IS_NULLABLE").equals("YES"),
                        rs.getString("REMARKS"),
                        pkColumns.contains(rs.getString("COLUMN_NAME"))
                ));
            }
        }

        // FK column names to skip as regular fields
        Set<String> fkColumns = new HashSet<>();
        if (generateRelations) {
            for (FkInfo fk : fks) fkColumns.add(fk.fkColumn);
        }

        // Check if we need BigDecimal or LocalDate
        for (ColInfo col : columns) {
            String jt = sqlTypeToJava(col.sqlType);
            if (jt.equals("BigDecimal")) extraImports.add("import java.math.BigDecimal;\n");
            if (jt.equals("LocalDate")) extraImports.add("import java.time.LocalDate;\n");
            if (jt.equals("LocalDateTime")) extraImports.add("import java.time.LocalDateTime;\n");
        }
        for (String imp : extraImports) sb.append(imp);
        sb.append("\n");

        // Class annotations
        if (useLombok) sb.append("@Data\n");
        sb.append("@Entity\n");
        sb.append("@Table(name = \"").append(table).append("\")\n");
        sb.append("public class ").append(className).append(" {\n\n");

        // Fields
        for (ColInfo col : columns) {
            if (fkColumns.contains(col.name)) continue;
            if (col.comment != null && !col.comment.isEmpty()) {
                sb.append("    /** ").append(col.comment).append(" */\n");
            }
            if (col.pk) {
                sb.append("    @Id\n");
                if (isAutoIncrement(col.sqlType)) {
                    sb.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
                }
            }
            sb.append("    @Column(name = \"").append(col.name).append("\"");
            if (!col.nullable && !col.pk) sb.append(", nullable = false");
            sb.append(")\n");
            String javaType = sqlTypeToJava(col.sqlType);
            String fieldName = toCamelCase(col.name, false);
            sb.append("    private ").append(javaType).append(" ").append(fieldName).append(";\n\n");
        }

        // Relations
        if (generateRelations) {
            for (FkInfo fk : fks) {
                String refClass = toCamelCase(fk.pkTable, true);
                String fieldName = toCamelCase(fk.pkTable, false);
                sb.append("    @ManyToOne(fetch = FetchType.LAZY)\n");
                sb.append("    @JoinColumn(name = \"").append(fk.fkColumn).append("\")\n");
                sb.append("    private ").append(refClass).append(" ").append(fieldName).append(";\n\n");
            }
        }

        // Getters/setters if no Lombok
        if (!useLombok) {
            for (ColInfo col : columns) {
                if (fkColumns.contains(col.name)) continue;
                String javaType = sqlTypeToJava(col.sqlType);
                String fieldName = toCamelCase(col.name, false);
                String cap = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                sb.append("    public ").append(javaType).append(" get").append(cap).append("() { return ").append(fieldName).append("; }\n");
                sb.append("    public void set").append(cap).append("(").append(javaType).append(" ").append(fieldName).append(") { this.").append(fieldName).append(" = ").append(fieldName).append("; }\n\n");
            }
        }

        sb.append("}\n");
        writeFile(entityPkg, className + ".java", sb.toString());
    }

    private void generateRepository(String table, DatabaseMetaData meta, String schema, Connection conn) throws Exception {
        String className = toCamelCase(table, true);
        // Determine PK type
        String pkType = "Long";
        try (ResultSet pkRs = meta.getPrimaryKeys(null, schema.isEmpty() ? null : schema, table)) {
            if (pkRs.next()) {
                String pkCol = pkRs.getString("COLUMN_NAME");
                try (ResultSet colRs = meta.getColumns(null, schema.isEmpty() ? null : schema, table, pkCol)) {
                    if (colRs.next()) pkType = sqlTypeToJava(colRs.getInt("DATA_TYPE"));
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(repoPkg).append(";\n\n");
        sb.append("import ").append(entityPkg).append(".").append(className).append(";\n");
        sb.append("import org.springframework.data.jpa.repository.JpaRepository;\n");
        sb.append("import org.springframework.stereotype.Repository;\n\n");
        sb.append("@Repository\n");
        sb.append("public interface ").append(className).append("Repository extends JpaRepository<").append(className).append(", ").append(pkType).append("> {\n}\n");

        writeFile(repoPkg, className + "Repository.java", sb.toString());
    }

    private void writeFile(String pkg, String fileName, String content) throws IOException {
        String dir = targetProject + "/" + pkg.replace('.', '/');
        new File(dir).mkdirs();
        try (FileWriter fw = new FileWriter(new File(dir, fileName))) {
            fw.write(content);
        }
    }

    private static String sqlTypeToJava(int sqlType) {
        switch (sqlType) {
            case Types.BIGINT: return "Long";
            case Types.INTEGER: case Types.SMALLINT: case Types.TINYINT: return "Integer";
            case Types.FLOAT: case Types.REAL: return "Float";
            case Types.DOUBLE: return "Double";
            case Types.DECIMAL: case Types.NUMERIC: return "BigDecimal";
            case Types.BOOLEAN: case Types.BIT: return "Boolean";
            case Types.DATE: return "LocalDate";
            case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: return "LocalDateTime";
            case Types.BLOB: case Types.BINARY: case Types.VARBINARY: case Types.LONGVARBINARY: return "byte[]";
            default: return "String";
        }
    }

    private static boolean isAutoIncrement(int sqlType) {
        return sqlType == Types.BIGINT || sqlType == Types.INTEGER || sqlType == Types.SMALLINT;
    }

    private static String toCamelCase(String name, boolean capitalizeFirst) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = capitalizeFirst;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-') { nextUpper = true; continue; }
            sb.append(nextUpper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            nextUpper = false;
        }
        return sb.toString();
    }

    private static class ColInfo {
        String name; int sqlType; boolean nullable; String comment; boolean pk;
        ColInfo(String name, int sqlType, boolean nullable, String comment, boolean pk) {
            this.name = name; this.sqlType = sqlType; this.nullable = nullable; this.comment = comment; this.pk = pk;
        }
    }

    private static class FkInfo {
        String fkColumn; String pkTable; String pkColumn;
        FkInfo(String fkColumn, String pkTable, String pkColumn) {
            this.fkColumn = fkColumn; this.pkTable = pkTable; this.pkColumn = pkColumn;
        }
    }
}
