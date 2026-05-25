package org.fastormbuilder.plugin.database;

import org.fastormbuilder.plugin.util.TextUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class GenericDataSource implements DataSource {
    private static final Map<String, Driver> driverCache = new HashMap<>();
    private final String driverLibrary;
    private final String driverClass;
    private String url;
    private String user;
    private String password;

    public GenericDataSource(String driverLibrary, String driverClass) {
        this.driverLibrary = driverLibrary;
        this.driverClass = driverClass;
    }

    public void setUrl(String url) { this.url = url; }
    public void setUser(String user) { this.user = user; }
    public void setPassword(String password) { this.password = password; }

    private Driver getDriver() throws SQLException {
        String key = driverClass + "@" + driverLibrary;
        if (!driverCache.containsKey(key)) {
            try {
                URL[] urls = {};
                if (driverLibrary != null && !driverLibrary.isEmpty()) {
                    urls = new URL[]{new File(driverLibrary).toURI().toURL()};
                }
                URLClassLoader cl = URLClassLoader.newInstance(urls, getClass().getClassLoader());
                Driver driver = (Driver) cl.loadClass(driverClass).newInstance();
                DriverManager.registerDriver(driver);
                driverCache.put(key, driver);
            } catch (Exception e) {
                throw new SQLException("Driver init failed: " + e.getMessage());
            }
        }
        return driverCache.get(key);
    }

    @Override public Connection getConnection() throws SQLException { return getConnection(user, password); }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Properties props = new Properties();
        if (TextUtils.hasValue(username)) props.put("user", username);
        if (TextUtils.hasValue(password)) props.put("password", password);
        if (!"org.duckdb.DuckDBDriver".equals(driverClass)) props.setProperty("remarks", "true");
        return getDriver().connect(url, props);
    }

    @Override public <T> T unwrap(Class<T> iface) { return null; }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    @Override public PrintWriter getLogWriter() { return null; }
    @Override public void setLogWriter(PrintWriter out) {}
    @Override public void setLoginTimeout(int seconds) {}
    @Override public int getLoginTimeout() { return 0; }
    @Override public Logger getParentLogger() { return null; }
}
