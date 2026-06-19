import { jsxDEV as _jsxDEV, Fragment as _Fragment } from "react/jsx-dev-runtime";
const {
  useState,
  useEffect,
  useCallback
} = React;
const DRIVERS = [{
  id: 'MySQL',
  label: 'MySQL',
  fileBased: false,
  port: 3306
}, {
  id: 'PostgreSQL',
  label: 'PostgreSQL',
  fileBased: false,
  port: 5432
}, {
  id: 'Oracle_SID',
  label: 'Oracle SID',
  fileBased: false,
  port: 1521
}, {
  id: 'Oracle_Service',
  label: 'Oracle Service',
  fileBased: false,
  port: 1521
}, {
  id: 'MariaDB',
  label: 'MariaDB',
  fileBased: false,
  port: 3306
}, {
  id: 'SQLite',
  label: 'SQLite',
  fileBased: true,
  port: 0
}, {
  id: 'DuckDB',
  label: 'DuckDB',
  fileBased: true,
  port: 0
}, {
  id: 'Custom',
  label: 'Custom',
  fileBased: false,
  port: 1234
}];
const DB_ICONS = {
  MySQL: '🐬',
  PostgreSQL: '🐘',
  Oracle_SID: '🔴',
  Oracle_Service: '🔴',
  MariaDB: '🦭',
  SQLite: '🪶',
  DuckDB: '🦆',
  Custom: '🗄'
};
const driverIcon = type => {
  const emoji = DB_ICONS[type] || DB_ICONS.Custom;
  return /*#__PURE__*/_jsxDEV("span", {
    style: {
      fontSize: 18,
      lineHeight: '24px',
      width: 24,
      textAlign: 'center',
      flexShrink: 0
    },
    children: emoji
  }, void 0, false);
};
const RUNTIMES = ['MyBatis3DynamicSql', 'MyBatis3', 'MyBatis3Simple'];
const CLIENTS = ['XMLMAPPER', 'ANNOTATEDMAPPER', 'MIXEDMAPPER'];
const MODELS = ['FLAT', 'HIERARCHICAL', 'CONDITIONAL'];
function callIde(action, data) {
  if (window.__fastbuilder_bridge) {
    window.__fastbuilder_bridge.call(action, JSON.stringify(data || {}));
  }
}

