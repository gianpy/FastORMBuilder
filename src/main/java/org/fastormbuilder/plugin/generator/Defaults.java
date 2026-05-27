package org.fastormbuilder.plugin.generator;

import org.fastormbuilder.plugin.generator.plugins.AnnotationPlugin;
import org.fastormbuilder.plugin.generator.plugins.NamingPlugin;
import org.fastormbuilder.plugin.generator.plugins.selectwithlock.LockQueryConfig;
import org.mybatis.generator.config.ModelType;

public class Defaults {
    private ModelType defaultModelType = ModelType.FLAT;
    private String targetRuntime = "MyBatis3DynamicSql";
    private String clientType = "XMLMAPPER";
    private String javaFileEncoding = "UTF-8";
    private Boolean forceBigDecimals = true;
    private Boolean useJSR310Types = false;
    private Boolean useLombok = false;
    private Boolean useGeneratedAnnotation = false;
    private String generatedComment = "generated automatically, do not modify!";
    private Integer historySize = 10;

    private AnnotationPlugin.Config mapperAnnotationConfig = new AnnotationPlugin.Config("org.springframework.stereotype.Repository");
    private LockQueryConfig selectWithLockConfig = new LockQueryConfig();
    private NamingPlugin.Config renameConfig = new NamingPlugin.Config();

    public ModelType getDefaultModelType() {
        return defaultModelType;
    }

    public void setDefaultModelType(ModelType defaultModelType) {
        this.defaultModelType = defaultModelType;
    }

    public String getTargetRuntime() {
        return targetRuntime;
    }

    public void setTargetRuntime(String targetRuntime) {
        this.targetRuntime = targetRuntime;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getJavaFileEncoding() {
        return javaFileEncoding;
    }

    public void setJavaFileEncoding(String javaFileEncoding) {
        this.javaFileEncoding = javaFileEncoding;
    }

    public String getGeneratedComment() {
        return generatedComment;
    }

    public void setGeneratedComment(String generatedComment) {
        this.generatedComment = generatedComment;
    }

    public Boolean getForceBigDecimals() {
        return forceBigDecimals;
    }

    public void setForceBigDecimals(Boolean forceBigDecimals) {
        this.forceBigDecimals = forceBigDecimals;
    }

    public Boolean getUseJSR310Types() {
        return useJSR310Types;
    }

    public void setUseJSR310Types(Boolean useJSR310Types) {
        this.useJSR310Types = useJSR310Types;
    }

    public Boolean getUseLombok() {
        return useLombok;
    }

    public void setUseLombok(Boolean useLombok) {
        this.useLombok = useLombok;
    }

    public Boolean getUseGeneratedAnnotation() {
        return useGeneratedAnnotation;
    }

    public void setUseGeneratedAnnotation(Boolean useGeneratedAnnotation) {
        this.useGeneratedAnnotation = useGeneratedAnnotation;
    }

    public Integer getHistorySize() {
        return historySize;
    }

    public void setHistorySize(Integer historySize) {
        this.historySize = historySize;
    }

    public AnnotationPlugin.Config getMapperAnnotationConfig() {
        return mapperAnnotationConfig;
    }

    public void setMapperAnnotationConfig(AnnotationPlugin.Config mapperAnnotationConfig) {
        this.mapperAnnotationConfig = mapperAnnotationConfig;
    }

    public LockQueryConfig getSelectWithLockConfig() {
        return selectWithLockConfig;
    }

    public void setSelectWithLockConfig(LockQueryConfig selectWithLockConfig) {
        this.selectWithLockConfig = selectWithLockConfig;
    }

    public NamingPlugin.Config getRenameConfig() {
        return renameConfig;
    }

    public void setRenameConfig(NamingPlugin.Config renameConfig) {
        this.renameConfig = renameConfig;
    }
}
