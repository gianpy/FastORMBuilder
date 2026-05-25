package org.fastormbuilder.plugin.generator;

import org.fastormbuilder.plugin.model.ConnectionProfile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Generates model files for JavaScript/TypeScript ORM frameworks.
 * Supports: Sequelize, Knex.js, Prisma, TypeORM, Bookshelf.js, Waterline, Objection.js, MikroORM.
 */
public class JsOrmGenerator {

    private final ConnectionProfile connection;
    private final String outputDir;
    private final boolean typescript;
    private final String ormMode;

    public JsOrmGenerator(ConnectionProfile connection, String outputDir, boolean typescript, String ormMode) {
        this.connection = connection;
        this.outputDir = outputDir;
        this.typescript = typescript;
        this.ormMode = ormMode;
    }

    public void generate(List<String> tables, String schema) throws Exception {
        String url = new org.fastormbuilder.plugin.database.UrlBuilder(connection).buildUrl();
        String driver = connection.getDriverType() != null ? connection.getDriverType().getDriverClass() : "";
        Class.forName(driver);

        try (Connection conn = DriverManager.getConnection(url, connection.getUserName(), connection.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();
            List<TableMeta> tableMetas = new ArrayList<>();
            for (String table : tables) {
                tableMetas.add(readTable(meta, schema, table));
            }

            switch (ormMode) {
                case "knexjs": generateKnex(tableMetas); break;
                case "prisma": generatePrisma(tableMetas); break;
                case "typeorm": generateTypeORM(tableMetas); break;
                case "bookshelfjs": generateBookshelf(tableMetas); break;
                case "waterline": generateWaterline(tableMetas); break;
                case "objectionjs": generateObjection(tableMetas); break;
                case "mikroorm": generateMikroORM(tableMetas); break;
                default: generateSequelize(tableMetas); break;
            }
        }
    }

    // ─── Table metadata reading ───────────────────────────────────────────

    private TableMeta readTable(DatabaseMetaData meta, String schema, String table) throws SQLException {
        TableMeta tm = new TableMeta();
        tm.name = table;
        tm.columns = new ArrayList<>();
        tm.primaryKeys = new LinkedHashSet<>();

        try (ResultSet rs = meta.getPrimaryKeys(null, schema.isEmpty() ? null : schema, table)) {
            while (rs.next()) tm.primaryKeys.add(rs.getString("COLUMN_NAME"));
        }
        try (ResultSet rs = meta.getColumns(null, schema.isEmpty() ? null : schema, table, null)) {
            while (rs.next()) {
                Col c = new Col();
                c.name = rs.getString("COLUMN_NAME");
                c.sqlType = rs.getInt("DATA_TYPE");
                c.nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                c.autoIncrement = "YES".equals(rs.getString("IS_AUTOINCREMENT"));
                c.defaultValue = rs.getString("COLUMN_DEF");
                c.isPk = tm.primaryKeys.contains(c.name);
                tm.columns.add(c);
            }
        }
        return tm;
    }

    // ─── Knex.js migration ────────────────────────────────────────────────

    private void generateKnex(List<TableMeta> tables) throws IOException {
        String ext = typescript ? ".ts" : ".js";
        String timestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());

        StringBuilder sb = new StringBuilder();
        if (typescript) {
            sb.append("import { Knex } from 'knex';\n\n");
            sb.append("export async function up(knex: Knex): Promise<void> {\n");
        } else {
            sb.append("exports.up = async function(knex) {\n");
        }

        for (TableMeta tm : tables) {
            sb.append("  await knex.schema.createTable('").append(tm.name).append("', (table) => {\n");
            for (Col c : tm.columns) {
                sb.append("    ");
                if (c.isPk && c.autoIncrement) {
                    sb.append("table.increments('").append(c.name).append("')");
                } else {
                    sb.append("table.").append(knexType(c.sqlType)).append("('").append(c.name).append("')");
                    if (c.isPk) sb.append(".primary()");
                    if (!c.nullable && !c.isPk) sb.append(".notNullable()");
                    if (c.defaultValue != null && !c.autoIncrement) sb.append(".defaultTo(").append(knexDefault(c.defaultValue, c.sqlType)).append(")");
                }
                sb.append(";\n");
            }
            sb.append("  });\n");
        }

        if (typescript) {
            sb.append("}\n\nexport async function down(knex: Knex): Promise<void> {\n");
        } else {
            sb.append("};\n\nexports.down = async function(knex) {\n");
        }
        for (int i = tables.size() - 1; i >= 0; i--) {
            sb.append("  await knex.schema.dropTableIfExists('").append(tables.get(i).name).append("');\n");
        }
        sb.append(typescript ? "}\n" : "};\n");

        writeFile(timestamp + "_create_tables" + ext, sb.toString());
    }