// --- App ---
function App() {
  const [tab, setTab] = useState('connections');
  const [connections, setConnections] = useState([]);
  const [schemas, setSchemas] = useState([]);
  const [selectedTables, setSelectedTables] = useState([]);
  const [editConn, setEditConn] = useState(null);
  const [showSettings, setShowSettings] = useState(false);
  const [status, setStatus] = useState('Ready');
  const [connStatus, setConnStatus] = useState('No connection');
  const [activeConnId, setActiveConnId] = useState(null);
  const [defaults, setDefaults] = useState({
    targetRuntime: 'MyBatis3DynamicSql',
    clientType: 'XMLMAPPER',
    modelType: 'FLAT',
    encoding: 'UTF-8',
    comment: '',
    forceBigDecimals: false,
    useJSR310: true,
    useLombok: false,
    useGeneratedAnnotation: false,
    historySize: 20
  });
  const [genParams, setGenParams] = useState({
    mode: 'mybatis',
    modelPkg: '',
    mapperPkg: '',
    xmlPkg: '',
    runtime: 'MyBatis3DynamicSql',
    client: 'XMLMAPPER',
    lombok: false
  });
  const [pkgHistory, setPkgHistory] = useState({
    modelPkgs: [],
    mapperPkgs: [],
    xmlPkgs: []
  });
  const [modules, setModules] = useState([]);
  const [history, setHistory] = useState([]);
  const [overwriteFiles, setOverwriteFiles] = useState(null);

  // Expose to Java bridge
  useEffect(() => {
    window.updateConnections = c => {
      setConnections(c);
      setStatus('Connections loaded');
    };
    window.updateSchemas = s => {
      setSchemas(s);
    };
    window.updateHistory = h => {
      setHistory(h);
    };
    window.updateDefaults = d => {
      // Convert string booleans from backend to actual booleans
      const boolKeys = ['forceBigDecimals', 'useJSR310', 'useLombok', 'useGeneratedAnnotation'];
      for (const k of boolKeys) {
        if (k in d) d[k] = d[k] === true || d[k] === 'true';
      }
      if ('historySize' in d) d.historySize = parseInt(d.historySize) || 20;
      setDefaults(prev => ({
        ...prev,
        ...d
      }));
    };
    window.updatePkgHistory = h => {
      setPkgHistory(h);
    };
    window.updateModules = m => {
      setModules(m);
    };
    window.setStatus = s => setStatus(s);
    window.setConnectionStatus = s => setConnStatus(s);
    window.confirmOverwrite = fileList => setOverwriteFiles(fileList);
  }, []);
  const toggleTable = (schema, table) => {
    const key = schema + '.' + table;
    setSelectedTables(prev => prev.includes(key) ? prev.filter(t => t !== key) : [...prev, key]);
  };
  const doGenerate = () => {
    if (selectedTables.length === 0) {
      setStatus('Select at least one table');
      return;
    }
    callIde('generate', {
      ...genParams,
      tables: selectedTables
    });
    setStatus('Generating...');
  };
  const doGenerateOverwrite = () => {
    setOverwriteFiles(null);
    callIde('generate', {
      ...genParams,
      tables: selectedTables,
      overwrite: true
    });
    setStatus('Generating (overwrite)...');
  };
  const doTestConnection = conn => {
    callIde('testConnection', conn);
  };
  const doSaveConnection = conn => {
    callIde('saveConnection', conn);
    setEditConn(null);
    setStatus('Connection saved');
  };
  const doDeleteConnection = id => {
    callIde('deleteConnection', {
      id
    });
    setConnections(prev => prev.filter(c => c.id !== id));
  };
  const doConnect = id => {
    callIde('connect', {
      id
    });
    setActiveConnId(id);
    setConnStatus('Connecting...');
    setTab('generate');
  };
  const doSaveDefaults = () => {
    callIde('saveDefaults', defaults);
    setShowSettings(false);
    setStatus('Settings saved');
  };
  return /*#__PURE__*/_jsxDEV(_Fragment, {
    children: [/*#__PURE__*/_jsxDEV("div", {
      className: "header",
      children: [/*#__PURE__*/_jsxDEV("h1", {
        children: "⚡ FastORM Builder"
      }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
        className: "btn",
        onClick: () => setShowSettings(true),
        style: {
          fontSize: 11,
          padding: '3px 8px'
        },
        children: "⚙ Settings"
      }, void 0, false)]
    }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
      className: "tabs",
      children: ['connections', 'generate', 'history'].map(t => /*#__PURE__*/_jsxDEV("div", {
        className: `tab ${tab === t ? 'active' : ''}`,
        onClick: () => setTab(t),
        children: t.charAt(0).toUpperCase() + t.slice(1)
      }, t, false))
    }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
      className: "content",
      children: [tab === 'connections' && /*#__PURE__*/_jsxDEV(ConnectionsPanel, {
        connections: connections,
        activeConnId: activeConnId,
        onEdit: setEditConn,
        onConnect: doConnect,
        onDelete: doDeleteConnection,
        onAdd: () => setEditConn({
          id: '',
          name: 'New Connection',
          driverType: 'MySQL',
          host: 'localhost',
          port: 3306,
          database: '',
          userName: '',
          password: '',
          active: true,
          targetRuntime: '',
          clientType: '',
          modelType: '',
          useLombok: false
        })
      }, void 0, false), tab === 'generate' && /*#__PURE__*/_jsxDEV(GeneratePanel, {
        schemas: schemas,
        selectedTables: selectedTables,
        toggleTable: toggleTable,
        genParams: genParams,
        setGenParams: setGenParams,
        onGenerate: doGenerate,
        pkgHistory: pkgHistory,
        overwriteFiles: overwriteFiles,
        setOverwriteFiles: setOverwriteFiles,
        doGenerateOverwrite: doGenerateOverwrite,
        modules: modules
      }, void 0, false), tab === 'history' && /*#__PURE__*/_jsxDEV(HistoryPanel, {
        history: history
      }, void 0, false)]
    }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
      className: "status",
      children: [/*#__PURE__*/_jsxDEV("span", {
        children: status
      }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
        children: connStatus
      }, void 0, false)]
    }, void 0, true), editConn && /*#__PURE__*/_jsxDEV(ConnectionModal, {
      conn: editConn,
      onSave: doSaveConnection,
      onTest: doTestConnection,
      onClose: () => setEditConn(null)
    }, void 0, false), showSettings && /*#__PURE__*/_jsxDEV(SettingsModal, {
      defaults: defaults,
      setDefaults: setDefaults,
      onSave: doSaveDefaults,
      onClose: () => setShowSettings(false)
    }, void 0, false)]
  }, void 0, true);
}

// --- Connections Panel ---
function ConnectionsPanel({
  connections,
  activeConnId,
  onEdit,
  onConnect,
  onDelete,
  onAdd
}) {
  if (!connections.length) return /*#__PURE__*/_jsxDEV("div", {
    className: "empty",
    children: [/*#__PURE__*/_jsxDEV("div", {
      className: "ico",
      children: "🔌"
    }, void 0, false), /*#__PURE__*/_jsxDEV("p", {
      children: "No connections configured"
    }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
      className: "btn",
      style: {
        marginTop: 8
      },
      onClick: onAdd,
      children: "+ Add Connection"
    }, void 0, false)]
  }, void 0, true);
  return /*#__PURE__*/_jsxDEV(_Fragment, {
    children: [/*#__PURE__*/_jsxDEV("div", {
      style: {
        display: 'flex',
        justifyContent: 'space-between',
        marginBottom: 8
      },
      children: [/*#__PURE__*/_jsxDEV("span", {
        style: {
          color: 'var(--bright)',
          fontSize: 12
        },
        children: "Database Connections"
      }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
        className: "btn",
        onClick: onAdd,
        children: "+ Add"
      }, void 0, false)]
    }, void 0, true), connections.map(c => /*#__PURE__*/_jsxDEV("div", {
      className: `conn-item ${c.id === activeConnId ? 'active' : ''}`,
      children: [driverIcon(c.driverType), /*#__PURE__*/_jsxDEV("span", {
        className: "name",
        children: c.name
      }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
        className: "type",
        children: c.driverType
      }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
        className: "btn",
        style: {
          padding: '2px 8px',
          fontSize: 11
        },
        onClick: () => onConnect(c.id),
        children: "▶ Connect"
      }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
        className: "btn-s",
        style: {
          padding: '2px 6px'
        },
        onClick: () => onEdit(c),
        children: "✎"
      }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
        className: "btn-d",
        style: {
          padding: '2px 6px'
        },
        onClick: () => onDelete(c.id),
        children: "✕"
      }, void 0, false)]
    }, c.id, true))]
  }, void 0, true);
}

