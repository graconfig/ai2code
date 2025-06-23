package customer.ai2code.model.config;

import cds.gen.configservice.ModelConfigs;
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
        ObjectMapper mapper = getDefaultObjectMapper();
        SAPAICoreClaudeConfig claudeConfig = 
            mapper.convertValue(modelConfigs.getParameters(), SAPAICoreClaudeConfig.class);
        return claudeConfig;
    }

    
}