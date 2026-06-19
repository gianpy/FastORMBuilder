package org.fastormbuilder.plugin.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.fastormbuilder.plugin.FastBuilderCoreService;
import org.fastormbuilder.plugin.model.ConnectionProfile;
import org.fastormbuilder.plugin.model.DbNode;
import org.fastormbuilder.plugin.model.DriverType;
import org.fastormbuilder.plugin.storage.FastBuilderStorage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FastBuilderSwingPanel extends JPanel {

    private final Project project;
    private final FastBuilderCoreService service;
    private final FastBuilderStorage storage;

    private final DefaultListModel<ConnectionProfile> connListModel = new DefaultListModel<>();
    private final JBList<ConnectionProfile> connList = new JBList<>(connListModel);
    private String activeConnectionId;

    private final DefaultTreeModel tableTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Schemas"));
    private final JTree tableTree = new JTree(tableTreeModel);
    private final ComboBox<String> modeCombo = new ComboBox<>(new String[]{"mybatis", "jpa", "hibernate", "yorm", "sequelize", "knexjs", "prisma", "typeorm"});
    private final JTextField modelPkgField = new JTextField(20);
    private final JTextField mapperPkgField = new JTextField(20);
    private final JTextField xmlPkgField = new JTextField(20);
    private final ComboBox<String> runtimeCombo = new ComboBox<>(new String[]{"MyBatis3DynamicSql", "MyBatis3", "MyBatis3Simple"});
    private final JCheckBox lombokCheck = new JCheckBox("Lombok");

    private final JBLabel statusLabel = new JBLabel("Ready");
    private final JBLabel connStatusLabel = new JBLabel("Not connected");

    private static final java.util.Map<DriverType, Integer> DEFAULT_PORTS = new java.util.LinkedHashMap<>();
    static {
        DEFAULT_PORTS.put(DriverType.MySQL, 3306);
        DEFAULT_PORTS.put(DriverType.PostgreSQL, 5432);
        DEFAULT_PORTS.put(DriverType.Oracle_SID, 1521);
        DEFAULT_PORTS.put(DriverType.Oracle_Service, 1521);
        DEFAULT_PORTS.put(DriverType.MariaDB, 3306);
        DEFAULT_PORTS.put(DriverType.SQLite, 0);
        DEFAULT_PORTS.put(DriverType.DuckDB, 0);
    }

    public FastBuilderSwingPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.service = FastBuilderCoreService.getInstance(project);
        this.storage = FastBuilderStorage.getInstance(project);

        JBTabbedPane tabs = new JBTabbedPane();
        tabs.addTab("  Connections  ", buildConnectionsTab());
        tabs.addTab("  Generate  ", buildGenerateTab());
        tabs.addTab("  History  ", buildHistoryTab());
        add(tabs, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0),
                JBUI.Borders.empty(4, 8)));
        statusLabel.setForeground(JBColor.GRAY);
        connStatusLabel.setForeground(new JBColor(new Color(100, 180, 100), new Color(100, 180, 100)));
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(connStatusLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        loadConnections();
    }

    // ============================= Connections Tab =============================

    private JPanel buildConnectionsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        connList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                ConnectionProfile c = (ConnectionProfile) value;
                String icon = getDriverEmoji(c.getDriverType());
                String text = icon + "  " + c.getName() + "  (" + (c.getDriverType() != null ? c.getDriverType().name() : "?") + ")";
                JLabel label = (JLabel) super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
                label.setBorder(JBUI.Borders.empty(6, 10));
                if (c.getId() != null && c.getId().equals(activeConnectionId)) {
                    label.setForeground(new JBColor(new Color(80, 160, 80), new Color(120, 220, 120)));
                }
                return label;
            }
        });
        connList.setFixedCellHeight(32);

        // Toolbar
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(4, 6)));
        JButton addBtn = createToolButton("+ Add");
        JButton editBtn = createToolButton("Edit");
        JButton deleteBtn = createToolButton("Delete");
        JButton testBtn = createToolButton("Test");
        JButton connectBtn = createToolButton("▶ Connect");
        connectBtn.setForeground(new JBColor(new Color(60, 140, 60), new Color(100, 200, 100)));

        addBtn.addActionListener(e -> showConnectionDialog(null));
        editBtn.addActionListener(e -> { if (connList.getSelectedValue() != null) showConnectionDialog(connList.getSelectedValue()); });
        deleteBtn.addActionListener(e -> deleteSelectedConnection());
        testBtn.addActionListener(e -> { if (connList.getSelectedValue() != null) testConnection(connList.getSelectedValue()); });
        connectBtn.addActionListener(e -> { if (connList.getSelectedValue() != null) doConnect(connList.getSelectedValue().getId()); });

        toolbar.add(addBtn);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(editBtn);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(deleteBtn);
        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(testBtn);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(connectBtn);
        toolbar.add(Box.createHorizontalGlue());

        // Double-click to connect
        connList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && connList.getSelectedValue() != null) {
                    doConnect(connList.getSelectedValue().getId());
                }
            }
        });

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JBScrollPane(connList), BorderLayout.CENTER);
        return panel;
    }

    private JButton createToolButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusable(false);
        btn.setMargin(JBUI.insets(2, 8));
        return btn;
    }

    private String getDriverEmoji(DriverType dt) {
        if (dt == null) return "🗄";
        switch (dt) {
            case MySQL: return "🐬";
            case PostgreSQL: return "🐘";
            case Oracle_SID: case Oracle_Service: return "🔴";
            case MariaDB: return "🦭";
            case SQLite: return "🪶";
            case DuckDB: return "🦆";
            default: return "🗄";
        }
    }

    private void loadConnections() {
        connListModel.clear();
        List<ConnectionProfile> conns = service.getConnections();
        if (conns != null) for (ConnectionProfile c : conns) connListModel.addElement(c);
    }

    private void deleteSelectedConnection() {
        ConnectionProfile sel = connList.getSelectedValue();
        if (sel == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Delete connection '" + sel.getName() + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            List<ConnectionProfile> conns = service.getConnections();
            conns.removeIf(x -> x.getId().equals(sel.getId()));
            service.saveConnections(conns);
            loadConnections();
            setStatus("Connection deleted");
        }
    }

    private void showConnectionDialog(ConnectionProfile existing) {
        JTextField nameField = new JTextField(existing != null ? existing.getName() : "New Connection", 25);
        ComboBox<DriverType> driverCombo = new ComboBox<>(DriverType.values());
        if (existing != null && existing.getDriverType() != null) driverCombo.setSelectedItem(existing.getDriverType());
        JTextField hostField = new JTextField(existing != null && existing.getHost() != null ? existing.getHost() : "localhost", 25);
        JTextField portField = new JTextField(existing != null ? String.valueOf(existing.getPort()) : "3306", 8);
        JTextField dbField = new JTextField(existing != null && existing.getDatabase() != null ? existing.getDatabase() : "", 25);
        JTextField userField = new JTextField(existing != null && existing.getUserName() != null ? existing.getUserName() : "", 25);
        JPasswordField pwField = new JPasswordField(existing != null && existing.getPassword() != null ? existing.getPassword() : "", 25);

        // Update port when driver changes
        driverCombo.addActionListener(e -> {
            DriverType sel = (DriverType) driverCombo.getSelectedItem();
            if (sel != null && DEFAULT_PORTS.containsKey(sel)) {
                portField.setText(String.valueOf(DEFAULT_PORTS.get(sel)));
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(JBUI.Borders.empty(8));
        GridBagConstraints l = new GridBagConstraints();
        l.anchor = GridBagConstraints.EAST;
        l.insets = JBUI.insets(4, 4, 4, 8);
        GridBagConstraints f = new GridBagConstraints();
        f.fill = GridBagConstraints.HORIZONTAL;
        f.weightx = 1;
        f.insets = JBUI.insets(4, 0, 4, 4);

        int row = 0;
        l.gridx = 0; l.gridy = row; form.add(new JBLabel("Name:"), l);
        f.gridx = 1; f.gridy = row++; form.add(nameField, f);
        l.gridy = row; form.add(new JBLabel("Driver:"), l);
        f.gridy = row++; form.add(driverCombo, f);
        l.gridy = row; form.add(new JBLabel("Host:"), l);
        f.gridy = row++; form.add(hostField, f);
        l.gridy = row; form.add(new JBLabel("Port:"), l);
        f.gridy = row++; form.add(portField, f);
        l.gridy = row; form.add(new JBLabel("Database:"), l);
        f.gridy = row++; form.add(dbField, f);
        l.gridy = row; form.add(new JBLabel("User:"), l);
        f.gridy = row++; form.add(userField, f);
        l.gridy = row; form.add(new JBLabel("Password:"), l);
        f.gridy = row; form.add(pwField, f);

        // Let the dialog size itself naturally based on content
        int result = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this), form,
                existing != null && existing.getId() != null && !existing.getId().isEmpty() ? "Edit Connection" : "New Connection",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            ConnectionProfile c = existing != null ? existing : new ConnectionProfile();
            if (c.getId() == null || c.getId().isEmpty()) c.setId(UUID.randomUUID().toString().replace("-", ""));
            c.setName(nameField.getText().trim());
            c.setDriverType((DriverType) driverCombo.getSelectedItem());
            c.setHost(hostField.getText().trim());
            try { c.setPort(Integer.parseInt(portField.getText().trim())); } catch (NumberFormatException ignored) {}
            c.setDatabase(dbField.getText().trim());
            c.setUserName(userField.getText().trim());
            c.setPassword(new String(pwField.getPassword()));
            c.setActive(true);

            if (c.getDriverType() != null) {
                String url = c.getDriverType().getUrlPattern()
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
            loadConnections();
            setStatus("Connection saved: " + c.getName());
        }
    }

    private void testConnection(ConnectionProfile profile) {
        setStatus("Testing connection...");
        new Task.Backgroundable(project, "Testing Connection") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    ConnectionProfile dto = profile.clone();
                    String pw = service.getPassword(dto);
                    if (pw != null && !pw.isEmpty()) dto.setPassword(pw);
                    service.testConnection(dto);
                    ApplicationManager.getApplication().invokeLater(() -> setStatus("✓ Connection successful!"));
                } catch (SQLException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> setStatus("✗ Failed: " + ex.getMessage()));
                }
            }
        }.queue();
    }

    // ============================= Generate Tab =============================

    private JPanel buildGenerateTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        // Top bar: mode + options
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topBar.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));
        topBar.add(new JBLabel("Mode:"));
        topBar.add(modeCombo);
        topBar.add(lombokCheck);
        JButton generateBtn = createToolButton("⚡ Generate");
        generateBtn.setForeground(new JBColor(new Color(60, 60, 200), new Color(130, 130, 255)));
        generateBtn.addActionListener(e -> doGenerate());
        topBar.add(Box.createHorizontalStrut(16));
        topBar.add(generateBtn);

        // Center: table tree
        tableTree.setRootVisible(false);
        tableTree.setShowsRootHandles(true);
        tableTree.setToggleClickCount(1);
        JBScrollPane treeScroll = new JBScrollPane(tableTree);

        // Bottom: package config
        JPanel outputPanel = new JPanel(new GridBagLayout());
        outputPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0),
                JBUI.Borders.empty(8, 10)));
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.EAST;
        lc.insets = JBUI.insets(3, 4, 3, 6);
        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1;
        fc.insets = JBUI.insets(3, 0, 3, 4);

        int row = 0;
        lc.gridx = 0; lc.gridy = row; outputPanel.add(new JBLabel("Model Pkg:"), lc);
        fc.gridx = 1; fc.gridy = row++; outputPanel.add(modelPkgField, fc);
        lc.gridy = row; outputPanel.add(new JBLabel("Mapper Pkg:"), lc);
        fc.gridy = row++; outputPanel.add(mapperPkgField, fc);
        lc.gridy = row; outputPanel.add(new JBLabel("XML Pkg:"), lc);
        fc.gridy = row++; outputPanel.add(xmlPkgField, fc);
        lc.gridy = row; outputPanel.add(new JBLabel("Runtime:"), lc);
        fc.gridy = row; outputPanel.add(runtimeCombo, fc);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(treeScroll, BorderLayout.CENTER);
        panel.add(outputPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void doConnect(String connId) {
        activeConnectionId = connId;
        setStatus("Connecting...");
        connStatusLabel.setText("Connecting...");
        new Task.Backgroundable(project, "FastORM Builder — Loading schemas") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    List<DbNode> schemas = service.fetchSchemas(connId);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Schemas");
                        for (DbNode schema : schemas) {
                            DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(schema.getName());
                            try {
                                List<DbNode> tables = service.fetchTables(connId, schema.getName());
                                for (DbNode t : tables) {
                                    String prefix = t.getType() == DbNode.NodeType.VIEW ? "👁 " : "▦ ";
                                    schemaNode.add(new DefaultMutableTreeNode(new TableNodeData(schema.getName(), t.getName(), t.getType() == DbNode.NodeType.VIEW)));
                                }
                            } catch (SQLException ignored) {}
                            root.add(schemaNode);
                        }
                        tableTreeModel.setRoot(root);
                        // Expand first schema
                        if (root.getChildCount() > 0) {
                            tableTree.expandPath(new TreePath(new Object[]{root, root.getChildAt(0)}));
                        }
                        connStatusLabel.setText("Connected (" + schemas.size() + " schemas)");
                        setStatus("Connected — select tables and click Generate");
                        loadConnections(); // refresh highlighting
                    });
                } catch (SQLException ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        connStatusLabel.setText("Failed");
                        setStatus("✗ " + ex.getMessage());
                    });
                }
            }
        }.queue();
    }

    private void doGenerate() {
        if (activeConnectionId == null) {
            setStatus("Connect to a database first");
            return;
        }

        TreePath[] paths = tableTree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            setStatus("Select at least one table in the tree");
            return;
        }

        List<String> selectedTables = new ArrayList<>();
        for (TreePath path : paths) {
            Object last = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (last instanceof TableNodeData) {
                TableNodeData td = (TableNodeData) last;
                selectedTables.add(td.schema + "." + td.tableName);
            }
        }

        if (selectedTables.isEmpty()) {
            setStatus("Select table nodes (not schema nodes)");
            return;
        }

        String mode = (String) modeCombo.getSelectedItem();
        String modelPkg = modelPkgField.getText().trim();
        String mapperPkg = mapperPkgField.getText().trim();
        String xmlPkg = xmlPkgField.getText().trim();
        String runtime = (String) runtimeCombo.getSelectedItem();
        boolean lombok = lombokCheck.isSelected();

        setStatus("⚡ Generating " + selectedTables.size() + " tables...");

        new Task.Backgroundable(project, "FastORM Builder — Generating") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    String schema = null;
                    List<org.fastormbuilder.plugin.model.TableSpec> tables = new ArrayList<>();
                    for (String entry : selectedTables) {
                        int dot = entry.indexOf('.');
                        String s = dot > 0 ? entry.substring(0, dot) : "";
                        String t = dot > 0 ? entry.substring(dot + 1) : entry;
                        if (schema == null) schema = s;
                        org.fastormbuilder.plugin.model.TableSpec ts = new org.fastormbuilder.plugin.model.TableSpec(s, t, null);
                        ts.setDomainName(org.mybatis.generator.internal.util.JavaBeansUtil.getCamelCaseString(t, true));
                        tables.add(ts);
                    }

                    ConnectionProfile conn = service.getConnectionWithPassword(activeConnectionId);
                    conn.setDatabase(schema);
                    String base = project.getBasePath();

                    if ("jpa".equals(mode)) {
                        List<String> tableNames = new ArrayList<>();
                        for (var ts : tables) tableNames.add(ts.getTableName());
                        String src = resolveSrc(base);
                        new org.fastormbuilder.plugin.generator.JpaEntityGenerator(
                                conn, modelPkg.isEmpty() ? "entity" : modelPkg, mapperPkg, src, lombok, false)
                                .generate(tableNames, schema);
                    } else if ("hibernate".equals(mode)) {
                        List<String> tableNames = new ArrayList<>();
                        for (var ts : tables) tableNames.add(ts.getTableName());
                        String src = resolveSrc(base);
                        String res = resolveRes(base);
                        new org.fastormbuilder.plugin.generator.HibernateEntityGenerator(
                                conn, modelPkg.isEmpty() ? "entity" : modelPkg, mapperPkg, src, res, lombok, false, false, false)
                                .generate(tableNames, schema);
                    } else if ("yorm".equals(mode)) {
                        List<String> tableNames = new ArrayList<>();
                        for (var ts : tables) tableNames.add(ts.getTableName());
                        String src = resolveSrc(base);
                        new org.fastormbuilder.plugin.generator.YormRecordGenerator(
                                conn, modelPkg.isEmpty() ? "record" : modelPkg, src)
                                .generate(tableNames, schema);
                    } else if ("mybatis".equals(mode)) {
                        org.fastormbuilder.plugin.generator.GenerationParams params = service.getLastParams();
                        params.setDefaultParameters(service.getDefaults());
                        params.setSelectedTables(tables);
                        String src = resolveSrc(base);
                        String res = resolveRes(base);
                        params.getJavaModelConfig().setTargetProject(src);
                        params.getJavaModelConfig().setTargetPackage(modelPkg.isEmpty() ? "model" : modelPkg);
                        params.getJavaClientConfig().setTargetProject(src);
                        params.getJavaClientConfig().setTargetPackage(mapperPkg.isEmpty() ? "mapper" : mapperPkg);
                        params.getSqlMapConfig().setTargetProject(res);
                        params.getSqlMapConfig().setTargetPackage(xmlPkg.isEmpty() ? "sqlmap" : xmlPkg);
                        if (runtime != null && !runtime.isEmpty()) params.setTargetRuntime(runtime);

                        org.mybatis.generator.config.JDBCConnectionConfiguration jdbc = params.getJdbcConfig();
                        jdbc.setDriverClass(conn.getDriverType() != null ? conn.getDriverType().getDriverClass() : "");
                        jdbc.setConnectionURL(new org.fastormbuilder.plugin.database.UrlBuilder(conn).buildUrl());
                        jdbc.setUserId(conn.getUserName());
                        jdbc.setPassword(conn.getPassword());
                        params.setConnectionProfile(conn);
                        service.stashParams(params);
                        org.fastormbuilder.plugin.generator.GeneratorRunner.generate(params,
                                new org.fastormbuilder.plugin.generator.callback.IndicatorCallback(indicator));
                    } else {
                        // JS ORM modes
                        List<String> tableNames = new ArrayList<>();
                        for (var ts : tables) tableNames.add(ts.getTableName());
                        String src = resolveSrc(base);
                        new org.fastormbuilder.plugin.generator.JsOrmGenerator(conn, src, false, mode)
                                .generate(tableNames, schema);
                    }

                    VirtualFile dir = ProjectUtil.guessProjectDir(project);
                    if (dir != null) VfsUtil.markDirtyAndRefresh(true, true, true, dir);
                    storage.recordGeneration(activeConnectionId, selectedTables, modelPkg, mapperPkg, xmlPkg, mode, "");

                    int cnt = selectedTables.size();
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setStatus("✓ Generated " + cnt + (cnt > 1 ? " tables" : " table") + " successfully");
                        refreshHistory();
                    });
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            setStatus("✗ Error: " + e.getMessage()));
                }
            }
        }.queue();
    }

    private String resolveSrc(String base) {
        String src = base + "/src/main/java";
        return new java.io.File(src).exists() ? src : base;
    }

    private String resolveRes(String base) {
        String res = base + "/src/main/resources";
        return new java.io.File(res).exists() ? res : base;
    }

    // ============================= History Tab =============================

    private final DefaultTreeModel historyTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("History"));
    private final JTree historyTree = new JTree(historyTreeModel);

    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());

        historyTree.setRootVisible(false);
        historyTree.setShowsRootHandles(true);

        JPanel topBar = new JPanel();
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.X_AXIS));
        topBar.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(4, 6)));
        JButton refreshBtn = createToolButton("↻ Refresh");
        refreshBtn.addActionListener(e -> refreshHistory());
        topBar.add(refreshBtn);
        topBar.add(Box.createHorizontalGlue());

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JBScrollPane(historyTree), BorderLayout.CENTER);

        refreshHistory();
        return panel;
    }

    private void refreshHistory() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("History");
        List<FastBuilderStorage.HistoryEntry> entries = storage.getAllHistory(30);
        for (FastBuilderStorage.HistoryEntry e : entries) {
            String label = (e.date != null ? e.date : "?") + "  —  " + (e.runtime != null ? e.runtime : "?");
            DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(label);
            // Add tables as children
            if (e.tables != null && !e.tables.isEmpty()) {
                String[] tables = e.tables.split(",");
                for (String t : tables) {
                    entryNode.add(new DefaultMutableTreeNode("▦ " + t.trim()));
                }
            }
            // Add package info
            if (e.modelPkg != null && !e.modelPkg.isEmpty())
                entryNode.add(new DefaultMutableTreeNode("📦 model: " + e.modelPkg));
            if (e.mapperPkg != null && !e.mapperPkg.isEmpty())
                entryNode.add(new DefaultMutableTreeNode("📦 mapper: " + e.mapperPkg));
            if (e.xmlPkg != null && !e.xmlPkg.isEmpty())
                entryNode.add(new DefaultMutableTreeNode("📦 xml: " + e.xmlPkg));
            root.add(entryNode);
        }
        if (entries.isEmpty()) {
            root.add(new DefaultMutableTreeNode("No generation history yet"));
        }
        historyTreeModel.setRoot(root);
        // Expand first few entries
        for (int i = 0; i < Math.min(3, historyTree.getRowCount()); i++) {
            historyTree.expandRow(i);
        }
    }

    // ============================= Utils =============================

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void dispose() { /* no-op */ }

    /** Data holder for table nodes in the tree */
    private static class TableNodeData {
        final String schema;
        final String tableName;
        final boolean isView;

        TableNodeData(String schema, String tableName, boolean isView) {
            this.schema = schema;
            this.tableName = tableName;
            this.isView = isView;
        }

        @Override
        public String toString() {
            return (isView ? "👁 " : "▦ ") + tableName;
        }
    }
}
