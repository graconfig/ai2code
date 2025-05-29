package customer.ai2code.model;

import cds.gen.configservice.ModelConfigs;

public interface AIModel {
    // private final ModelConfigs modelConfigs;

    // private Map<String, E extends Object> modelParametersMap;
    public String getModelName();
    // public Map<String, Object> getModelParametersMap();
    public void parseModelConfigs(ModelConfigs modelConfigs);
}
