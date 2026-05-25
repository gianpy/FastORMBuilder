package org.fastormbuilder.plugin.storage;

import com.intellij.openapi.project.Project;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Unified SQLite storage in .fastbuilder/fastbuilder.db
 * Handles: encrypted passwords, generation history, package history, and logs.
 */
public class FastBuilderStorage {
    private static final Map<String, FastBuilderStorage> instances = new HashMap<>();
    private final String dbPath;
    private final byte[] encKey;

    public static synchronized FastBuilderStorage getInstance(Project project) {
        String base = project.getBasePath();
        if (base == null) base = System.getProperty("user.home");
        String finalBase = base;
        return instances.computeIfAbsent(base, k -> new FastBuilderStorage(finalBase));
    }

    private FastBuilderStorage(String projectBase) {
        File dir = new File(projectBase, ".fastbuilder");
        if (!dir.exists()) dir.mkdirs();
        this.dbPath = new File(dir, "fastbuilder.db").getAbsolutePath();
        this.encKey = deriveKey(projectBase);
        // Explicitly load SQLite driver (plugin classloader may not auto-discover it)
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[FastBuilder] SQLite JDBC driver not found: " + e.getMessage());
        }
        initDb();
    }

    private byte[] deriveKey(String seed) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Arrays.copyOf(md.digest(("FastBuilder_" + seed).getBytes(StandardCharsets.UTF_8)), 16);
        } catch (Exception e) {
            return "FastBuilder__Key!".getBytes(StandardCharsets.UTF_8);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    private void initDb() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS passwords (" +
                    "conn_id TEXT PRIMARY KEY," +
                    "encrypted_pw TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS generation_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "connection_id TEXT," +
                    "tables TEXT," +
                    "model_pkg TEXT," +
                    "mapper_pkg TEXT," +
                    "xml_pkg TEXT," +
                    "runtime TEXT," +
                    "client_type TEXT," +
                    "created_at TEXT DEFAULT (datetime('now','localtime')))");
            s.execute("CREATE TABLE IF NOT EXISTS pkg_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "connection_id TEXT," +
                    "pkg_type TEXT," +
                    "pkg_value TEXT," +
                    "UNIQUE(connection_id, pkg_type, pkg_value))");
            s.execute("CREATE TABLE IF NOT EXISTS logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "ts TEXT NOT NULL," +
                    "level TEXT NOT NULL," +
                    "source TEXT," +
                    "message TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS user_preferences (" +
                    "key TEXT PRIMARY KEY," +
                    "value TEXT NOT NULL)");
        } catch (SQLException e) {
            com.intellij.openapi.diagnostic.Logger.getInstance(FastBuilderStorage.class)
                    .error("[FastBuilder] Failed to init SQLite DB at " + dbPath, e);
        }
    }

    // --- Password Storage (AES encrypted) ---

    public void savePassword(String connId, String password) {
        if (password == null || password.isEmpty()) return;
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT OR REPLACE INTO passwords (conn_id, encrypted_pw) VALUES (?,?)")) {
            ps.setString(1, connId);
            ps.setString(2, encrypt(password));
            ps.executeUpdate();
        } catch (Exception e) {
            logError("PasswordStore", "Failed to save password: " + e.getMessage());
        }
    }

    public String getPassword(String connId) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT encrypted_pw FROM passwords WHERE conn_id=?")) {
            ps.setString(1, connId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return decrypt(rs.getString(1));
        } catch (Exception e) {
            logError("PasswordStore", "Failed to get password: " + e.getMessage());
        }
        return null;
    }

    public void deletePassword(String connId) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM passwords WHERE conn_id=?")) {
            ps.setString(1, connId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("PasswordStore", "Failed to delete password: " + e.getMessage());
        }
    }

    private String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encKey, "AES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
    }

    private String decrypt(String ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encKey, "AES"));
        return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
    }

    // --- Generation History ---

    public void recordGeneration(String connectionId, List<String> tables, String modelPkg, String mapperPkg, String xmlPkg, String runtime, String clientType) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO generation_history (connection_id, tables, model_pkg, mapper_pkg, xml_pkg, runtime, client_type) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, connectionId);
            ps.setString(2, String.join(",", tables));
            ps.setString(3, modelPkg);
            ps.setString(4, mapperPkg);
            ps.setString(5, xmlPkg);
            ps.setString(6, runtime);
            ps.setString(7, clientType);
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("History", "Failed to record: " + e.getMessage());
        }

        savePkg(connectionId, "model", modelPkg);
        savePkg(connectionId, "mapper", mapperPkg);
        savePkg(connectionId, "xml", xmlPkg);
    }

    private void savePkg(String connectionId, String type, String value) {
        if (value == null || value.isEmpty()) return;
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO pkg_history (connection_id, pkg_type, pkg_value) VALUES (?,?,?)")) {
            ps.setString(1, connectionId);
            ps.setString(2, type);
            ps.setString(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("History", "Failed to save pkg: " + e.getMessage());
        }
    }

    public List<String> getPkgHistory(String connectionId, String type) {
        List<String> result = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT pkg_value FROM pkg_history WHERE connection_id=? AND pkg_type=? ORDER BY id DESC LIMIT 10")) {
            ps.setString(1, connectionId);
            ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(rs.getString(1));
        } catch (SQLException e) {
            logError("History", "Failed to get pkg history: " + e.getMessage());
        }
        return result;
    }

    public List<HistoryEntry> getHistory(String connectionId, int limit) {
        List<HistoryEntry> result = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT connection_id, tables, model_pkg, mapper_pkg, xml_pkg, runtime, created_at FROM generation_history WHERE connection_id=? ORDER BY id DESC LIMIT ?")) {
            ps.setString(1, connectionId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HistoryEntry e = new HistoryEntry();
                e.connectionId = rs.getString(1);
                e.tables = rs.getString(2);
                e.modelPkg = rs.getString(3);
                e.mapperPkg = rs.getString(4);
                e.xmlPkg = rs.getString(5);
                e.runtime = rs.getString(6);
                e.date = rs.getString(7);
                result.add(e);
            }
        } catch (SQLException e) {
            logError("History", "Failed to get history: " + e.getMessage());
        }
        return result;
    }

    public List<HistoryEntry> getAllHistory(int limit) {
        List<HistoryEntry> result = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT connection_id, tables, model_pkg, mapper_pkg, xml_pkg, runtime, created_at FROM generation_history ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HistoryEntry e = new HistoryEntry();
                e.connectionId = rs.getString(1);
                e.tables = rs.getString(2);
                e.modelPkg = rs.getString(3);
                e.mapperPkg = rs.getString(4);
                e.xmlPkg = rs.getString(5);
                e.runtime = rs.getString(6);
                e.date = rs.getString(7);
                result.add(e);
            }
        } catch (SQLException e) {
            logError("History", "Failed to get all history: " + e.getMessage());
        }
        return result;
    }

    // --- Logging (no sensitive data) ---

    public void logInfo(String source, String message) {
        insertLog("INFO", source, message);
    }

    public void logDebug(String source, String message) {
        insertLog("DEBUG", source, message);
    }

    public void logError(String source, String message) {
        insertLog("ERROR", source, message);
    }

    private void insertLog(String level, String source, String message) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO logs (ts, level, source, message) VALUES (?,?,?,?)")) {
            ps.setString(1, ts);
            ps.setString(2, level);
            ps.setString(3, source);
            ps.setString(4, message);
            ps.executeUpdate();
        } catch (SQLException ignored) { /* avoid infinite loop */ }
    }

    /**
     * Export logs as text for bug reports (no sensitive data).
     */
    public String exportLogs(int maxLines) {
        StringBuilder sb = new StringBuilder();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT ts, level, source, message FROM logs ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, maxLines);
            ResultSet rs = ps.executeQuery();
            List<String> lines = new ArrayList<>();
            while (rs.next()) {
                lines.add(String.format("[%s] %s [%s] %s", rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
            Collections.reverse(lines);
            for (String l : lines) sb.append(l).append("\n");
        } catch (SQLException e) {
            sb.append("Error reading logs: ").append(e.getMessage());
        }
        return sb.toString();
    }

    // --- User Preferences ---

    public void setPreference(String key, String value) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT OR REPLACE INTO user_preferences (key, value) VALUES (?,?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("Preferences", "Failed to set " + key + ": " + e.getMessage());
        }
    }

    public String getPreference(String key, String defaultValue) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT value FROM user_preferences WHERE key=?")) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            logError("Preferences", "Failed to get " + key + ": " + e.getMessage());
        }
        return defaultValue;
    }

    public Map<String, String> getAllPreferences() {
        Map<String, String> prefs = new HashMap<>();
        try (Connection c = connect(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT key, value FROM user_preferences")) {
            while (rs.next()) prefs.put(rs.getString(1), rs.getString(2));
        } catch (SQLException e) {
            logError("Preferences", "Failed to get all: " + e.getMessage());
        }
        return prefs;
    }

    public static class HistoryEntry {
        public String connectionId;
        public String tables;
        public String modelPkg;
        public String mapperPkg;
        public String xmlPkg;
        public String runtime;
        public String date;
    }
}
