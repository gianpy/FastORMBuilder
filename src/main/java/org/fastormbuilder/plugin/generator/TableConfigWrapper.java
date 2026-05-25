package org.fastormbuilder.plugin.generator;

import org.mybatis.generator.config.DomainObjectRenamingRule;

public class TableConfigWrapper {
    private boolean insertStatementEnabled = true;
    private boolean updateByPrimaryKeyStatementEnabled = true;
    private boolean selectByPrimaryKeyStatementEnabled = true;
    private boolean deleteByPrimaryKeyStatementEnabled = true;
    private boolean selectByExampleStatementEnabled = true;
    private boolean countByExampleStatementEnabled = true;
    private boolean updateByExampleStatementEnabled = true;
    private boolean deleteByExampleStatementEnabled = true;
    private GeneratedKeySpec generatedKeySpec = new GeneratedKeySpec();
    private DomainObjectRenamingRule domainObjectRenamingRule = new DomainObjectRenamingRule();

    public boolean isInsertStatementEnabled() { return insertStatementEnabled; }
    public void setInsertStatementEnabled(boolean v) { this.insertStatementEnabled = v; }
    public boolean isUpdateByPrimaryKeyStatementEnabled() { return updateByPrimaryKeyStatementEnabled; }
    public void setUpdateByPrimaryKeyStatementEnabled(boolean v) { this.updateByPrimaryKeyStatementEnabled = v; }
    public boolean isSelectByPrimaryKeyStatementEnabled() { return selectByPrimaryKeyStatementEnabled; }
    public void setSelectByPrimaryKeyStatementEnabled(boolean v) { this.selectByPrimaryKeyStatementEnabled = v; }
    public boolean isDeleteByPrimaryKeyStatementEnabled() { return deleteByPrimaryKeyStatementEnabled; }
    public void setDeleteByPrimaryKeyStatementEnabled(boolean v) { this.deleteByPrimaryKeyStatementEnabled = v; }
    public boolean isSelectByExampleStatementEnabled() { return selectByExampleStatementEnabled; }
    public void setSelectByExampleStatementEnabled(boolean v) { this.selectByExampleStatementEnabled = v; }
    public boolean isCountByExampleStatementEnabled() { return countByExampleStatementEnabled; }
    public void setCountByExampleStatementEnabled(boolean v) { this.countByExampleStatementEnabled = v; }
    public boolean isUpdateByExampleStatementEnabled() { return updateByExampleStatementEnabled; }
    public void setUpdateByExampleStatementEnabled(boolean v) { this.updateByExampleStatementEnabled = v; }
    public boolean isDeleteByExampleStatementEnabled() { return deleteByExampleStatementEnabled; }
    public void setDeleteByExampleStatementEnabled(boolean v) { this.deleteByExampleStatementEnabled = v; }
    public GeneratedKeySpec getGeneratedKeySpec() { return generatedKeySpec; }
    public void setGeneratedKeySpec(GeneratedKeySpec v) { this.generatedKeySpec = v; }
    public DomainObjectRenamingRule getDomainObjectRenamingRule() { return domainObjectRenamingRule; }
    public void setDomainObjectRenamingRule(DomainObjectRenamingRule v) { this.domainObjectRenamingRule = v; }
}