// --- Generate Panel ---
function GeneratePanel({
  schemas,
  selectedTables,
  toggleTable,
  genParams,
  setGenParams,
  onGenerate,
  pkgHistory,
  overwriteFiles,
  setOverwriteFiles,
  doGenerateOverwrite,
  modules
}) {
  const [subTab, setSubTab] = useState('tables');
  const [collapsed, setCollapsed] = useState({});
  const upd = (k, v) => setGenParams(p => ({
    ...p,
    [k]: v
  }));
  const toggleCollapse = name => setCollapsed(p => ({
    ...p,
    [name]: !p[name]
  }));

  // Start schemas folded except the first one
  useEffect(() => {
    if (schemas.length) {
      setCollapsed(prev => {
        const next = {
          ...prev
        };
        schemas.forEach((s, i) => {
          if (!(s.name in next)) next[s.name] = i > 0;
        });
        return next;
      });
    }
  }, [schemas]);
  const JAVA_ORMS = ['MyBatis', 'JPA', 'Hibernate', 'YORM'];
  const JS_ORMS = ['Sequelize', 'Knex.js', 'Prisma', 'TypeORM', 'Bookshelf.js', 'Waterline', 'Objection.js', 'MikroORM'];
  const isJs = genParams.lang === 'js';
  return /*#__PURE__*/_jsxDEV("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      flex: 1,
      overflow: 'hidden'
    },
    children: [/*#__PURE__*/_jsxDEV("div", {
      style: {
        display: 'flex',
        gap: 4,
        padding: '4px 0',
        flexShrink: 0,
        alignItems: 'center'
      },
      children: [/*#__PURE__*/_jsxDEV("button", {
        className: `btn ${!isJs ? '' : 'btn-s'}`,
        style: {
          padding: '3px 8px',
          fontSize: 11
        },
        onClick: () => {
          upd('lang', 'java');
          upd('mode', 'mybatis');
        },
        children: "☕ Java"
      }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
        className: `btn ${isJs ? '' : 'btn-s'}`,
        style: {
          padding: '3px 8px',
          fontSize: 11
        },
        onClick: () => {
          upd('lang', 'js');
          upd('mode', 'sequelize');
        },
        children: "⬡ JS"
      }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
        value: genParams.mode,
        onChange: e => upd('mode', e.target.value),
        style: {
          padding: '3px 6px',
          background: 'var(--surface2)',
          color: 'var(--bright)',
          border: '1px solid var(--border)',
          borderRadius: 4,
          fontSize: 11,
          flex: 1,
          minWidth: 0
        },
        children: (!isJs ? JAVA_ORMS : JS_ORMS).map(o => /*#__PURE__*/_jsxDEV("option", {
          value: o.toLowerCase().replace(/[.\s]/g, ''),
          children: o
        }, o, false))
      }, void 0, false), isJs && /*#__PURE__*/_jsxDEV("select", {
        value: genParams.typescript ? 'ts' : 'js',
        onChange: e => upd('typescript', e.target.value === 'ts'),
        style: {
          padding: '3px 6px',
          background: 'var(--surface2)',
          color: 'var(--bright)',
          border: '1px solid var(--border)',
          borderRadius: 4,
          fontSize: 11,
          width: 52
        },
        children: [/*#__PURE__*/_jsxDEV("option", {
          value: "js",
          children: "JS"
        }, void 0, false), /*#__PURE__*/_jsxDEV("option", {
          value: "ts",
          children: "TS"
        }, void 0, false)]
      }, void 0, true)]
    }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
      className: "sub-tabs",
      children: [/*#__PURE__*/_jsxDEV("div", {
        className: `sub-tab ${subTab === 'tables' ? 'active' : ''}`,
        onClick: () => setSubTab('tables'),
        children: ["📋 Tables (", selectedTables.length, ")"]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: `sub-tab ${subTab === 'output' ? 'active' : ''}`,
        onClick: () => setSubTab('output'),
        children: "⚙ Output Settings"
      }, void 0, false)]
    }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
      className: "sub-content",
      children: overwriteFiles ? /*#__PURE__*/_jsxDEV("div", {
        style: {
          padding: 12
        },
        children: [/*#__PURE__*/_jsxDEV("h3", {
          style: {
            margin: '0 0 8px'
          },
          children: "⚠ Files already exist"
        }, void 0, false), /*#__PURE__*/_jsxDEV("p", {
          style: {
            fontSize: 12,
            margin: '4px 0'
          },
          children: "The following files will be overwritten:"
        }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
          style: {
            background: 'var(--bg)',
            padding: 8,
            borderRadius: 4,
            fontSize: 11,
            maxHeight: 150,
            overflow: 'auto',
            margin: '8px 0'
          },
          children: overwriteFiles.split(', ').map(f => /*#__PURE__*/_jsxDEV("div", {
            children: ["• ", f]
          }, f, true))
        }, void 0, false), /*#__PURE__*/_jsxDEV("p", {
          style: {
            fontSize: 12,
            margin: '8px 0'
          },
          children: "Do you want to overwrite?"
        }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
          style: {
            display: 'flex',
            gap: 8,
            marginTop: 12
          },
          children: [/*#__PURE__*/_jsxDEV("button", {
            className: "btn btn-s",
            onClick: () => setOverwriteFiles(null),
            children: "Cancel"
          }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
            className: "btn",
            onClick: doGenerateOverwrite,
            children: "Overwrite"
          }, void 0, false)]
        }, void 0, true)]
      }, void 0, true) : /*#__PURE__*/_jsxDEV(_Fragment, {
        children: [subTab === 'tables' && (!schemas.length ? /*#__PURE__*/_jsxDEV("div", {
          className: "empty",
          children: [/*#__PURE__*/_jsxDEV("div", {
            className: "ico",
            children: "📋"
          }, void 0, false), /*#__PURE__*/_jsxDEV("p", {
            children: "Connect to a database to see tables"
          }, void 0, false)]
        }, void 0, true) : /*#__PURE__*/_jsxDEV("div", {
          className: "table-tree",
          children: schemas.map(s => /*#__PURE__*/_jsxDEV("div", {
            className: "schema-group",
            children: [/*#__PURE__*/_jsxDEV("div", {
              className: "schema-header",
              onClick: () => toggleCollapse(s.name),
              children: [/*#__PURE__*/_jsxDEV("span", {
                className: "fold-icon",
                children: collapsed[s.name] ? '▶' : '▼'
              }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                children: "🗄"
              }, void 0, false), /*#__PURE__*/_jsxDEV("strong", {
                children: s.name
              }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                className: "schema-count",
                children: [(s.tables || []).filter(t => !t.isView).length, " tables, ", (s.tables || []).filter(t => t.isView).length, " views"]
              }, void 0, true)]
            }, void 0, true), !collapsed[s.name] && (s.tables || []).map(t => /*#__PURE__*/_jsxDEV("div", {
              className: "table-row",
              onClick: () => toggleTable(s.name, t.name),
              children: [/*#__PURE__*/_jsxDEV("input", {
                type: "checkbox",
                checked: selectedTables.includes(s.name + '.' + t.name),
                readOnly: true
              }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                className: "table-icon",
                children: t.isView ? '👁' : '▦'
              }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                className: "table-name",
                children: t.name
              }, void 0, false), t.comment && /*#__PURE__*/_jsxDEV("span", {
                className: "table-comment",
                children: t.comment
              }, void 0, false)]
            }, t.name, true))]
          }, s.name, true))
        }, void 0, false)), subTab === 'output' && genParams.mode === 'mybatis' && /*#__PURE__*/_jsxDEV("div", {
          className: "output-form",
          children: [modules.length > 0 && /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Target Module"
              }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
                value: genParams.targetModule || '',
                onChange: e => upd('targetModule', e.target.value),
                children: [/*#__PURE__*/_jsxDEV("option", {
                  value: "",
                  children: "Project root"
                }, void 0, false), modules.map(m => /*#__PURE__*/_jsxDEV("option", {
                  value: m,
                  children: m
                }, m, false))]
              }, void 0, true)]
            }, void 0, true)
          }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: [/*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Model Package"
              }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
                value: genParams.modelPkg,
                onChange: e => upd('modelPkg', e.target.value),
                placeholder: "com.example.model"
              }, void 0, false), pkgHistory.modelPkgs && pkgHistory.modelPkgs.length > 0 && /*#__PURE__*/_jsxDEV("div", {
                className: "pkg-history",
                children: pkgHistory.modelPkgs.map(p => /*#__PURE__*/_jsxDEV("span", {
                  className: "pkg-chip",
                  onClick: () => upd('modelPkg', p),
                  children: p
                }, p, false))
              }, void 0, false)]
            }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Mapper Package"
              }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
                value: genParams.mapperPkg,
                onChange: e => upd('mapperPkg', e.target.value),
                placeholder: "com.example.mapper"
              }, void 0, false), pkgHistory.mapperPkgs && pkgHistory.mapperPkgs.length > 0 && /*#__PURE__*/_jsxDEV("div", {
                className: "pkg-history",
                children: pkgHistory.mapperPkgs.map(p => /*#__PURE__*/_jsxDEV("span", {
                  className: "pkg-chip",
                  onClick: () => upd('mapperPkg', p),
                  children: p
                }, p, false))
              }, void 0, false)]
            }, void 0, true)]
          }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: [/*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "XML Package"
              }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
                value: genParams.xmlPkg,
                onChange: e => upd('xmlPkg', e.target.value),
                placeholder: "mapper"
              }, void 0, false), pkgHistory.xmlPkgs && pkgHistory.xmlPkgs.length > 0 && /*#__PURE__*/_jsxDEV("div", {
                className: "pkg-history",
                children: pkgHistory.xmlPkgs.map(p => /*#__PURE__*/_jsxDEV("span", {
                  className: "pkg-chip",
                  onClick: () => upd('xmlPkg', p),
                  children: p
                }, p, false))
              }, void 0, false)]
            }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Runtime"
              }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
                value: genParams.runtime,
                onChange: e => upd('runtime', e.target.value),
                children: RUNTIMES.map(r => /*#__PURE__*/_jsxDEV("option", {
                  children: r
                }, r, false))
              }, void 0, false)]
            }, void 0, true)]
          }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: [/*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Client Type"
              }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
                value: genParams.client,
                onChange: e => upd('client', e.target.value),
                children: CLIENTS.map(c => /*#__PURE__*/_jsxDEV("option", {
                  children: c
                }, c, false))
              }, void 0, false)]
            }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "\xA0"
              }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
                className: "check",
                children: [/*#__PURE__*/_jsxDEV("input", {
                  type: "checkbox",
                  checked: genParams.lombok,
                  onChange: e => upd('lombok', e.target.checked)
                }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                  children: "Use Lombok"
                }, void 0, false)]
              }, void 0, true)]
            }, void 0, true)]
          }, void 0, true)]
        }, void 0, true), subTab === 'output' && genParams.mode === 'jpa' && /*#__PURE__*/_jsxDEV("div", {
          className: "output-form",
          children: [modules.length > 0 && /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Target Module"
              }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
                value: genParams.targetModule || '',
                onChange: e => upd('targetModule', e.target.value),
                children: [/*#__PURE__*/_jsxDEV("option", {
                  value: "",
                  children: "Project root"
                }, void 0, false), modules.map(m => /*#__PURE__*/_jsxDEV("option", {
                  value: m,
                  children: m
                }, m, false))]
              }, void 0, true)]
            }, void 0, true)
          }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: [/*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Entity Package"
              }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
                value: genParams.modelPkg,
                onChange: e => upd('modelPkg', e.target.value),
                placeholder: "com.example.entity"
              }, void 0, false), pkgHistory.modelPkgs && pkgHistory.modelPkgs.length > 0 && /*#__PURE__*/_jsxDEV("div", {
                className: "pkg-history",
                children: pkgHistory.modelPkgs.map(p => /*#__PURE__*/_jsxDEV("span", {
                  className: "pkg-chip",
                  onClick: () => upd('modelPkg', p),
                  children: p
                }, p, false))
              }, void 0, false)]
            }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Repository Package"
              }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
                value: genParams.mapperPkg,
                onChange: e => upd('mapperPkg', e.target.value),
                placeholder: "com.example.repository"
              }, void 0, false), pkgHistory.mapperPkgs && pkgHistory.mapperPkgs.length > 0 && /*#__PURE__*/_jsxDEV("div", {
                className: "pkg-history",
                children: pkgHistory.mapperPkgs.map(p => /*#__PURE__*/_jsxDEV("span", {
                  className: "pkg-chip",
                  onClick: () => upd('mapperPkg', p),
                  children: p
                }, p, false))
              }, void 0, false)]
            }, void 0, true)]
          }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: [/*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "\xA0"
              }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
                className: "check",
                children: [/*#__PURE__*/_jsxDEV("input", {
                  type: "checkbox",
                  checked: genParams.lombok,
                  onChange: e => upd('lombok', e.target.checked)
                }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                  children: "Use Lombok"
                }, void 0, false)]
              }, void 0, true)]
            }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "\xA0"
              }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
                className: "check",
                children: [/*#__PURE__*/_jsxDEV("input", {
                  type: "checkbox",
                  checked: genParams.jpaRelations || false,
                  onChange: e => upd('jpaRelations', e.target.checked)
                }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                  children: "Generate Relations"
                }, void 0, false)]
              }, void 0, true)]
            }, void 0, true)]
          }, void 0, true)]
        }, void 0, true), subTab === 'output' && genParams.mode === 'hibernate' && /*#__PURE__*/_jsxDEV("div", {
          className: "output-form",
          children: [modules.length > 0 && /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Target Module"
              }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
                value: genParams.targetModule || '',
                onChange: e => upd('targetModule', e.target.value),
                children: [/*#__PURE__*/_jsxDEV("option", {
                  value: "",
                  children: "Project root"
                }, void 0, false), modules.map(m => /*#__PURE__*/_jsxDEV("option", {
                  value: m,
                  children: m
                }, m, false))]
              }, void 0, true)]
            }, void 0, true)
          }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: [/*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Entity Package"
              }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
                value: genParams.modelPkg,
                onChange: e => upd('modelPkg', e.target.value),
                placeholder: "com.example.entity"
              }, void 0, false), pkgHistory.modelPkgs && pkgHistory.modelPkgs.length > 0 && /*#__PURE__*/_jsxDEV("div", {
                className: "pkg-history",
                children: pkgHistory.modelPkgs.map(p => /*#__PURE__*/_jsxDEV("span", {
                  className: "pkg-chip",
                  onClick: () => upd('modelPkg', p),
                  children: p
                }, p, false))
              }, void 0, false)]
            }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "DAO Package"
              }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
                value: genParams.mapperPkg,
                onChange: e => upd('mapperPkg', e.target.value),
                placeholder: "com.example.dao"
              }, void 0, false), pkgHistory.mapperPkgs && pkgHistory.mapperPkgs.length > 0 && /*#__PURE__*/_jsxDEV("div", {
                className: "pkg-history",
                children: pkgHistory.mapperPkgs.map(p => /*#__PURE__*/_jsxDEV("span", {
                  className: "pkg-chip",
                  onClick: () => upd('mapperPkg', p),
                  children: p
                }, p, false))
              }, void 0, false)]
            }, void 0, true)]
          }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: [/*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "\xA0"
              }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
                className: "check",
                children: [/*#__PURE__*/_jsxDEV("input", {
                  type: "checkbox",
                  checked: genParams.lombok,
                  onChange: e => upd('lombok', e.target.checked)
                }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                  children: "Use Lombok"
                }, void 0, false)]
              }, void 0, true)]
            }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "\xA0"
              }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
                className: "check",
                children: [/*#__PURE__*/_jsxDEV("input", {
                  type: "checkbox",
                  checked: genParams.hbmXml || false,
                  onChange: e => upd('hbmXml', e.target.checked)
                }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                  children: "Generate .hbm.xml"
                }, void 0, false)]
              }, void 0, true)]
            }, void 0, true)]
          }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: [/*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "\xA0"
              }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
                className: "check",
                children: [/*#__PURE__*/_jsxDEV("input", {
                  type: "checkbox",
                  checked: genParams.jpaRelations || false,
                  onChange: e => upd('jpaRelations', e.target.checked)
                }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                  children: "Generate Relations"
                }, void 0, false)]
              }, void 0, true)]
            }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "\xA0"
              }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
                className: "check",
                children: [/*#__PURE__*/_jsxDEV("input", {
                  type: "checkbox",
                  checked: genParams.hibernateCfg || false,
                  onChange: e => upd('hibernateCfg', e.target.checked)
                }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
                  children: "Generate hibernate.cfg.xml"
                }, void 0, false)]
              }, void 0, true)]
            }, void 0, true)]
          }, void 0, true)]
        }, void 0, true), subTab === 'output' && genParams.mode === 'yorm' && /*#__PURE__*/_jsxDEV("div", {
          className: "output-form",
          children: [modules.length > 0 && /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Target Module"
              }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
                value: genParams.targetModule || '',
                onChange: e => upd('targetModule', e.target.value),
                children: [/*#__PURE__*/_jsxDEV("option", {
                  value: "",
                  children: "Project root"
                }, void 0, false), modules.map(m => /*#__PURE__*/_jsxDEV("option", {
                  value: m,
                  children: m
                }, m, false))]
              }, void 0, true)]
            }, void 0, true)
          }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Record Package"
              }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
                value: genParams.modelPkg,
                onChange: e => upd('modelPkg', e.target.value),
                placeholder: "com.example.record"
              }, void 0, false), pkgHistory.modelPkgs && pkgHistory.modelPkgs.length > 0 && /*#__PURE__*/_jsxDEV("div", {
                className: "pkg-history",
                children: pkgHistory.modelPkgs.map(p => /*#__PURE__*/_jsxDEV("span", {
                  className: "pkg-chip",
                  onClick: () => upd('modelPkg', p),
                  children: p
                }, p, false))
              }, void 0, false)]
            }, void 0, true)
          }, void 0, false), /*#__PURE__*/_jsxDEV("p", {
            style: {
              fontSize: 11,
              color: 'var(--text)',
              marginTop: 8
            },
            children: "YORM uses Java Records with convention-based mapping (no annotations). Fields use camelCase, PK is assumed to be \"id\" with auto-increment."
          }, void 0, false)]
        }, void 0, true), subTab === 'output' && isJs && /*#__PURE__*/_jsxDEV("div", {
          className: "output-form",
          children: [/*#__PURE__*/_jsxDEV("div", {
            className: "row",
            children: /*#__PURE__*/_jsxDEV("div", {
              className: "fg",
              children: [/*#__PURE__*/_jsxDEV("label", {
                children: "Output Directory"
              }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
                value: genParams.modelPkg,
                onChange: e => upd('modelPkg', e.target.value),
                placeholder: "src/models"
              }, void 0, false), pkgHistory.modelPkgs && pkgHistory.modelPkgs.length > 0 && /*#__PURE__*/_jsxDEV("div", {
                className: "pkg-history",
                children: pkgHistory.modelPkgs.map(p => /*#__PURE__*/_jsxDEV("span", {
                  className: "pkg-chip",
                  onClick: () => upd('modelPkg', p),
                  children: p
                }, p, false))
              }, void 0, false)]
            }, void 0, true)
          }, void 0, false), /*#__PURE__*/_jsxDEV("p", {
            style: {
              fontSize: 11,
              color: 'var(--text)',
              marginTop: 8
            },
            children: ["Generates model definitions for the selected JavaScript ORM framework.", genParams.mode === 'prisma' && ' Output is a schema.prisma file.', genParams.mode === 'knexjs' && ' Output is a migration file.']
          }, void 0, true)]
        }, void 0, true)]
      }, void 0, true)
    }, void 0, false), !overwriteFiles && /*#__PURE__*/_jsxDEV("div", {
      style: {
        padding: '8px 0',
        flexShrink: 0
      },
      children: /*#__PURE__*/_jsxDEV("button", {
        className: "btn",
        onClick: onGenerate,
        children: ["⚡ Generate (", selectedTables.length, " tables)"]
      }, void 0, true)
    }, void 0, false)]
  }, void 0, true);
}

