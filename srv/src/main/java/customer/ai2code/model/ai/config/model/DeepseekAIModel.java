package customer.ai2code.model.ai.config.model;

import cds.gen.configservice.ModelConfigs;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.ai.config.service.AIServiceConfig;
import customer.ai2code.model.ai.config.service.DeepseekServiceConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.databind.ObjectMapper;

// Deepseek模型实现（保留核心配置解析）
@Data
@AllArgsConstructor
public class DeepseekAIModel implements AIModel {
    private ModelConfigs modelConfigs;

    @Override
    public String getModelName() {
        return modelConfigs.getModelName();
    }

    @Override
    public AIServiceConfig parseModelConfigs() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            DeepseekServiceConfig config = mapper.convertValue(
                modelConfigs.getParameters(), 
                DeepseekServiceConfig.class
            );
            config.setEnableThinking(false); // 保留Deepseek专属参数
            return config;
        } catch (Exception e) {
            throw new BusinessException("Deepseek模型配置解析失败", e);
        }
    }
}
