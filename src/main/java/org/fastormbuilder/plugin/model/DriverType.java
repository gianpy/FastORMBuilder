package org.fastormbuilder.plugin.model;

public enum DriverType {
    MySQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://${host}:${port}/${db}?useSSL=false&characterEncoding=utf8", 3306, "/images/MySQL.svg"),
    PostgreSQL("org.postgresql.Driver", "jdbc:postgresql://${host}:${port}/${db}", 5432, "/images/PostgreSQL.svg"),
    Oracle_SID("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@${host}:${port}:${db}", 1521, "/images/Oracle.svg"),
    Oracle_Service("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@//${host}:${port}/${db}", 1521, "/images/Oracle.svg"),
    MariaDB("org.mariadb.jdbc.Driver", "jdbc:mariadb://${host}:${port}/${db}", 3306, "/images/MariaDB.svg"),
    SQLite("org.sqlite.JDBC", "jdbc:sqlite:${db}", 0, "/images/SQLite.svg"),
    DuckDB("org.duckdb.DuckDBDriver", "jdbc:duckdb:${db}", 0, "/images/DuckDB.svg"),
    Custom("", "jdbc:${vendor}://${host}:${port}/${db}", 1234, "/images/connection.svg");

    private final String driverClass;
    private final String urlPattern;
    private final int defaultPort;
    private final String icon;

    DriverType(String driverClass, String urlPattern, int defaultPort, String icon) {
        this.driverClass = driverClass;
        this.urlPattern = urlPattern;
        this.defaultPort = defaultPort;
        this.icon = icon;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isFileBased() {
        return this == SQLite || this == DuckDB;
    }

    @Override
    public String toString() {
        return name().replace('_', ' ');
    }
}