// --- History Panel ---
function HistoryPanel({
  history
}) {
  const [collapsed, setCollapsed] = useState({});
  if (!history.length) return /*#__PURE__*/_jsxDEV("div", {
    className: "empty",
    children: [/*#__PURE__*/_jsxDEV("div", {
      className: "ico",
      children: "📜"
    }, void 0, false), /*#__PURE__*/_jsxDEV("p", {
      children: "Generation history will appear here"
    }, void 0, false)]
  }, void 0, true);

  // Group by date + HH:mm
  const groups = {};
  history.forEach(h => {
    const ts = h.date ? h.date.substring(0, 16) : 'Unknown';
    if (!groups[ts]) groups[ts] = [];
    groups[ts].push(h);
  });
  const toggle = k => setCollapsed(p => ({
    ...p,
    [k]: !p[k]
  }));
  const regenerateTable = (h, table) => {
    callIde('generate', {
      modelPkg: h.modelPkg || '',
      mapperPkg: h.mapperPkg || '',
      xmlPkg: h.xmlPkg || '',
      runtime: h.runtime || '',
      tables: [table]
    });
  };
  return Object.entries(groups).map(([ts, items]) => /*#__PURE__*/_jsxDEV("div", {
    className: "schema-group",
    children: [/*#__PURE__*/_jsxDEV("div", {
      className: "schema-header",
      onClick: () => toggle(ts),
      children: [/*#__PURE__*/_jsxDEV("span", {
        className: "fold-icon",
        children: collapsed[ts] ? '▶' : '▼'
      }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
        children: "🕐"
      }, void 0, false), /*#__PURE__*/_jsxDEV("strong", {
        children: ts
      }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
        className: "schema-count",
        children: [items.reduce((n, h) => n + (h.tables?.length || 0), 0), " table(s)"]
      }, void 0, true)]
    }, void 0, true), !collapsed[ts] && items.map((h, i) => /*#__PURE__*/_jsxDEV("div", {
      children: (h.tables || []).map((t, j) => /*#__PURE__*/_jsxDEV("div", {
        className: "table-row",
        style: {
          display: 'flex',
          alignItems: 'center',
          padding: '3px 8px 3px 20px',
          gap: 6
        },
        children: [driverIcon(h.driverType), /*#__PURE__*/_jsxDEV("span", {
          style: {
            flex: 1,
            fontSize: 12,
            color: 'var(--bright)'
          },
          children: t
        }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
          style: {
            fontSize: 10,
            color: 'var(--text)',
            maxWidth: 90,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap'
          },
          children: h.modelPkg
        }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
          className: "btn",
          style: {
            padding: '1px 5px',
            fontSize: 9
          },
          onClick: () => regenerateTable(h, t),
          children: "↻"
        }, void 0, false)]
      }, j, true))
    }, i, false))]
  }, ts, true));
}

