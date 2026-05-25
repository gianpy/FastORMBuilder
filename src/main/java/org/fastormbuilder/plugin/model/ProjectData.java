package org.fastormbuilder.plugin.model;

import org.fastormbuilder.plugin.generator.GenerationParams;
import org.fastormbuilder.plugin.generator.Defaults;

import java.util.*;

public class ProjectData {
    private List<ConnectionProfile> connectionInfoList = new ArrayList<>();
    private Defaults defaultParameters = new Defaults();
    private GenerationParams lastGenerationParams = new GenerationParams();
    private Map<String, GenerationParams> stashMap = new LinkedHashMap<>(16, 0.75f, true);
    private Map<String, TableSpec> tableInfoMap = new LinkedHashMap<>();
    private Map<String, List<String>> historyMap = new HashMap<>();

    public List<ConnectionProfile> getConnectionInfoList() { return connectionInfoList; }
    public void setConnectionInfoList(List<ConnectionProfile> connectionInfoList) { this.connectionInfoList = connectionInfoList; }
    public Defaults getDefaultParameters() { return defaultParameters; }
    public void setDefaultParameters(Defaults defaultParameters) { this.defaultParameters = defaultParameters; }
    public GenerationParams getLastGenerationParams() { return lastGenerationParams; }
    public void setLastGenerationParams(GenerationParams lastGenerationParams) { this.lastGenerationParams = lastGenerationParams; }
    public Map<String, GenerationParams> getStashMap() { return stashMap; }
    public void setStashMap(Map<String, GenerationParams> stashMap) { this.stashMap = stashMap; }
    public Map<String, TableSpec> getTableInfoMap() { return tableInfoMap; }
    public void setTableInfoMap(Map<String, TableSpec> tableInfoMap) { this.tableInfoMap = tableInfoMap; }
    public Map<String, List<String>> getHistoryMap() { return historyMap; }
    public void setHistoryMap(Map<String, List<String>> historyMap) { this.historyMap = historyMap; }
}
