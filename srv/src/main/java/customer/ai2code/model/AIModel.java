package customer.ai2code.model;

import cds.gen.configservice.ModelConfigs;
// import cds.gen.configservice.ModelConfigs;
// import cds.gen.configservice.ModelConfigs;
import customer.ai2code.model.config.AIServiceConfig;
import customer.ai2code.service.AIService;

public interface AIModel {
    // public ModelConfigs modelConfigs;

    // private Map<String, E extends Object> modelParametersMap;
    public String getModelName();
    // public Map<String, Object> getModelParametersMap();
    public AIServiceConfig parseModelConfigs();

    public ModelConfigs getModelConfigs();
}
