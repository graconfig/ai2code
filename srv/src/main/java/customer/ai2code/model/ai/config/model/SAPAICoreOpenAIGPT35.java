package customer.ai2code.model.ai.config.model;

import cds.gen.configservice.ModelConfigs;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.service.AIServiceConfig;
import customer.ai2code.model.ai.config.service.SAPAICoreConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import static com.sap.ai.sdk.core.JacksonConfiguration.getDefaultObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAICoreOpenAIGPT35 implements AIModel {
    private ModelConfigs modelConfigs; 

    @Override
    public String getModelName() {
        return modelConfigs.getModelName(); 
    }

    @Override
    public AIServiceConfig parseModelConfigs() {
        try {
            ObjectMapper mapper = getDefaultObjectMapper();
            
            Object parameters = modelConfigs.getParameters();
            if (parameters instanceof String) {
                return mapper.readValue((String) parameters, SAPAICoreConfig.class);
            } else {
                return mapper.convertValue(parameters, SAPAICoreConfig.class);
            }
        } catch (Exception e) {
            throw new BusinessException("Failed to parse model configuration", e);
        }
    }
    // @Override
    // public ModelConfigs getModelConfigs() {
    //     return modelConfigs;
    // }
}