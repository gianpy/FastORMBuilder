package org.fastormbuilder.plugin.generator;

import org.fastormbuilder.plugin.generator.callback.IndicatorCallback;
import org.fastormbuilder.plugin.model.ConnectionProfile;
import org.fastormbuilder.plugin.model.DriverType;
import org.fastormbuilder.plugin.model.TableSpec;
import org.fastormbuilder.plugin.util.TextUtils;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.*;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GeneratorRunner {

    public static List<String> runWithConfig(String configPath, Properties props, IndicatorCallback callback) throws Exception {
        List<String> warnings = new ArrayList<>();
        ConfigurationParser parser = new ConfigurationParser(props, warnings);
        Configuration config = parser.parseConfiguration(new File(configPath));
        DefaultShellCallback shellCallback = new DefaultShellCallback(true);
        MyBatisGenerator generator = new MyBatisGenerator(config, shellCallback, warnings);
        generator.generate(callback);
        return warnings;
    }

    public static List<String> generate(GenerationParams params, IndicatorCallback callback) throws Exception {
        List<String> warnings = new ArrayList<>();
        Configuration config = buildConfiguration(params);
        DefaultShellCallback shellCallback = new DefaultShellCallback(true);
        MyBatisGenerator generator = new MyBatisGenerator(config, shellCallback, warnings);
        generator.generate(callback);
        return warnings;
    }

    private static Configuration buildConfiguration(GenerationParams params) {
        Configuration config = new Configuration();
        Defaults defaults = params.getDefaultParameters();
        ConnectionProfile connProfile = params.getConnectionProfile();

        // Resolve per-connection overrides (connection > global defaults)
        String runtime = resolve(connProfile != null ? connProfile.getTargetRuntime() : null,
                params.getTargetRuntime(), defaults.getTargetRuntime());
        String clientType = resolve(connProfile != null ? connProfile.getClientType() : null,
                null, defaults.getClientType());
        boolean useLombok = connProfile != null && connProfile.getUseLombok() != null
                ? connProfile.getUseLombok() : Boolean.TRUE.equals(defaults.getUseLombok());
        ModelType modelType = defaults.getDefaultModelType();
        if (connProfile != null && connProfile.getModelType() != null) {
            try {
                modelType = ModelType.valueOf(connProfile.getModelType());
            } catch (Exception ignored) {
            }
        }

        // Driver library classpath
        if (TextUtils.hasValue(params.getDriverLibrary())) {
            config.addClasspathEntry(params.getDriverLibrary());
        }

        // Context
        Context context = new Context(modelType);
        context.setId("FastBuilderContext");
        context.setTargetRuntime(runtime);
        config.addContext(context);

        // Delimiters
        if (TextUtils.hasValue(params.getBeginningDelimiter())) {
            context.addProperty("beginningDelimiter", params.getBeginningDelimiter());
        }
        if (TextUtils.hasValue(params.getEndingDelimiter())) {
            context.addProperty("endingDelimiter", params.getEndingDelimiter());
        }

        // JDBC connection
        JDBCConnectionConfiguration jdbc = params.getJdbcConfig();
        context.setJdbcConnectionConfiguration(jdbc);

        // Java model generator
        JavaModelGeneratorConfiguration modelConfig = params.getJavaModelConfig();
        if (Boolean.TRUE.equals(params.getTrimStrings())) {
            modelConfig.addProperty("trimStrings", "true");
        }
        context.setJavaModelGeneratorConfiguration(modelConfig);

        // Java client generator
        boolean isDynamicSql = "MyBatis3DynamicSql".equals(runtime);
        JavaClientGeneratorConfiguration clientConfig = params.getJavaClientConfig();
        if (clientConfig.getConfigurationType() == null) {
            clientConfig.setConfigurationType(clientType != null ? clientType : "XMLMAPPER");
        }
        context.setJavaClientGeneratorConfiguration(clientConfig);

        // SQL map generator (not for DynamicSql or ANNOTATEDMAPPER)
        if (!isDynamicSql && !"ANNOTATEDMAPPER".equals(clientConfig.getConfigurationType())) {
            context.setSqlMapGeneratorConfiguration(params.getSqlMapConfig());
        }

        // Comment generator
        CommentGeneratorConfiguration commentConfig = new CommentGeneratorConfiguration();
        commentConfig.addProperty("suppressAllComments", "false");
        commentConfig.addProperty("suppressDate", "true");
        commentConfig.addProperty("addRemarkComments", "true");
        if (!Boolean.TRUE.equals(defaults.getUseGeneratedAnnotation())) {
            commentConfig.addProperty("suppressAnnotation", "true");
        }
        context.setCommentGeneratorConfiguration(commentConfig);

        // JavaType resolver
        JavaTypeResolverConfiguration typeResolver = new JavaTypeResolverConfiguration();
        typeResolver.addProperty("forceBigDecimals", String.valueOf(defaults.getForceBigDecimals()));
        typeResolver.addProperty("useJSR310Types", String.valueOf(defaults.getUseJSR310Types()));
        context.setJavaTypeResolverConfiguration(typeResolver);

        // Lombok plugin
        if (useLombok) {
            PluginConfiguration lombokPlugin = new PluginConfiguration();
            lombokPlugin.setConfigurationType("org.fastormbuilder.plugin.generator.plugins.LombokPlugin");
            context.addPluginConfiguration(lombokPlugin);
        }

        // Tables
        TableConfigWrapper tableDefaults = params.getDefaultTableConfig();
        boolean useSchema = connProfile != null && (connProfile.getDriverType() == DriverType.PostgreSQL
                || connProfile.getDriverType() == DriverType.Oracle_SID
                || connProfile.getDriverType() == DriverType.Oracle_Service);
        for (TableSpec table : params.getSelectedTables()) {
            TableConfiguration tc = new TableConfiguration(context);
            if (useSchema) {
                tc.setSchema(table.getDatabase());
            } else {
                tc.setCatalog(table.getDatabase());
            }
            tc.setTableName(table.getTableName());
            if (TextUtils.hasValue(table.getDomainName())) {
                tc.setDomainObjectName(table.getDomainName());
            }
            tc.setInsertStatementEnabled(tableDefaults.isInsertStatementEnabled());
            tc.setUpdateByPrimaryKeyStatementEnabled(tableDefaults.isUpdateByPrimaryKeyStatementEnabled());
            tc.setSelectByPrimaryKeyStatementEnabled(tableDefaults.isSelectByPrimaryKeyStatementEnabled());
            tc.setDeleteByPrimaryKeyStatementEnabled(tableDefaults.isDeleteByPrimaryKeyStatementEnabled());
            tc.setSelectByExampleStatementEnabled(tableDefaults.isSelectByExampleStatementEnabled());
            tc.setCountByExampleStatementEnabled(tableDefaults.isCountByExampleStatementEnabled());
            tc.setUpdateByExampleStatementEnabled(tableDefaults.isUpdateByExampleStatementEnabled());
            tc.setDeleteByExampleStatementEnabled(tableDefaults.isDeleteByExampleStatementEnabled());

            // Generated key
            GeneratedKeySpec keySpec = tableDefaults.getGeneratedKeySpec();
            if (TextUtils.hasValue(table.getKeyColumn()) || TextUtils.hasValue(keySpec.getColumn())) {
                String col = TextUtils.hasValue(table.getKeyColumn()) ? table.getKeyColumn() : keySpec.getColumn();
                GeneratedKey gk = new GeneratedKey(col,
                        TextUtils.hasValue(keySpec.getStatement()) ? keySpec.getStatement() : "JDBC",
                        keySpec.isIdentity(), null);
                tc.setGeneratedKey(gk);
            }

            if (Boolean.TRUE.equals(params.getDatabaseRemark())) {
                tc.addProperty("useActualColumnNames", "false");
            }

            context.addTableConfiguration(tc);
        }

        return config;
    }

    private static String resolve(String connOverride, String paramValue, String defaultValue) {
        if (TextUtils.hasValue(connOverride)) return connOverride;
        if (TextUtils.hasValue(paramValue)) return paramValue;
        return defaultValue;
    }
}
