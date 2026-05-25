package org.fastormbuilder.plugin.generator;

import org.fastormbuilder.plugin.model.ConnectionProfile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Generates Java Records compatible with YORM (convention-based ORM).
 * YORM expects: record name = CamelCase of table name, fields in camelCase,
 * PK named "id" with auto-increment, FK named "tableId".
 */
public class YormRecordGenerator {

    private final ConnectionProfile connection;
    private final String recordPkg;
    private final String targetProject;

    public YormRecordGenerator(ConnectionProfile connection, String recordPkg, String targetProject) {
        this.connection = connection;
        this.recordPkg = recordPkg;
        this.targetProject = targetProject;
    }

    public void generate(List<String> tables, String schema) throws Exception {
        String url = new org.fastormbuilder.plugin.database.UrlBuilder(connection).buildUrl();
        String driver = connection.getDriverType() != null ? connection.getDriverType().getDriverClass() : "";
        Class.forName(driver);

        try (Connection conn = DriverManager.getConnection(url, connection.getUserName(), connection.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();
            for (String table : tables) {
                generateRecord(meta, schema, table);
            }
        }
    }

    private void generateRecord(DatabaseMetaData meta, String schema, String table) throws Exception {
        String className = toCamelCase(table, true);
        List<String> fields = new ArrayList<>();
        Set<String> extraImports = new LinkedHashSet<>();

        try (ResultSet rs = meta.getColumns(null, schema.isEmpty() ? null : schema, table, null)) {
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME");
                int sqlType = rs.getInt("DATA_TYPE");
                String javaType = sqlTypeToJava(sqlType);
                String fieldName = toCamelCase(colName, false);

                if (javaType.equals("BigDecimal")) extraImports.add("import java.math.BigDecimal;");
                if (javaType.equals("LocalDate")) extraImports.add("import java.time.LocalDate;");
                if (javaType.equals("LocalDateTime")) extraImports.add("import java.time.LocalDateTime;");

                fields.add(javaType + " " + fieldName);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(recordPkg).append(";\n\n");
        for (String imp : extraImports) sb.append(imp).append("\n");
        if (!extraImports.isEmpty()) sb.append("\n");
        sb.append("public record ").append(className).append("(\n");
        for (int i = 0; i < fields.size(); i++) {
            sb.append("        ").append(fields.get(i));
            if (i < fields.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(") {}\n");

        writeFile(className + ".java", sb.toString());
    }

    private void writeFile(String fileName, String content) throws IOException {
        String dir = targetProject + "/" + recordPkg.replace('.', '/');
        new File(dir).mkdirs();
        try (FileWriter fw = new FileWriter(new File(dir, fileName))) {
            fw.write(content);
        }
    }

    private static String sqlTypeToJava(int sqlType) {
        switch (sqlType) {
            case Types.BIGINT: return "long";
            case Types.INTEGER: case Types.SMALLINT: case Types.TINYINT: return "int";
            case Types.FLOAT: case Types.REAL: return "float";
            case Types.DOUBLE: return "double";
            case Types.DECIMAL: case Types.NUMERIC: return "BigDecimal";
            case Types.BOOLEAN: case Types.BIT: return "boolean";
            case Types.DATE: return "LocalDate";
            case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: return "LocalDateTime";
            default: return "String";
        }
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
}