// --- Connection Modal ---
function ConnectionModal({
  conn,
  onSave,
  onTest,
  onClose
}) {
  const [form, setForm] = useState({
    ...conn
  });
  const upd = (k, v) => setForm(p => ({
    ...p,
    [k]: v
  }));
  const driver = DRIVERS.find(d => d.id === form.driverType) || DRIVERS[0];
  const onDriverChange = id => {
    const d = DRIVERS.find(x => x.id === id);
    upd('driverType', id);
    if (d) setForm(p => ({
      ...p,
      driverType: id,
      port: d.port
    }));
  };
  return /*#__PURE__*/_jsxDEV("div", {
    className: "modal-bg",
    onClick: onClose,
    children: /*#__PURE__*/_jsxDEV("div", {
      className: "modal",
      onClick: e => e.stopPropagation(),
      children: [/*#__PURE__*/_jsxDEV("h2", {
        children: conn.id ? 'Edit Connection' : 'New Connection'
      }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
        className: "fg",
        children: [/*#__PURE__*/_jsxDEV("label", {
          children: "Name"
        }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
          value: form.name,
          onChange: e => upd('name', e.target.value)
        }, void 0, false)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "fg",
        children: [/*#__PURE__*/_jsxDEV("label", {
          children: "Driver"
        }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
          value: form.driverType,
          onChange: e => onDriverChange(e.target.value),
          children: DRIVERS.map(d => /*#__PURE__*/_jsxDEV("option", {
            value: d.id,
            children: d.label
          }, d.id, false))
        }, void 0, false)]
      }, void 0, true), !driver.fileBased && /*#__PURE__*/_jsxDEV(_Fragment, {
        children: [/*#__PURE__*/_jsxDEV("div", {
          className: "row",
          children: [/*#__PURE__*/_jsxDEV("div", {
            className: "fg",
            children: [/*#__PURE__*/_jsxDEV("label", {
              children: "Host"
            }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
              value: form.host || '',
              onChange: e => upd('host', e.target.value)
            }, void 0, false)]
          }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
            className: "fg",
            style: {
              maxWidth: 80
            },
            children: [/*#__PURE__*/_jsxDEV("label", {
              children: "Port"
            }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
              type: "number",
              value: form.port || '',
              onChange: e => upd('port', parseInt(e.target.value) || 0)
            }, void 0, false)]
          }, void 0, true)]
        }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: form.driverType === 'Oracle_SID' ? 'SID' : form.driverType === 'Oracle_Service' ? 'Service Name' : 'Database'
          }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
            value: form.database || '',
            onChange: e => upd('database', e.target.value)
          }, void 0, false)]
        }, void 0, true)]
      }, void 0, true), driver.fileBased && /*#__PURE__*/_jsxDEV("div", {
        className: "fg",
        children: [/*#__PURE__*/_jsxDEV("label", {
          children: "File Path"
        }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
          value: form.database || '',
          onChange: e => upd('database', e.target.value),
          placeholder: "/path/to/db.sqlite"
        }, void 0, false)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "row",
        children: [/*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "User"
          }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
            value: form.userName || '',
            onChange: e => upd('userName', e.target.value)
          }, void 0, false)]
        }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "Password"
          }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
            type: "password",
            value: form.password || '',
            onChange: e => upd('password', e.target.value)
          }, void 0, false)]
        }, void 0, true)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "check",
        children: [/*#__PURE__*/_jsxDEV("input", {
          type: "checkbox",
          checked: form.active !== false,
          onChange: e => upd('active', e.target.checked)
        }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
          children: "Active"
        }, void 0, false)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "sep"
      }, void 0, false), /*#__PURE__*/_jsxDEV("h3", {
        style: {
          fontSize: 12,
          color: 'var(--bright)',
          marginBottom: 8
        },
        children: "Generation Overrides"
      }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
        className: "row",
        children: [/*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "Runtime"
          }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
            value: form.targetRuntime || '',
            onChange: e => upd('targetRuntime', e.target.value),
            children: [/*#__PURE__*/_jsxDEV("option", {
              value: "",
              children: "Default"
            }, void 0, false), RUNTIMES.map(r => /*#__PURE__*/_jsxDEV("option", {
              children: r
            }, r, false))]
          }, void 0, true)]
        }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "Client"
          }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
            value: form.clientType || '',
            onChange: e => upd('clientType', e.target.value),
            children: [/*#__PURE__*/_jsxDEV("option", {
              value: "",
              children: "Default"
            }, void 0, false), CLIENTS.map(c => /*#__PURE__*/_jsxDEV("option", {
              children: c
            }, c, false))]
          }, void 0, true)]
        }, void 0, true)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "row",
        children: [/*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "Model"
          }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
            value: form.modelType || '',
            onChange: e => upd('modelType', e.target.value),
            children: [/*#__PURE__*/_jsxDEV("option", {
              value: "",
              children: "Default"
            }, void 0, false), MODELS.map(m => /*#__PURE__*/_jsxDEV("option", {
              children: m
            }, m, false))]
          }, void 0, true)]
        }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "\xA0"
          }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
            className: "check",
            children: [/*#__PURE__*/_jsxDEV("input", {
              type: "checkbox",
              checked: form.useLombok || false,
              onChange: e => upd('useLombok', e.target.checked)
            }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
              children: "Lombok"
            }, void 0, false)]
          }, void 0, true)]
        }, void 0, true)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "modal-actions",
        children: [/*#__PURE__*/_jsxDEV("button", {
          className: "btn-s",
          onClick: () => onTest(form),
          children: "Test"
        }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
          className: "btn-s",
          onClick: onClose,
          children: "Cancel"
        }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
          className: "btn",
          onClick: () => onSave(form),
          children: "Save"
        }, void 0, false)]
      }, void 0, true)]
    }, void 0, true)
  }, void 0, false);
}

