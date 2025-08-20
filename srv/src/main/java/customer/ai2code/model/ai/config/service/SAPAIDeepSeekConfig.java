package customer.ai2code.model.ai.config.service;

import customer.ai2code.model.ai.config.auth.AIAuthConfig;
import customer.ai2code.model.ai.config.auth.SAPApiKeyConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAIDeepSeekConfig implements AIServiceConfig {
    private String apiUrl; // DeepSeek API地址
    private String apiKey; // 认证密钥
    private String model;  // 模型名称

    @Override
    public String getServiceType() {
        return "DEEPSEEK";
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public AIAuthConfig getAuthConfig() {
        return new SAPApiKeyConfig(apiKey);
    }

    @Override
    public boolean isValid() {
        return apiUrl != null && !apiUrl.isEmpty() 
                && apiKey != null && !apiKey.isEmpty()
                && model != null && !model.isEmpty();
    }
}