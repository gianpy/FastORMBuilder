package org.fastormbuilder.plugin.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.fastormbuilder.plugin.FastBuilderCoreService;
import org.fastormbuilder.plugin.model.ConnectionProfile;
import org.fastormbuilder.plugin.model.DbNode;
import org.fastormbuilder.plugin.model.DriverType;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation", "removal"})
public class FastBuilderWebPanel extends JPanel {
    private final JBCefBrowser browser;
    private final JBCefJSQuery bridgeQuery;
    private final Project project;
    private final FastBuilderCoreService service;
    private final Gson gson = new Gson();
    private final org.fastormbuilder.plugin.storage.FastBuilderStorage storage;
    private String activeConnectionId;

    public FastBuilderWebPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.service = FastBuilderCoreService.getInstance(project);
        this.storage = org.fastormbuilder.plugin.storage.FastBuilderStorage.getInstance(project);
        this.browser = new JBCefBrowser();
        this.bridgeQuery = JBCefJSQuery.create(browser);

        bridgeQuery.addHandler(request -> {
            handleBridgeCall(request);
            return new JBCefJSQuery.Response("");
        });

        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                injectBridge(cefBrowser);
                // Delay slightly to ensure React has mounted and registered window handlers
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            pushConnections();
                            pushGenerationHistory(null);
                            pushPreferences();
                            pushModules();
                        });
                    }
                }, 300);
            }
        }, browser.getCefBrowser());

        // Extract webview resources to a temp directory and load via file:// URL
        try {
            java.io.File tmpDir = new java.io.File(System.getProperty("java.io.tmpdir"), "fastbuilder-webview");
            if (!tmpDir.exists()) tmpDir.mkdirs();

            String[] resources = {"fastbuilder.html", "react.production.min.js", "react-dom.production.min.js", "app.compiled.js"};
            for (String res : resources) {
                java.io.InputStream is = getClass().getResourceAsStream("/webview/" + res);
                if (is != null) {
                    java.nio.file.Files.write(new java.io.File(tmpDir, res).toPath(), is.readAllBytes(),
                            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
                }
            }

            java.io.File htmlFile = new java.io.File(tmpDir, "fastbuilder.html");
            browser.loadURL(htmlFile.toURI().toString());
        } catch (Exception e) {
            browser.loadHTML("<html><body><h3>Failed to load UI: " + e.getMessage() + "</h3></body></html>");
        }
        add(browser.getComponent(), BorderLayout.CENTER);
    }

    private void injectBridge(CefBrowser cefBrowser) {
        String js = "window.__fastbuilder_bridge = { call: function(action, data) { " +
                bridgeQuery.inject("action + '|' + data") + " } };";
        cefBrowser.executeJavaScript(js, "", 0);
    }

    private void handleBridgeCall(String request) {
        int sep = request.indexOf('|');
        String action = sep > 0 ? request.substring(0, sep) : request;
        String dataStr = sep > 0 ? request.substring(sep + 1) : "{}";

        ApplicationManager.getApplication().invokeLater(() -> {
            switch (action) {
                case "settings":
                    pushConnections();
                    pushPreferences();
                    break;
                case "saveConnection":
                    saveConnection(dataStr);
                    break;
                case "deleteConnection":
                    deleteConnection(dataStr);
                    break;
                case "testConnection":
                    testConnection(dataStr);
                    break;
                case "connect":
                    connectAndLoadSchemas(dataStr);
                    break;
                case "generate":
                    triggerGenerate(dataStr);
                    break;
                case "saveDefaults":
                    saveDefaults(dataStr);
                    break;
                case "exportLogs":
                    exportLogs(dataStr);
                    break;
            }
        });
    }

    private void pushConnections() {
        List<ConnectionProfile> conns = service.getConnections();
        if (conns == null) conns = new ArrayList<>();
        String json = gson.toJson(conns);
        execJs("window.updateConnections(" + json + ")");
    }

    private void saveConnection(String dataStr) {
        JsonObject obj = JsonParser.parseString(dataStr).getAsJsonObject();
        ConnectionProfile c = new ConnectionProfile();
        String id = obj.has("id") && !obj.get("id").getAsString().isEmpty() ? obj.get("id").getAsString() : UUID.randomUUID().toString().replace("-", "");
        c.setId(id);
        c.setName(getStr(obj, "name"));
        c.setDriverType(DriverType.valueOf(getStr(obj, "driverType")));
        c.setHost(getStr(obj, "host"));
        c.setPort(obj.has("port") ? obj.get("port").getAsInt() : 3306);
        c.setDatabase(getStr(obj, "database"));
        c.setUserName(getStr(obj, "userName"));
        c.setPassword(getStr(obj, "password"));
        c.setActive(obj.has("active") && obj.get("active").getAsBoolean());
        if (obj.has("targetRuntime") && !obj.get("targetRuntime").getAsString().isEmpty())
            c.setTargetRuntime(obj.get("targetRuntime").getAsString());
        if (obj.has("clientType") && !obj.get("clientType").getAsString().isEmpty())
            c.setClientType(obj.get("clientType").getAsString());
        if (obj.has("modelType") && !obj.get("modelType").getAsString().isEmpty())
            c.setModelType(obj.get("modelType").getAsString());
        c.setUseLombok(obj.has("useLombok") && obj.get("useLombok").getAsBoolean() ? Boolean.TRUE : null);

        // Build URL
        DriverType dt = c.getDriverType();
        if (dt != null) {
            String url = dt.getUrlPattern()
                    .replace("${host}", c.getHost() != null ? c.getHost() : "")
                    .replace("${port}", String.valueOf(c.getPort()))
                    .replace("${db}", c.getDatabase() != null ? c.getDatabase() : "");
            c.setUrl(url);
        }

        List<ConnectionProfile> conns = service.getConnections();
        if (conns == null) conns = new ArrayList<>();
        conns.removeIf(x -> x.getId().equals(c.getId()));
        conns.add(c);
        service.saveConnections(conns);
        pushConnections();
    }

    private void deleteConnection(String dataStr) {
        JsonObject obj = JsonParser.parseString(dataStr).getAsJsonObject();
        String id = getStr(obj, "id");
        List<ConnectionProfile> conns = service.getConnections();
        if (conns != null) {
            conns.removeIf(x -> x.getId().equals(id));
            service.saveConnections(conns);
        }
        pushConnections();
    }

    private void testConnection(String dataStr) {
        JsonObject obj = JsonParser.parseString(dataStr).getAsJsonObject();
        ConnectionProfile c = new ConnectionProfile();
        c.setDriverType(DriverType.valueOf(getStr(obj, "driverType")));
        c.setHost(getStr(obj, "host"));
        c.setPort(obj.has("port") ? obj.get("port").getAsInt() : 3306);
        c.setDatabase(getStr(obj, "database"));
        c.setUserName(getStr(obj, "userName"));
        c.setPassword(getStr(obj, "password"));
        DriverType dt = c.getDriverType();
        if (dt != null) {
            c.setUrl(dt.getUrlPattern()
                    .replace("${host}", c.getHost() != null ? c.getHost() : "")
                    .replace("${port}", String.valueOf(c.getPort()))
                    .replace("${db}", c.getDatabase() != null ? c.getDatabase() : ""));
        }
        try {
            service.testConnection(c);
            execJs("window.setStatus('Connection successful!')");
        } catch (SQLException ex) {
            execJs("window.setStatus('Connection failed: " + escapeJs(ex.getMessage()) + "')");
        }
    }

    private void connectAndLoadSchemas(String dataStr) {
        JsonObject obj = JsonParser.parseString(dataStr).getAsJsonObject();
        String id = getStr(obj, "id");
        activeConnectionId = id;
        storage.logInfo("Connect", "Connecting to " + id);
        try {
            List<DbNode> schemas = service.fetchSchemas(id);
            storage.logInfo("Connect", "Found " + schemas.size() + " schemas/databases");
            List<SchemaDto> dtos = new ArrayList<>();
            for (DbNode s : schemas) {
                SchemaDto dto = new SchemaDto();
                dto.name = s.getName();
                try {
                    dto.tables = service.fetchTables(id, s.getName()).stream()
                            .map(t -> {
                                TableDto td = new TableDto();
                                td.name = t.getName();
                                td.comment = t.getComment();
                                td.isView = t.getType() == DbNode.NodeType.VIEW;
                                return td;
                            })
                            .collect(Collectors.toList());
                } catch (SQLException ignored) {
                    dto.tables = new ArrayList<>();
                }
                dtos.add(dto);
            }
            execJs("window.updateSchemas(" + gson.toJson(dtos) + ")");
            execJs("window.setConnectionStatus('Connected: " + id.substring(0, Math.min(8, id.length())) + "...')");
            pushPkgHistory(id);
            pushGenerationHistory(id);
        } catch (SQLException ex) {
            storage.logError("Connect", "Connection failed: " + ex.getMessage());
            execJs("window.setStatus('Error: " + escapeJs(ex.getMessage()) + "')");
        }
    }

    private void pushPkgHistory(String connId) {
        java.util.Map<String, Object> h = new java.util.HashMap<>();
        h.put("modelPkgs", storage.getPkgHistory(connId, "model"));
        h.put("mapperPkgs", storage.getPkgHistory(connId, "mapper"));
        h.put("xmlPkgs", storage.getPkgHistory(connId, "xml"));
        execJs("window.updatePkgHistory(" + gson.toJson(h) + ")");
    }

    private void pushGenerationHistory(String connId) {
        List<org.fastormbuilder.plugin.storage.FastBuilderStorage.HistoryEntry> entries =
                (connId != null && !connId.isEmpty()) ? storage.getHistory(connId, 20) : storage.getAllHistory(20);
        storage.logDebug("History", "pushGenerationHistory connId=" + connId + " entries=" + entries.size());

        // Resolve driverType for each connection
        java.util.Map<String, String> connDriverCache = new java.util.HashMap<>();
        List<ConnectionProfile> conns = service.getConnections();
        if (conns != null) for (var c : conns)
            connDriverCache.put(c.getId(), c.getDriverType() != null ? c.getDriverType().name() : "");

        List<java.util.Map<String, Object>> list = new ArrayList<>();
        for (var e : entries) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("tables", e.tables != null ? java.util.Arrays.asList(e.tables.split(",")) : new ArrayList<>());
            m.put("modelPkg", e.modelPkg);
            m.put("mapperPkg", e.mapperPkg);
            m.put("xmlPkg", e.xmlPkg);
            m.put("runtime", e.runtime);
            m.put("date", e.date);
            m.put("driverType", connDriverCache.getOrDefault(e.connectionId, ""));
            list.add(m);
        }
        execJs("window.updateHistory(" + gson.toJson(list) + ")");
    }

    private void triggerGenerate(String dataStr) {
        try {
            triggerGenerateImpl(dataStr);
        } catch (Exception e) {
            storage.logError("Generate", e.getClass().getSimpleName() + ": " + e.getMessage());
            execJs("window.setStatus('Error: " + escapeJs(e.getClass().getSimpleName() + ": " + e.getMessage()) + "')");
        }
    }

    private void triggerGenerateImpl(String dataStr) throws SQLException {
        if (activeConnectionId == null || activeConnectionId.isEmpty()) {
            execJs("window.setStatus('Error: No active connection. Connect first.')");
            return;
        }
        JsonObject obj = JsonParser.parseString(dataStr).getAsJsonObject();
        com.google.gson.JsonArray tablesArr = obj.has("tables") ? obj.getAsJsonArray("tables") : null;
        if (tablesArr == null || tablesArr.size() == 0) {
            execJs("window.setStatus('Error: No tables selected')");
            return;
        }

        String mode = getStr(obj, "mode");
        java.util.Set<String> jsOrmModes = new java.util.HashSet<>(java.util.Arrays.asList("sequelize", "knexjs", "prisma", "typeorm", "bookshelfjs", "waterline", "objectionjs", "mikroorm"));
        if ("jpa".equals(mode)) {
            triggerGenerateJpa(obj, tablesArr);
        } else if ("hibernate".equals(mode)) {
            triggerGenerateHibernate(obj, tablesArr);
        } else if ("yorm".equals(mode)) {
            triggerGenerateYorm(obj, tablesArr);
        } else if (jsOrmModes.contains(mode)) {
            triggerGenerateJsOrm(obj, tablesArr, mode);
        } else {
            triggerGenerateMybatis(obj, tablesArr);
        }
    }

    private void triggerGenerateMybatis(JsonObject obj, com.google.gson.JsonArray tablesArr) {
        String modelPkg = getStr(obj, "modelPkg");
        String mapperPkg = getStr(obj, "mapperPkg");
        String xmlPkg = getStr(obj, "xmlPkg");
        String runtime = getStr(obj, "runtime");
        String client = getStr(obj, "client");
        boolean lombok = obj.has("lombok") && obj.get("lombok").getAsBoolean();

        // Parse tables: "schema.tableName"
        String schema = null;
        List<org.fastormbuilder.plugin.model.TableSpec> tables = new ArrayList<>();
        for (int i = 0; i < tablesArr.size(); i++) {
            String entry = tablesArr.get(i).getAsString();
            int dot = entry.indexOf('.');
            String s = dot > 0 ? entry.substring(0, dot) : "";
            String t = dot > 0 ? entry.substring(dot + 1) : entry;
            if (schema == null) schema = s;
            org.fastormbuilder.plugin.model.TableSpec ts = new org.fastormbuilder.plugin.model.TableSpec(s, t, null);
            ts.setDomainName(org.mybatis.generator.internal.util.JavaBeansUtil.getCamelCaseString(t, true));
            tables.add(ts);
        }

        try {
            ConnectionProfile conn = service.getConnectionWithPassword(activeConnectionId);
            conn.setDatabase(schema);

            org.fastormbuilder.plugin.generator.GenerationParams params = service.getLastParams();
            params.setDefaultParameters(service.getDefaults());
            params.setSelectedTables(tables);

            // Override packages if provided
            String base = resolveBase(obj);
            String src = base + "/src/main/java";
            String res = base + "/src/main/resources";
            if (!new java.io.File(src).exists()) src = base;
            if (!new java.io.File(res).exists()) res = base;

            params.getJavaModelConfig().setTargetProject(src);
            params.getJavaModelConfig().setTargetPackage(modelPkg.isEmpty() ? "model" : modelPkg);
            params.getJavaClientConfig().setTargetProject(src);
            params.getJavaClientConfig().setTargetPackage(mapperPkg.isEmpty() ? "mapper" : mapperPkg);
            params.getSqlMapConfig().setTargetProject(res);
            params.getSqlMapConfig().setTargetPackage(xmlPkg.isEmpty() ? "sqlmap" : xmlPkg);

            if (!runtime.isEmpty()) params.setTargetRuntime(runtime);

            // Set JDBC
            org.mybatis.generator.config.JDBCConnectionConfiguration jdbc = params.getJdbcConfig();
            String driver = conn.getDriverType() != null ? conn.getDriverType().getDriverClass() : "";
            jdbc.setDriverClass(driver);
            jdbc.setConnectionURL(new org.fastormbuilder.plugin.database.UrlBuilder(conn).buildUrl());
            jdbc.setUserId(conn.getUserName());
            jdbc.setPassword(conn.getPassword());
            params.setConnectionProfile(conn);

            service.stashParams(params);

            // Run in background
            new com.intellij.openapi.progress.Task.Backgroundable(project, "FastORM Builder — Generating") {
                @Override
                public void run(@org.jetbrains.annotations.NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    try {
                        List<String> warnings = org.fastormbuilder.plugin.generator.GeneratorRunner.generate(params,
                                new org.fastormbuilder.plugin.generator.callback.IndicatorCallback(indicator));
                        com.intellij.openapi.vfs.VirtualFile dir = com.intellij.openapi.project.ProjectUtil.guessProjectDir(project);
                        if (dir != null) com.intellij.openapi.vfs.VfsUtil.markDirtyAndRefresh(true, true, true, dir);

                        // Record in history DB
                        List<String> tableNames = new ArrayList<>();
                        for (var ts : tables) tableNames.add(ts.getDatabase() + "." + ts.getTableName());
                        storage.recordGeneration(activeConnectionId, tableNames, modelPkg, mapperPkg, xmlPkg, runtime, client);
                        storage.logInfo("Generate", "Generated " + tableNames.size() + " tables: " + String.join(", ", tableNames));

                        int cnt = tables.size();
                        String msg = "Generated " + cnt + (cnt > 1 ? " tables" : " table") + " successfully";
                        if (!warnings.isEmpty()) msg += " (" + warnings.size() + " warnings)";
                        String finalMsg = msg;
                        ApplicationManager.getApplication().invokeLater(() -> {
                            execJs("window.setStatus('" + escapeJs(finalMsg) + "')");
                            pushPkgHistory(activeConnectionId);
                            pushGenerationHistory(activeConnectionId);
                        });
                    } catch (Exception e) {
                        storage.logError("Generate", "Generation failed: " + e.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                execJs("window.setStatus('Error: " + escapeJs(e.getMessage()) + "')"));
                    }
                }
            }.queue();
            execJs("window.setStatus('Generating " + tables.size() + " tables...')");
        } catch (SQLException ex) {
            execJs("window.setStatus('Connection error: " + escapeJs(ex.getMessage()) + "')");
        }
    }

    private void triggerGenerateJpa(JsonObject obj, com.google.gson.JsonArray tablesArr) {
        String entityPkg = getStr(obj, "modelPkg");
        String repoPkg = getStr(obj, "mapperPkg");
        boolean lombok = obj.has("lombok") && obj.get("lombok").getAsBoolean();
        boolean relations = obj.has("jpaRelations") && obj.get("jpaRelations").getAsBoolean();

        String schema = null;
        List<String> tableNames = new ArrayList<>();
        for (int i = 0; i < tablesArr.size(); i++) {
            String entry = tablesArr.get(i).getAsString();
            int dot = entry.indexOf('.');
            if (schema == null) schema = dot > 0 ? entry.substring(0, dot) : "";
            tableNames.add(dot > 0 ? entry.substring(dot + 1) : entry);
        }

        String base = resolveBase(obj);
        String src = base + "/src/main/java";
        if (!new java.io.File(src).exists()) src = base;

        String finalSchema = schema != null ? schema : "";
        String finalSrc = src;
        try {
            ConnectionProfile conn = service.getConnectionWithPassword(activeConnectionId);
            conn.setDatabase(finalSchema);

            new com.intellij.openapi.progress.Task.Backgroundable(project, "JPA Entity Generation") {
                @Override
                public void run(@org.jetbrains.annotations.NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    try {
                        org.fastormbuilder.plugin.generator.JpaEntityGenerator gen =
                                new org.fastormbuilder.plugin.generator.JpaEntityGenerator(
                                        conn, entityPkg.isEmpty() ? "entity" : entityPkg,
                                        repoPkg, finalSrc, lombok, relations);
                        gen.generate(tableNames, finalSchema);

                        com.intellij.openapi.vfs.VirtualFile dir = com.intellij.openapi.project.ProjectUtil.guessProjectDir(project);
                        if (dir != null) com.intellij.openapi.vfs.VfsUtil.markDirtyAndRefresh(true, true, true, dir);

                        List<String> fullNames = new ArrayList<>();
                        for (String t : tableNames) fullNames.add(finalSchema + "." + t);
                        storage.recordGeneration(activeConnectionId, fullNames, entityPkg, repoPkg, "", "JPA", "");
                        storage.logInfo("JPA Generate", "Generated " + tableNames.size() + " entities");

                        String msg = "Generated " + tableNames.size() + " JPA entit" + (tableNames.size() > 1 ? "ies" : "y");
                        ApplicationManager.getApplication().invokeLater(() -> {
                            execJs("window.setStatus('" + escapeJs(msg) + "')");
                            pushGenerationHistory(activeConnectionId);
                        });
                    } catch (Exception e) {
                        storage.logError("JPA Generate", e.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                execJs("window.setStatus('Error: " + escapeJs(e.getMessage()) + "')"));
                    }
                }
            }.queue();
            execJs("window.setStatus('Generating " + tableNames.size() + " JPA entities...')");
        } catch (SQLException ex) {
            execJs("window.setStatus('Connection error: " + escapeJs(ex.getMessage()) + "')");
        }
    }

    private void triggerGenerateHibernate(JsonObject obj, com.google.gson.JsonArray tablesArr) {
        String entityPkg = getStr(obj, "modelPkg");
        String daoPkg = getStr(obj, "mapperPkg");
        boolean lombok = obj.has("lombok") && obj.get("lombok").getAsBoolean();
        boolean relations = obj.has("jpaRelations") && obj.get("jpaRelations").getAsBoolean();
        boolean hbmXml = obj.has("hbmXml") && obj.get("hbmXml").getAsBoolean();
        boolean hibernateCfg = obj.has("hibernateCfg") && obj.get("hibernateCfg").getAsBoolean();

        String schema = null;
        List<String> tableNames = new ArrayList<>();
        for (int i = 0; i < tablesArr.size(); i++) {
            String entry = tablesArr.get(i).getAsString();
            int dot = entry.indexOf('.');
            if (schema == null) schema = dot > 0 ? entry.substring(0, dot) : "";
            tableNames.add(dot > 0 ? entry.substring(dot + 1) : entry);
        }

        String base = resolveBase(obj);
        String src = base + "/src/main/java";
        String res = base + "/src/main/resources";
        if (!new java.io.File(src).exists()) src = base;
        if (!new java.io.File(res).exists()) res = base;

        String finalSchema = schema != null ? schema : "";
        String finalSrc = src;
        String finalRes = res;
        try {
            ConnectionProfile conn = service.getConnectionWithPassword(activeConnectionId);
            conn.setDatabase(finalSchema);

            new com.intellij.openapi.progress.Task.Backgroundable(project, "Hibernate Entity Generation") {
                @Override
                public void run(@org.jetbrains.annotations.NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    try {
                        org.fastormbuilder.plugin.generator.HibernateEntityGenerator gen =
                                new org.fastormbuilder.plugin.generator.HibernateEntityGenerator(
                                        conn, entityPkg.isEmpty() ? "entity" : entityPkg,
                                        daoPkg, finalSrc, finalRes, lombok, relations, hbmXml, hibernateCfg);
                        gen.generate(tableNames, finalSchema);

                        com.intellij.openapi.vfs.VirtualFile dir = com.intellij.openapi.project.ProjectUtil.guessProjectDir(project);
                        if (dir != null) com.intellij.openapi.vfs.VfsUtil.markDirtyAndRefresh(true, true, true, dir);

                        List<String> fullNames = new ArrayList<>();
                        for (String t : tableNames) fullNames.add(finalSchema + "." + t);
                        storage.recordGeneration(activeConnectionId, fullNames, entityPkg, daoPkg, "", "Hibernate", "");
                        storage.logInfo("Hibernate Generate", "Generated " + tableNames.size() + " entities");

                        String msg = "Generated " + tableNames.size() + " Hibernate entit" + (tableNames.size() > 1 ? "ies" : "y");
                        ApplicationManager.getApplication().invokeLater(() -> {
                            execJs("window.setStatus('" + escapeJs(msg) + "')");
                            pushGenerationHistory(activeConnectionId);
                        });
                    } catch (Exception e) {
                        storage.logError("Hibernate Generate", e.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                execJs("window.setStatus('Error: " + escapeJs(e.getMessage()) + "')"));
                    }
                }
            }.queue();
            execJs("window.setStatus('Generating " + tableNames.size() + " Hibernate entities...')");
        } catch (SQLException ex) {
            execJs("window.setStatus('Connection error: " + escapeJs(ex.getMessage()) + "')");
        }
    }

    private void triggerGenerateYorm(JsonObject obj, com.google.gson.JsonArray tablesArr) {
        String recordPkg = getStr(obj, "modelPkg");

        String schema = null;
        List<String> tableNames = new ArrayList<>();
        for (int i = 0; i < tablesArr.size(); i++) {
            String entry = tablesArr.get(i).getAsString();
            int dot = entry.indexOf('.');
            if (schema == null) schema = dot > 0 ? entry.substring(0, dot) : "";
            tableNames.add(dot > 0 ? entry.substring(dot + 1) : entry);
        }

        String base = resolveBase(obj);
        String src = base + "/src/main/java";
        if (!new java.io.File(src).exists()) src = base;

        String finalSchema = schema != null ? schema : "";
        String finalSrc = src;
        try {
            ConnectionProfile conn = service.getConnectionWithPassword(activeConnectionId);
            conn.setDatabase(finalSchema);

            new com.intellij.openapi.progress.Task.Backgroundable(project, "YORM Record Generation") {
                @Override
                public void run(@org.jetbrains.annotations.NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    try {
                        org.fastormbuilder.plugin.generator.YormRecordGenerator gen =
                                new org.fastormbuilder.plugin.generator.YormRecordGenerator(
                                        conn, recordPkg.isEmpty() ? "record" : recordPkg, finalSrc);
                        gen.generate(tableNames, finalSchema);

                        com.intellij.openapi.vfs.VirtualFile dir = com.intellij.openapi.project.ProjectUtil.guessProjectDir(project);
                        if (dir != null) com.intellij.openapi.vfs.VfsUtil.markDirtyAndRefresh(true, true, true, dir);

                        List<String> fullNames = new ArrayList<>();
                        for (String t : tableNames) fullNames.add(finalSchema + "." + t);
                        storage.recordGeneration(activeConnectionId, fullNames, recordPkg, "", "", "YORM", "");
                        storage.logInfo("YORM Generate", "Generated " + tableNames.size() + " records");

                        String msg = "Generated " + tableNames.size() + " YORM record" + (tableNames.size() > 1 ? "s" : "");
                        ApplicationManager.getApplication().invokeLater(() -> {
                            execJs("window.setStatus('" + escapeJs(msg) + "')");
                            pushGenerationHistory(activeConnectionId);
                        });
                    } catch (Exception e) {
                        storage.logError("YORM Generate", e.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                execJs("window.setStatus('Error: " + escapeJs(e.getMessage()) + "')"));
                    }
                }
            }.queue();
            execJs("window.setStatus('Generating " + tableNames.size() + " YORM records...')");
        } catch (SQLException ex) {
            execJs("window.setStatus('Connection error: " + escapeJs(ex.getMessage()) + "')");
        }
    }

    private void triggerGenerateJsOrm(JsonObject obj, com.google.gson.JsonArray tablesArr, String ormMode) {
        boolean typescript = obj.has("typescript") && obj.get("typescript").getAsBoolean();
        boolean overwrite = obj.has("overwrite") && obj.get("overwrite").getAsBoolean();

        String schema = null;
        List<String> tableNames = new ArrayList<>();
        for (int i = 0; i < tablesArr.size(); i++) {
            String entry = tablesArr.get(i).getAsString();
            int dot = entry.indexOf('.');
            if (schema == null) schema = dot > 0 ? entry.substring(0, dot) : "";
            tableNames.add(dot > 0 ? entry.substring(dot + 1) : entry);
        }

        String base = resolveBase(obj);
        String outDir = getStr(obj, "modelPkg");
        if (outDir.isEmpty()) outDir = "models";
        if (!new java.io.File(outDir).isAbsolute()) {
            outDir = base + "/" + outDir;
        }

        // Check for existing files before generating
        if (!overwrite) {
            List<String> existing = new ArrayList<>();
            java.io.File dir = new java.io.File(outDir);
            if (dir.exists()) {
                String ext = typescript ? ".ts" : ".js";
                for (String t : tableNames) {
                    String fileName = toCamelCaseJs(t) + ext;
                    if ("typeorm".equals(ormMode) || "mikroorm".equals(ormMode))
                        fileName = toCamelCaseJs(t) + ".entity" + ext;
                    if (new java.io.File(dir, fileName).exists()) existing.add(fileName);
                }
                // Special files
                if ("prisma".equals(ormMode) && new java.io.File(dir, "schema.prisma").exists())
                    existing.add("schema.prisma");
                if ("knexjs".equals(ormMode)) {
                    String[] files = dir.list((d, n) -> n.endsWith("_create_tables" + ext));
                    if (files != null) for (String f : files) existing.add(f);
                }
            }
            if (!existing.isEmpty()) {
                String fileList = String.join(", ", existing);
                execJs("window.confirmOverwrite('" + escapeJs(fileList) + "')");
                return;
            }
        }

        String finalSchema = schema != null ? schema : "";
        String finalOutDir = outDir;
        try {
            ConnectionProfile conn = service.getConnectionWithPassword(activeConnectionId);
            conn.setDatabase(finalSchema);

            new com.intellij.openapi.progress.Task.Backgroundable(project, ormMode + " Generation") {
                @Override
                public void run(@org.jetbrains.annotations.NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    try {
                        org.fastormbuilder.plugin.generator.JsOrmGenerator gen =
                                new org.fastormbuilder.plugin.generator.JsOrmGenerator(conn, finalOutDir, typescript, ormMode);
                        gen.generate(tableNames, finalSchema);

                        com.intellij.openapi.vfs.VirtualFile dir = com.intellij.openapi.project.ProjectUtil.guessProjectDir(project);
                        if (dir != null) com.intellij.openapi.vfs.VfsUtil.markDirtyAndRefresh(true, true, true, dir);

                        List<String> fullNames = new ArrayList<>();
                        for (String t : tableNames) fullNames.add(finalSchema + "." + t);
                        storage.recordGeneration(activeConnectionId, fullNames, finalOutDir, "", "", ormMode, "");
                        storage.logInfo(ormMode + " Generate", "Generated " + tableNames.size() + " models");

                        String msg = "Generated " + tableNames.size() + " " + ormMode + " model" + (tableNames.size() > 1 ? "s" : "");
                        ApplicationManager.getApplication().invokeLater(() -> {
                            execJs("window.setStatus('" + escapeJs(msg) + "')");
                            pushGenerationHistory(activeConnectionId);
                        });
                    } catch (Exception e) {
                        storage.logError(ormMode + " Generate", e.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                execJs("window.setStatus('Error: " + escapeJs(e.getMessage()) + "')"));
                    }
                }
            }.queue();
            execJs("window.setStatus('Generating " + tableNames.size() + " " + ormMode + " models...')");
        } catch (SQLException ex) {
            execJs("window.setStatus('Connection error: " + escapeJs(ex.getMessage()) + "')");
        }
    }

    private static String toCamelCaseJs(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-') {
                nextUpper = true;
                continue;
            }
            sb.append(nextUpper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            nextUpper = false;
        }
        return sb.toString();
    }

    private void pushPreferences() {
        java.util.Map<String, String> prefs = new java.util.HashMap<>(storage.getAllPreferences());
        // Include service-level defaults so the web UI shows current values
        org.fastormbuilder.plugin.generator.Defaults defaults = service.getDefaults();
        prefs.putIfAbsent("targetRuntime", defaults.getTargetRuntime());
        prefs.putIfAbsent("clientType", defaults.getClientType());
        prefs.putIfAbsent("encoding", defaults.getJavaFileEncoding());
        prefs.putIfAbsent("comment", defaults.getGeneratedComment());
        prefs.putIfAbsent("forceBigDecimals", String.valueOf(defaults.getForceBigDecimals()));
        prefs.putIfAbsent("useJSR310", String.valueOf(defaults.getUseJSR310Types()));
        prefs.putIfAbsent("lombok", String.valueOf(defaults.getUseLombok()));
        prefs.putIfAbsent("useGeneratedAnnotation", String.valueOf(defaults.getUseGeneratedAnnotation()));
        prefs.putIfAbsent("historySize", String.valueOf(defaults.getHistorySize()));
        execJs("window.updateDefaults(" + gson.toJson(prefs) + ")");
    }

    private void saveDefaults(String dataStr) {
        JsonObject obj = JsonParser.parseString(dataStr).getAsJsonObject();
        for (var entry : obj.entrySet()) {
            storage.setPreference(entry.getKey(), entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue().toString());
        }
        // Also persist to service-level Defaults for generation
        org.fastormbuilder.plugin.generator.Defaults defaults = service.getDefaults();
        if (obj.has("targetRuntime")) defaults.setTargetRuntime(obj.get("targetRuntime").getAsString());
        if (obj.has("clientType")) defaults.setClientType(obj.get("clientType").getAsString());
        if (obj.has("encoding")) defaults.setJavaFileEncoding(obj.get("encoding").getAsString());
        if (obj.has("comment")) defaults.setGeneratedComment(obj.get("comment").getAsString());
        if (obj.has("forceBigDecimals")) defaults.setForceBigDecimals(obj.get("forceBigDecimals").getAsBoolean());
        if (obj.has("useJSR310")) defaults.setUseJSR310Types(obj.get("useJSR310").getAsBoolean());
        if (obj.has("lombok")) defaults.setUseLombok(obj.get("lombok").getAsBoolean());
        if (obj.has("useGeneratedAnnotation"))
            defaults.setUseGeneratedAnnotation(obj.get("useGeneratedAnnotation").getAsBoolean());
        if (obj.has("historySize")) defaults.setHistorySize(obj.get("historySize").getAsInt());
        service.saveDefaults(defaults);
        execJs("window.setStatus('Settings saved')");
    }

    private void exportLogs(String dataStr) {
        String logs = storage.exportLogs(500);
        java.io.File logFile = new java.io.File(project.getBasePath(), ".fastbuilder/fastbuilder.log");
        try {
            java.nio.file.Files.writeString(logFile.toPath(), logs);
            execJs("window.setStatus('Logs exported to .fastbuilder/fastbuilder.log')");
        } catch (java.io.IOException e) {
            execJs("window.setStatus('Error exporting logs: " + escapeJs(e.getMessage()) + "')");
        }
    }

    private void execJs(String js) {
        browser.getCefBrowser().executeJavaScript(js, "", 0);
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private String escapeJs(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String resolveBase(JsonObject obj) {
        String targetModule = getStr(obj, "targetModule");
        if (!targetModule.isEmpty()) {
            com.intellij.openapi.module.Module[] modules = com.intellij.openapi.module.ModuleManager.getInstance(project).getModules();
            for (com.intellij.openapi.module.Module m : modules) {
                if (targetModule.equals(m.getName())) {
                    String modulePath = com.intellij.openapi.module.ModuleUtilCore.getModuleDirPath(m);
                    if (modulePath != null) return modulePath;
                }
            }
        }
        return project.getBasePath();
    }

    private void pushModules() {
        com.intellij.openapi.module.Module[] modules = com.intellij.openapi.module.ModuleManager.getInstance(project).getModules();
        List<String> names = new ArrayList<>();
        for (com.intellij.openapi.module.Module m : modules) names.add(m.getName());
        if (names.size() > 1) {
            execJs("window.updateModules(" + gson.toJson(names) + ")");
        }
    }

    public void dispose() {
        bridgeQuery.dispose();
        browser.dispose();
    }

    private static class SchemaDto {
        String name;
        List<TableDto> tables;
    }

    private static class TableDto {
        String name;
        String comment;
        boolean isView;
    }
}
