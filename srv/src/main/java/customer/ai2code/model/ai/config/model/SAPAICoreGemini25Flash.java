package customer.ai2code.model.ai.config.model;

import cds.gen.configservice.ModelConfigs;
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
public class SAPAICoreGemini25Flash implements AIModel {
    private ModelConfigs modelConfigs;

    @Override
    public String getModelName() {
        return modelConfigs.getModelName(); // 如"gemini-2.5-flash"
    }

    @Override
    public AIServiceConfig parseModelConfigs() {
        ObjectMapper mapper = getDefaultObjectMapper();
        SAPAICoreConfig claudeConfig =
            mapper.convertValue(modelConfigs.getParameters(), SAPAICoreConfig.class);
        return claudeConfig;
    }

    
}