// --- Settings Modal ---
function SettingsModal({
  defaults,
  setDefaults,
  onSave,
  onClose
}) {
  const upd = (k, v) => setDefaults(p => ({
    ...p,
    [k]: v
  }));
  return /*#__PURE__*/_jsxDEV("div", {
    className: "modal-bg",
    onClick: onClose,
    children: /*#__PURE__*/_jsxDEV("div", {
      className: "modal",
      onClick: e => e.stopPropagation(),
      children: [/*#__PURE__*/_jsxDEV("h2", {
        children: "Global Settings"
      }, void 0, false), /*#__PURE__*/_jsxDEV("div", {
        className: "row",
        children: [/*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "Target Runtime"
          }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
            value: defaults.targetRuntime,
            onChange: e => upd('targetRuntime', e.target.value),
            children: RUNTIMES.map(r => /*#__PURE__*/_jsxDEV("option", {
              children: r
            }, r, false))
          }, void 0, false)]
        }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "Client Type"
          }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
            value: defaults.clientType,
            onChange: e => upd('clientType', e.target.value),
            children: CLIENTS.map(c => /*#__PURE__*/_jsxDEV("option", {
              children: c
            }, c, false))
          }, void 0, false)]
        }, void 0, true)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "row",
        children: [/*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "Model Type"
          }, void 0, false), /*#__PURE__*/_jsxDEV("select", {
            value: defaults.modelType,
            onChange: e => upd('modelType', e.target.value),
            children: MODELS.map(m => /*#__PURE__*/_jsxDEV("option", {
              children: m
            }, m, false))
          }, void 0, false)]
        }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
          className: "fg",
          children: [/*#__PURE__*/_jsxDEV("label", {
            children: "Encoding"
          }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
            value: defaults.encoding,
            onChange: e => upd('encoding', e.target.value)
          }, void 0, false)]
        }, void 0, true)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "fg",
        children: [/*#__PURE__*/_jsxDEV("label", {
          children: "Generated Comment"
        }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
          value: defaults.comment,
          onChange: e => upd('comment', e.target.value),
          placeholder: "Auto-generated by FastORM Builder"
        }, void 0, false)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "check",
        children: [/*#__PURE__*/_jsxDEV("input", {
          type: "checkbox",
          checked: defaults.forceBigDecimals,
          onChange: e => upd('forceBigDecimals', e.target.checked)
        }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
          children: "Force BigDecimals"
        }, void 0, false)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "check",
        children: [/*#__PURE__*/_jsxDEV("input", {
          type: "checkbox",
          checked: defaults.useJSR310,
          onChange: e => upd('useJSR310', e.target.checked)
        }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
          children: "Use JSR-310 Date Types"
        }, void 0, false)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "check",
        children: [/*#__PURE__*/_jsxDEV("input", {
          type: "checkbox",
          checked: defaults.useLombok,
          onChange: e => upd('useLombok', e.target.checked)
        }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
          children: "Use Lombok"
        }, void 0, false)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "check",
        children: [/*#__PURE__*/_jsxDEV("input", {
          type: "checkbox",
          checked: defaults.useGeneratedAnnotation,
          onChange: e => upd('useGeneratedAnnotation', e.target.checked)
        }, void 0, false), /*#__PURE__*/_jsxDEV("span", {
          children: "Add @Generated annotation (MyBatis)"
        }, void 0, false)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "fg",
        children: [/*#__PURE__*/_jsxDEV("label", {
          children: "History Size"
        }, void 0, false), /*#__PURE__*/_jsxDEV("input", {
          type: "number",
          value: defaults.historySize,
          onChange: e => upd('historySize', parseInt(e.target.value) || 20),
          style: {
            width: 60
          }
        }, void 0, false)]
      }, void 0, true), /*#__PURE__*/_jsxDEV("div", {
        className: "modal-actions",
        children: [/*#__PURE__*/_jsxDEV("button", {
          className: "btn-s",
          onClick: onClose,
          children: "Cancel"
        }, void 0, false), /*#__PURE__*/_jsxDEV("button", {
          className: "btn",
          onClick: onSave,
          children: "Save"
        }, void 0, false)]
      }, void 0, true)]
    }, void 0, true)
  }, void 0, false);
}
ReactDOM.createRoot(document.getElementById('root')).render(/*#__PURE__*/_jsxDEV(App, {}, void 0, false));