package customer.ai2code.model;

import cds.gen.configservice.ModelConfigs;
import customer.ai2code.model.config.AIServiceConfig;
import customer.ai2code.model.config.SAPAICoreConfig;
import customer.ai2code.service.AIService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import static com.sap.ai.sdk.core.JacksonConfiguration.getDefaultObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAICoreOpenAIgpt4o implements AIModel {

    private ModelConfigs modelConfigs;

    @Override
    public String getModelName() {
        return modelConfigs.getModelName();
    }

    @Override
    public AIServiceConfig parseModelConfigs() {
        try {
            ObjectMapper mapper = getDefaultObjectMapper();
            
            // Check if parameters is a String (JSON) or already an Object
            Object parameters = modelConfigs.getParameters();
            if (parameters instanceof String) {
                // Parse JSON string to SAPAICoreConfig
                return mapper.readValue((String) parameters, SAPAICoreConfig.class);
            } else {
                // Convert Object to SAPAICoreConfig
                return mapper.convertValue(parameters, SAPAICoreConfig.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse model configuration", e);
        }
    }
}
