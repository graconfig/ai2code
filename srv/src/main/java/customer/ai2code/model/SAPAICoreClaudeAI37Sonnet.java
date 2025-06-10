package customer.ai2code.model;

import cds.gen.configservice.ModelConfigs;
import customer.ai2code.model.config.AIServiceConfig;
import customer.ai2code.model.config.SAPAICoreClaudeConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import static com.sap.ai.sdk.core.JacksonConfiguration.getDefaultObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAICoreClaudeAI37Sonnet implements AIModel {
    private ModelConfigs modelConfigs;

    @Override
    public String getModelName() {
        return modelConfigs.getModelName();
    }

    @Override
    public AIServiceConfig parseModelConfigs() {
        ObjectMapper mapper = getDefaultObjectMapper();
        SAPAICoreClaudeConfig sapAiCoreclaudeConfig = mapper.convertValue(modelConfigs.getParameters(),
                SAPAICoreClaudeConfig.class);
        return sapAiCoreclaudeConfig;
    }

}