    // ─── Prisma schema ────────────────────────────────────────────────────

    private void generatePrisma(List<TableMeta> tables) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("generator client {\n  provider = \"prisma-client-js\"\n}\n\n");
        sb.append("datasource db {\n  provider = \"").append(prismaProvider()).append("\"\n  url      = env(\"DATABASE_URL\")\n}\n\n");

        for (TableMeta tm : tables) {
            sb.append("model ").append(toPascalCase(tm.name)).append(" {\n");
            for (Col c : tm.columns) {
                sb.append("  ").append(toCamelCase(c.name)).append(" ");
                sb.append(prismaType(c.sqlType, c.nullable));
                List<String> attrs = new ArrayList<>();
                if (c.isPk) attrs.add("@id");
                if (c.autoIncrement) attrs.add("@default(autoincrement())");
                else if (c.defaultValue != null && c.defaultValue.toLowerCase().contains("now")) attrs.add("@default(now())");
                if (!c.name.equals(toCamelCase(c.name))) attrs.add("@map(\"" + c.name + "\")");
                if (!attrs.isEmpty()) sb.append(" ").append(String.join(" ", attrs));
                sb.append("\n");
            }
            sb.append("\n  @@map(\"").append(tm.name).append("\")\n}\n\n");
        }

        writeFile("schema.prisma", sb.toString());
    }

    // ─── TypeORM entities ─────────────────────────────────────────────────

    private void generateTypeORM(List<TableMeta> tables) throws IOException {
        String ext = typescript ? ".ts" : ".js";
        for (TableMeta tm : tables) {
            String className = toPascalCase(tm.name);
            StringBuilder sb = new StringBuilder();

            if (typescript) {
                sb.append("import { Entity, PrimaryGeneratedColumn, PrimaryColumn, Column } from 'typeorm';\n\n");
                sb.append("@Entity('").append(tm.name).append("')\n");
                sb.append("export class ").append(className).append(" {\n");
            } else {
                sb.append("const { EntitySchema } = require('typeorm');\n\n");
                sb.append("module.exports = new EntitySchema({\n");
                sb.append("  name: '").append(className).append("',\n");
                sb.append("  tableName: '").append(tm.name).append("',\n");
                sb.append("  columns: {\n");
            }

            for (Col c : tm.columns) {
                if (typescript) {
                    if (c.isPk && c.autoIncrement) sb.append("  @PrimaryGeneratedColumn()\n");
                    else if (c.isPk) sb.append("  @PrimaryColumn()\n");
                    else sb.append("  @Column({ nullable: ").append(c.nullable).append(" })\n");
                    sb.append("  ").append(toCamelCase(c.name)).append(typescript ? ": " + tsType(c.sqlType) : "").append(";\n\n");
                } else {
                    sb.append("    ").append(toCamelCase(c.name)).append(": {\n");
                    sb.append("      type: '").append(typeormType(c.sqlType)).append("',\n");
                    if (c.isPk) { sb.append("      primary: true,\n"); if (c.autoIncrement) sb.append("      generated: true,\n"); }
                    if (!c.nullable && !c.isPk) sb.append("      nullable: false,\n");
                    if (!c.name.equals(toCamelCase(c.name))) sb.append("      name: '").append(c.name).append("',\n");
                    sb.append("    },\n");
                }
            }

            if (typescript) sb.append("}\n");
            else sb.append("  }\n});\n");

            writeFile(toCamelCase(tm.name) + ".entity" + ext, sb.toString());
        }
    }

    // ─── Bookshelf.js models ──────────────────────────────────────────────

    private void generateBookshelf(List<TableMeta> tables) throws IOException {
        String ext = typescript ? ".ts" : ".js";
        for (TableMeta tm : tables) {
            String className = toPascalCase(tm.name);
            StringBuilder sb = new StringBuilder();
            if (typescript) {
                sb.append("import bookshelf from './bookshelf';\n\n");
                sb.append("export const ").append(className).append(" = bookshelf.model('").append(className).append("', {\n");
            } else {
                sb.append("const bookshelf = require('./bookshelf');\n\n");
                sb.append("const ").append(className).append(" = bookshelf.model('").append(className).append("', {\n");
            }
            sb.append("  tableName: '").append(tm.name).append("',\n");
            String pk = tm.primaryKeys.isEmpty() ? "id" : tm.primaryKeys.iterator().next();
            sb.append("  idAttribute: '").append(pk).append("',\n");
            sb.append("});\n");
            if (!typescript) sb.append("\nmodule.exports = { ").append(className).append(" };\n");

            writeFile(toCamelCase(tm.name) + ext, sb.toString());
        }
    }

    // ─── Waterline models ─────────────────────────────────────────────────

    private void generateWaterline(List<TableMeta> tables) throws IOException {
        String ext = typescript ? ".ts" : ".js";
        for (TableMeta tm : tables) {
            String identity = tm.name.toLowerCase();
            StringBuilder sb = new StringBuilder();
            if (typescript) {
                sb.append("export default {\n");
            } else {
                sb.append("module.exports = {\n");
            }
            sb.append("  identity: '").append(identity).append("',\n");
            sb.append("  datastore: 'default',\n");
            sb.append("  primaryKey: '").append(tm.primaryKeys.isEmpty() ? "id" : tm.primaryKeys.iterator().next()).append("',\n");
            sb.append("  attributes: {\n");
            for (Col c : tm.columns) {
                sb.append("    ").append(toCamelCase(c.name)).append(": {\n");
                sb.append("      type: '").append(waterlineType(c.sqlType)).append("',\n");
                if (c.isPk && c.autoIncrement) sb.append("      autoIncrement: true,\n");
                if (!c.nullable && !c.isPk) sb.append("      required: true,\n");
                if (!c.name.equals(toCamelCase(c.name))) sb.append("      columnName: '").append(c.name).append("',\n");
                sb.append("    },\n");
            }
            sb.append("  }\n};\n");

            writeFile(toCamelCase(tm.name) + ext, sb.toString());
        }
    }

    // ─── Objection.js models ──────────────────────────────────────────────

    private void generateObjection(List<TableMeta> tables) throws IOException {
        String ext = typescript ? ".ts" : ".js";
        for (TableMeta tm : tables) {
            String className = toPascalCase(tm.name);
            StringBuilder sb = new StringBuilder();
            if (typescript) {
                sb.append("import { Model } from 'objection';\n\n");
                sb.append("export class ").append(className).append(" extends Model {\n");
            } else {
                sb.append("const { Model } = require('objection');\n\n");
                sb.append("class ").append(className).append(" extends Model {\n");
            }
            sb.append("  static get tableName() { return '").append(tm.name).append("'; }\n\n");
            sb.append("  static get jsonSchema() {\n    return {\n      type: 'object',\n");
            List<String> required = new ArrayList<>();
            for (Col c : tm.columns) { if (!c.nullable && !c.isPk) required.add("'" + toCamelCase(c.name) + "'"); }
            if (!required.isEmpty()) sb.append("      required: [").append(String.join(", ", required)).append("],\n");
            sb.append("      properties: {\n");
            for (Col c : tm.columns) {
                sb.append("        ").append(toCamelCase(c.name)).append(": { type: '").append(jsonSchemaType(c.sqlType)).append("' },\n");
            }
            sb.append("      }\n    };\n  }\n}\n");
            if (!typescript) sb.append("\nmodule.exports = { ").append(className).append(" };\n");

            writeFile(toCamelCase(tm.name) + ext, sb.toString());
        }
    }

    // ─── MikroORM entities ────────────────────────────────────────────────

    private void generateMikroORM(List<TableMeta> tables) throws IOException {
        String ext = typescript ? ".ts" : ".js";
        for (TableMeta tm : tables) {
            String className = toPascalCase(tm.name);
            StringBuilder sb = new StringBuilder();

            if (typescript) {
                sb.append("import { Entity, PrimaryKey, Property } from '@mikro-orm/core';\n\n");
                sb.append("@Entity({ tableName: '").append(tm.name).append("' })\n");
                sb.append("export class ").append(className).append(" {\n");
                for (Col c : tm.columns) {
                    if (c.isPk) sb.append("  @PrimaryKey()\n");
                    else sb.append("  @Property({ nullable: ").append(c.nullable).append(" })\n");
                    sb.append("  ").append(toCamelCase(c.name)).append("!: ").append(tsType(c.sqlType)).append(";\n\n");
                }
                sb.append("}\n");
            } else {
                sb.append("const { EntitySchema } = require('@mikro-orm/core');\n\n");
                sb.append("module.exports = new EntitySchema({\n");
                sb.append("  name: '").append(className).append("',\n");
                sb.append("  tableName: '").append(tm.name).append("',\n");
                sb.append("  properties: {\n");
                for (Col c : tm.columns) {
                    sb.append("    ").append(toCamelCase(c.name)).append(": { type: '").append(mikroType(c.sqlType)).append("'");
                    if (c.isPk) sb.append(", primary: true");
                    if (c.nullable) sb.append(", nullable: true");
                    sb.append(" },\n");
                }
                sb.append("  }\n});\n");
            }

            writeFile(toCamelCase(tm.name) + ".entity" + ext, sb.toString());
        }
    }

    // ─── Sequelize (delegates to existing generator) ──────────────────────

    private void generateSequelize(List<TableMeta> tables) throws IOException {
        // Reuse the dedicated SequelizeModelGenerator via direct generation
        String ext = typescript ? ".ts" : ".js";
        for (TableMeta tm : tables) {
            String modelName = toPascalCase(tm.name);
            StringBuilder sb = new StringBuilder();

            if (typescript) {
                sb.append("import { Model, DataTypes, Sequelize } from 'sequelize';\n\n");
                sb.append("export default (sequelize: Sequelize) => {\n");
            } else {
                sb.append("const { DataTypes } = require('sequelize');\n\n");
                sb.append("module.exports = (sequelize) => {\n");
            }
            sb.append("  const ").append(modelName).append(" = sequelize.define('").append(modelName).append("', {\n");
            for (int i = 0; i < tm.columns.size(); i++) {
                Col c = tm.columns.get(i);
                sb.append("    ").append(toCamelCase(c.name)).append(": {\n");
                sb.append("      type: DataTypes.").append(sequelizeType(c.sqlType)).append(",\n");
                if (c.isPk) sb.append("      primaryKey: true,\n");
                if (c.autoIncrement) sb.append("      autoIncrement: true,\n");
                if (!c.nullable && !c.isPk) sb.append("      allowNull: false,\n");
                if (!c.name.equals(toCamelCase(c.name))) sb.append("      field: '").append(c.name).append("',\n");
                sb.append("    }").append(i < tm.columns.size() - 1 ? "," : "").append("\n");
            }
            sb.append("  }, {\n    tableName: '").append(tm.name).append("',\n    timestamps: false,\n  });\n\n");
            sb.append("  return ").append(modelName).append(";\n};\n");

            writeFile(toCamelCase(tm.name) + ext, sb.toString());
        }
    }

    // ─── Type mappings ────────────────────────────────────────────────────

    private static String knexType(int t) {
        switch (t) {
            case Types.BIGINT: return "bigInteger";
            case Types.INTEGER: case Types.SMALLINT: case Types.TINYINT: return "integer";
            case Types.FLOAT: case Types.REAL: case Types.DOUBLE: return "float";
            case Types.DECIMAL: case Types.NUMERIC: return "decimal";
            case Types.BOOLEAN: case Types.BIT: return "boolean";
            case Types.DATE: return "date";
            case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: return "timestamp";
            case Types.CLOB: case Types.LONGVARCHAR: return "text";
            case Types.BLOB: case Types.BINARY: case Types.VARBINARY: return "binary";
            default: return "string";
        }
    }

    private static String knexDefault(String val, int sqlType) {
        if (val == null) return "null";
        val = val.trim();
        if (val.equalsIgnoreCase("CURRENT_TIMESTAMP") || val.equalsIgnoreCase("NOW()")) return "knex.fn.now()";
        if (val.equalsIgnoreCase("NULL")) return "null";
        switch (sqlType) {
            case Types.BOOLEAN: case Types.BIT: return val.equals("1") ? "true" : "false";
            case Types.INTEGER: case Types.BIGINT: case Types.FLOAT: case Types.DOUBLE: case Types.DECIMAL: return val.replaceAll("[^0-9.\\-]", "");
            default: return "'" + val.replaceAll("^'|'$", "") + "'";
        }
    }

    private String prismaProvider() {
        if (connection.getDriverType() == null) return "postgresql";
        switch (connection.getDriverType().name()) {
            case "MySQL": case "MariaDB": return "mysql";
            case "SQLite": return "sqlite";
            default: return "postgresql";
        }
    }

    private static String prismaType(int t, boolean nullable) {
        String base;
        switch (t) {
            case Types.BIGINT: base = "BigInt"; break;
            case Types.INTEGER: case Types.SMALLINT: case Types.TINYINT: base = "Int"; break;
            case Types.FLOAT: case Types.REAL: case Types.DOUBLE: case Types.DECIMAL: case Types.NUMERIC: base = "Float"; break;
            case Types.BOOLEAN: case Types.BIT: base = "Boolean"; break;
            case Types.DATE: case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: base = "DateTime"; break;
            case Types.BLOB: case Types.BINARY: case Types.VARBINARY: base = "Bytes"; break;
            default: base = "String"; break;
        }
        return base + (nullable ? "?" : "");
    }

    private static String typeormType(int t) {
        switch (t) {
            case Types.BIGINT: return "bigint";
            case Types.INTEGER: case Types.SMALLINT: case Types.TINYINT: return "int";
            case Types.FLOAT: case Types.REAL: case Types.DOUBLE: return "float";
            case Types.DECIMAL: case Types.NUMERIC: return "decimal";
            case Types.BOOLEAN: case Types.BIT: return "boolean";
            case Types.DATE: return "date";
            case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: return "timestamp";
            case Types.CLOB: case Types.LONGVARCHAR: return "text";
            default: return "varchar";
        }
    }

    private static String waterlineType(int t) {
        switch (t) {
            case Types.INTEGER: case Types.BIGINT: case Types.SMALLINT: case Types.TINYINT: return "number";
            case Types.FLOAT: case Types.REAL: case Types.DOUBLE: case Types.DECIMAL: case Types.NUMERIC: return "number";
            case Types.BOOLEAN: case Types.BIT: return "boolean";
            default: return "string";
        }
    }

    private static String jsonSchemaType(int t) {
        switch (t) {
            case Types.INTEGER: case Types.BIGINT: case Types.SMALLINT: case Types.TINYINT: return "integer";
            case Types.FLOAT: case Types.REAL: case Types.DOUBLE: case Types.DECIMAL: case Types.NUMERIC: return "number";
            case Types.BOOLEAN: case Types.BIT: return "boolean";
            default: return "string";
        }
    }

    private static String mikroType(int t) {
        switch (t) {
            case Types.INTEGER: case Types.BIGINT: case Types.SMALLINT: case Types.TINYINT: return "number";
            case Types.FLOAT: case Types.REAL: case Types.DOUBLE: case Types.DECIMAL: case Types.NUMERIC: return "number";
            case Types.BOOLEAN: case Types.BIT: return "boolean";
            case Types.DATE: case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: return "Date";
            default: return "string";
        }
    }

    private static String tsType(int t) {
        switch (t) {
            case Types.INTEGER: case Types.BIGINT: case Types.SMALLINT: case Types.TINYINT:
            case Types.FLOAT: case Types.REAL: case Types.DOUBLE: case Types.DECIMAL: case Types.NUMERIC: return "number";
            case Types.BOOLEAN: case Types.BIT: return "boolean";
            case Types.DATE: case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: return "Date";
            default: return "string";
        }
    }

    private static String sequelizeType(int t) {
        switch (t) {
            case Types.BIGINT: return "BIGINT";
            case Types.INTEGER: return "INTEGER";
            case Types.SMALLINT: return "SMALLINT";
            case Types.TINYINT: return "TINYINT";
            case Types.FLOAT: case Types.REAL: return "FLOAT";
            case Types.DOUBLE: return "DOUBLE";
            case Types.DECIMAL: case Types.NUMERIC: return "DECIMAL";
            case Types.BOOLEAN: case Types.BIT: return "BOOLEAN";
            case Types.DATE: return "DATEONLY";
            case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: return "DATE";
            case Types.CLOB: case Types.LONGVARCHAR: return "TEXT";
            default: return "STRING";
        }
    }

    // ─── Utilities ────────────────────────────────────────────────────────

    private void writeFile(String fileName, String content) throws IOException {
        new File(outputDir).mkdirs();
        try (FileWriter fw = new FileWriter(new File(outputDir, fileName))) {
            fw.write(content);
        }
    }

    private static String toPascalCase(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-') { nextUpper = true; continue; }
            sb.append(nextUpper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            nextUpper = false;
        }
        return sb.toString();
    }

    private static String toCamelCase(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-') { nextUpper = true; continue; }
            sb.append(nextUpper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            nextUpper = false;
        }
        return sb.toString();
    }

    private static class TableMeta {
        String name;
        List<Col> columns;
        Set<String> primaryKeys;
    }

    private static class Col {
        String name;
        int sqlType;
        boolean nullable;
        boolean autoIncrement;
        String defaultValue;
        boolean isPk;
    }
}
