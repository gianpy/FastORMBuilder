package org.fastormbuilder.plugin.generator;

import org.fastormbuilder.plugin.model.ConnectionProfile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class HibernateEntityGenerator {

    private final ConnectionProfile connection;
    private final String entityPkg;
    private final String daoPkg;
    private final String targetProject;
    private final String resourcesDir;
    private final boolean useLombok;
    private final boolean generateRelations;
    private final boolean generateHbmXml;
    private final boolean generateHibernateCfg;

    public HibernateEntityGenerator(ConnectionProfile connection, String entityPkg, String daoPkg,
                                    String targetProject, String resourcesDir, boolean useLombok,
                                    boolean generateRelations, boolean generateHbmXml, boolean generateHibernateCfg) {
        this.connection = connection;
        this.entityPkg = entityPkg;
        this.daoPkg = daoPkg;
        this.targetProject = targetProject;
        this.resourcesDir = resourcesDir;
        this.useLombok = useLombok;
        this.generateRelations = generateRelations;
        this.generateHbmXml = generateHbmXml;
        this.generateHibernateCfg = generateHibernateCfg;
    }

    public List<String> generate(List<String> tables, String schema) throws Exception {
        List<String> warnings = new ArrayList<>();
        String url = new org.fastormbuilder.plugin.database.UrlBuilder(connection).buildUrl();
        String driver = connection.getDriverType() != null ? connection.getDriverType().getDriverClass() : "";
        Class.forName(driver);

        try (Connection conn = DriverManager.getConnection(url, connection.getUserName(), connection.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();

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
                List<ColInfo> columns = getColumns(meta, schema, table);
                generateEntity(table, columns, fkMap.getOrDefault(table, Collections.emptyList()));
                if (generateHbmXml) {
                    generateHbm(table, columns, schema, fkMap.getOrDefault(table, Collections.emptyList()));
                }
                if (daoPkg != null && !daoPkg.isEmpty()) {
                    generateDao(table, columns);
                }
            }

            if (generateHibernateCfg) {
                generateCfgXml(tables, schema, url, driver);
            }
        }
        return warnings;
    }

    private List<ColInfo> getColumns(DatabaseMetaData meta, String schema, String table) throws SQLException {
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
                        rs.getString("TYPE_NAME"),
                        rs.getString("IS_NULLABLE").equals("YES"),
                        rs.getString("REMARKS"),
                        pkColumns.contains(rs.getString("COLUMN_NAME")),
                        rs.getInt("COLUMN_SIZE")
                ));
            }
        }
        return columns;
    }

    private void generateEntity(String table, List<ColInfo> columns, List<FkInfo> fks) throws IOException {
        String className = toCamelCase(table, true);
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(entityPkg).append(";\n\n");

        sb.append("import jakarta.persistence.*;\n");
        if (useLombok) sb.append("import lombok.Data;\nimport lombok.NoArgsConstructor;\n");

        Set<String> extraImports = new LinkedHashSet<>();
        Set<String> fkColumns = new HashSet<>();
        if (generateRelations) for (FkInfo fk : fks) fkColumns.add(fk.fkColumn);

        for (ColInfo col : columns) {
            String jt = sqlTypeToJava(col.sqlType);
            if (jt.equals("BigDecimal")) extraImports.add("import java.math.BigDecimal;\n");
            if (jt.equals("LocalDate")) extraImports.add("import java.time.LocalDate;\n");
            if (jt.equals("LocalDateTime")) extraImports.add("import java.time.LocalDateTime;\n");
        }
        for (String imp : extraImports) sb.append(imp);
        sb.append("\n");

        if (useLombok) sb.append("@Data\n@NoArgsConstructor\n");
        sb.append("@Entity\n@Table(name = \"").append(table).append("\")\n");
        sb.append("public class ").append(className).append(" implements java.io.Serializable {\n\n");

        for (ColInfo col : columns) {
            if (fkColumns.contains(col.name)) continue;
            if (col.comment != null && !col.comment.isEmpty()) sb.append("    /** ").append(col.comment).append(" */\n");
            if (col.pk) {
                sb.append("    @Id\n");
                sb.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
            }
            sb.append("    @Column(name = \"").append(col.name).append("\"");
            if (!col.nullable && !col.pk) sb.append(", nullable = false");
            if (sqlTypeToJava(col.sqlType).equals("String") && col.length > 0 && col.length != 255)
                sb.append(", length = ").append(col.length);
            sb.append(")\n");
            sb.append("    private ").append(sqlTypeToJava(col.sqlType)).append(" ").append(toCamelCase(col.name, false)).append(";\n\n");
        }

        if (generateRelations) {
            for (FkInfo fk : fks) {
                String refClass = toCamelCase(fk.pkTable, true);
                sb.append("    @ManyToOne(fetch = FetchType.LAZY)\n");
                sb.append("    @JoinColumn(name = \"").append(fk.fkColumn).append("\")\n");
                sb.append("    private ").append(refClass).append(" ").append(toCamelCase(fk.pkTable, false)).append(";\n\n");
            }
        }

        if (!useLombok) {
            for (ColInfo col : columns) {
                if (fkColumns.contains(col.name)) continue;
                String javaType = sqlTypeToJava(col.sqlType);
                String field = toCamelCase(col.name, false);
                String cap = field.substring(0, 1).toUpperCase() + field.substring(1);
                sb.append("    public ").append(javaType).append(" get").append(cap).append("() { return ").append(field).append("; }\n");
                sb.append("    public void set").append(cap).append("(").append(javaType).append(" ").append(field).append(") { this.").append(field).append(" = ").append(field).append("; }\n\n");
            }
        }

        sb.append("}\n");
        writeJava(entityPkg, className + ".java", sb.toString());
    }

    private void generateHbm(String table, List<ColInfo> columns, String schema, List<FkInfo> fks) throws IOException {
        String className = toCamelCase(table, true);
        String fqn = entityPkg + "." + className;
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE hibernate-mapping PUBLIC\n");
        sb.append("    \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"\n");
        sb.append("    \"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">\n\n");
        sb.append("<hibernate-mapping>\n");
        sb.append("    <class name=\"").append(fqn).append("\" table=\"").append(table).append("\"");
        if (!schema.isEmpty()) sb.append(" schema=\"").append(schema).append("\"");
        sb.append(">\n");

        Set<String> fkCols = new HashSet<>();
        if (generateRelations) for (FkInfo fk : fks) fkCols.add(fk.fkColumn);

        // ID
        ColInfo pkCol = columns.stream().filter(c -> c.pk).findFirst().orElse(null);
        if (pkCol != null) {
            sb.append("        <id name=\"").append(toCamelCase(pkCol.name, false)).append("\" column=\"").append(pkCol.name).append("\" type=\"").append(hibernateType(pkCol.sqlType)).append("\">\n");
            sb.append("            <generator class=\"identity\"/>\n");
            sb.append("        </id>\n");
        }

        // Properties
        for (ColInfo col : columns) {
            if (col.pk || fkCols.contains(col.name)) continue;
            sb.append("        <property name=\"").append(toCamelCase(col.name, false))
                    .append("\" column=\"").append(col.name)
                    .append("\" type=\"").append(hibernateType(col.sqlType)).append("\"");
            if (!col.nullable) sb.append(" not-null=\"true\"");
            if (hibernateType(col.sqlType).equals("string") && col.length > 0 && col.length != 255)
                sb.append(" length=\"").append(col.length).append("\"");
            sb.append("/>\n");
        }

        // Many-to-one
        if (generateRelations) {
            for (FkInfo fk : fks) {
                sb.append("        <many-to-one name=\"").append(toCamelCase(fk.pkTable, false))
                        .append("\" class=\"").append(entityPkg).append(".").append(toCamelCase(fk.pkTable, true))
                        .append("\" column=\"").append(fk.fkColumn).append("\" lazy=\"proxy\"/>\n");
            }
        }

        sb.append("    </class>\n</hibernate-mapping>\n");
        writeResource(className + ".hbm.xml", sb.toString());
    }

    private void generateDao(String table, List<ColInfo> columns) throws IOException {
        String className = toCamelCase(table, true);
        String pkType = columns.stream().filter(c -> c.pk).findFirst()
                .map(c -> sqlTypeToJava(c.sqlType)).orElse("Long");

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(daoPkg).append(";\n\n");
        sb.append("import ").append(entityPkg).append(".").append(className).append(";\n");
        sb.append("import org.hibernate.Session;\nimport org.hibernate.SessionFactory;\n\n");
        sb.append("public class ").append(className).append("Dao {\n\n");
        sb.append("    private final SessionFactory sessionFactory;\n\n");
        sb.append("    public ").append(className).append("Dao(SessionFactory sessionFactory) {\n");
        sb.append("        this.sessionFactory = sessionFactory;\n    }\n\n");
        sb.append("    public ").append(className).append(" findById(").append(pkType).append(" id) {\n");
        sb.append("        try (Session session = sessionFactory.openSession()) {\n");
        sb.append("            return session.get(").append(className).append(".class, id);\n");
        sb.append("        }\n    }\n\n");
        sb.append("    public void save(").append(className).append(" entity) {\n");
        sb.append("        try (Session session = sessionFactory.openSession()) {\n");
        sb.append("            session.beginTransaction();\n");
        sb.append("            session.persist(entity);\n");
        sb.append("            session.getTransaction().commit();\n");
        sb.append("        }\n    }\n\n");
        sb.append("    public void update(").append(className).append(" entity) {\n");
        sb.append("        try (Session session = sessionFactory.openSession()) {\n");
        sb.append("            session.beginTransaction();\n");
        sb.append("            session.merge(entity);\n");
        sb.append("            session.getTransaction().commit();\n");
        sb.append("        }\n    }\n\n");
        sb.append("    public void delete(").append(className).append(" entity) {\n");
        sb.append("        try (Session session = sessionFactory.openSession()) {\n");
        sb.append("            session.beginTransaction();\n");
        sb.append("            session.remove(entity);\n");
        sb.append("            session.getTransaction().commit();\n");
        sb.append("        }\n    }\n}\n");

        writeJava(daoPkg, className + "Dao.java", sb.toString());
    }

    private void generateCfgXml(List<String> tables, String schema, String url, String driver) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE hibernate-configuration PUBLIC\n");
        sb.append("    \"-//Hibernate/Hibernate Configuration DTD 3.0//EN\"\n");
        sb.append("    \"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">\n\n");
        sb.append("<hibernate-configuration>\n    <session-factory>\n");
        sb.append("        <property name=\"hibernate.connection.driver_class\">").append(driver).append("</property>\n");
        sb.append("        <property name=\"hibernate.connection.url\">").append(escapeXml(url)).append("</property>\n");
        sb.append("        <property name=\"hibernate.connection.username\">").append(escapeXml(connection.getUserName())).append("</property>\n");
        sb.append("        <property name=\"hibernate.dialect\">").append(guessDialect()).append("</property>\n");
        sb.append("        <property name=\"hibernate.show_sql\">true</property>\n");
        sb.append("        <property name=\"hibernate.hbm2ddl.auto\">validate</property>\n\n");

        for (String table : tables) {
            String className = toCamelCase(table, true);
            if (generateHbmXml) {
                sb.append("        <mapping resource=\"").append(className).append(".hbm.xml\"/>\n");
            } else {
                sb.append("        <mapping class=\"").append(entityPkg).append(".").append(className).append("\"/>\n");
            }
        }
        sb.append("    </session-factory>\n</hibernate-configuration>\n");
        writeResource("hibernate.cfg.xml", sb.toString());
    }

    private String guessDialect() {
        if (connection.getDriverType() == null) return "org.hibernate.dialect.MySQLDialect";
        switch (connection.getDriverType().name()) {
            case "MySQL": case "MariaDB": return "org.hibernate.dialect.MySQLDialect";
            case "PostgreSQL": return "org.hibernate.dialect.PostgreSQLDialect";
            case "Oracle_SID": case "Oracle_Service": return "org.hibernate.dialect.OracleDialect";
            case "SQLite": return "org.hibernate.community.dialect.SQLiteDialect";
            case "DuckDB": return "org.hibernate.dialect.H2Dialect";
            default: return "org.hibernate.dialect.MySQLDialect";
        }
    }

    private void writeJava(String pkg, String fileName, String content) throws IOException {
        String dir = targetProject + "/" + pkg.replace('.', '/');
        new File(dir).mkdirs();
        try (FileWriter fw = new FileWriter(new File(dir, fileName))) { fw.write(content); }
    }

    private void writeResource(String fileName, String content) throws IOException {
        new File(resourcesDir).mkdirs();
        try (FileWriter fw = new FileWriter(new File(resourcesDir, fileName))) { fw.write(content); }
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

    private static String hibernateType(int sqlType) {
        switch (sqlType) {
            case Types.BIGINT: return "long";
            case Types.INTEGER: case Types.SMALLINT: case Types.TINYINT: return "integer";
            case Types.FLOAT: case Types.REAL: return "float";
            case Types.DOUBLE: return "double";
            case Types.DECIMAL: case Types.NUMERIC: return "big_decimal";
            case Types.BOOLEAN: case Types.BIT: return "boolean";
            case Types.DATE: return "date";
            case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: return "timestamp";
            case Types.BLOB: case Types.BINARY: case Types.VARBINARY: case Types.LONGVARBINARY: return "binary";
            default: return "string";
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

    private static String escapeXml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static class ColInfo {
        String name; int sqlType; String typeName; boolean nullable; String comment; boolean pk; int length;
        ColInfo(String name, int sqlType, String typeName, boolean nullable, String comment, boolean pk, int length) {
            this.name = name; this.sqlType = sqlType; this.typeName = typeName;
            this.nullable = nullable; this.comment = comment; this.pk = pk; this.length = length;
        }
    }

    private static class FkInfo {
        String fkColumn; String pkTable; String pkColumn;
        FkInfo(String fkColumn, String pkTable, String pkColumn) {
            this.fkColumn = fkColumn; this.pkTable = pkTable; this.pkColumn = pkColumn;
        }
    }
}
