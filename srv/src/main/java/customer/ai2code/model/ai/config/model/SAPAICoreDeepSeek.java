package customer.ai2code.model.ai.config.model;

import cds.gen.configservice.ModelConfigs;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.service.AIServiceConfig;
import customer.ai2code.model.ai.config.service.SAPAIDeepSeekConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAICoreDeepSeek implements AIModel {
    private ModelConfigs modelConfigs;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getModelName() {
        return modelConfigs.getModelName(); // 如"deepseek-chat"
    }

    @Override
    public AIServiceConfig parseModelConfigs() {
        try {
            // 解析模型配置参数为DeepSeek服务配置
            if (modelConfigs.getParameters() instanceof String) {
                return mapper.readValue((String) modelConfigs.getParameters(), SAPAIDeepSeekConfig.class);
            } else {
                return mapper.convertValue(modelConfigs.getParameters(), SAPAIDeepSeekConfig.class);
            }
        } catch (Exception e) {
            throw new BusinessException("Failed to parse DeepSeek config", e);
        }
    }

    @Override
    public ModelConfigs getModelConfigs() {
        return modelConfigs;
    }
}