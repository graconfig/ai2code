package customer.ai2code.model.ai.config.model;

import cds.gen.configservice.ModelConfigs;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.service.AIServiceConfig;
import customer.ai2code.model.ai.config.service.SAPAICoreClaudeConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import static com.sap.ai.sdk.core.JacksonConfiguration.getDefaultObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAICoreClaudeAI35Sonnet implements AIModel {
    private ModelConfigs modelConfigs;

    @Override
    public String getModelName() {
        return modelConfigs.getModelName(); // 如"claude-3.5-sonnet"
    }

    @Override
    public AIServiceConfig parseModelConfigs() {
        try {
            ObjectMapper mapper = getDefaultObjectMapper();
            
            Object parameters = modelConfigs.getParameters();
            if (parameters instanceof String) {
                return mapper.readValue((String) parameters, SAPAICoreClaudeConfig.class);
            } else {
                return mapper.convertValue(parameters, SAPAICoreClaudeConfig.class);
            }
        } catch (Exception e) {
            throw new BusinessException("Failed to parse model configuration", e);
        }
    }

    
}