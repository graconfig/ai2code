package customer.ai2code.model.ai.config.service;

import customer.ai2code.model.ai.config.auth.AIAuthConfig;
import customer.ai2code.model.ai.config.auth.DeepseekAuthConfig;
import lombok.Data;

// Deepseek服务配置实现（保留核心参数）
@Data
public class DeepseekServiceConfig implements AIServiceConfig {
    private String apiUrl;
    private String apiKey;
    private Integer maxTokens = 4096;
    private Double temperature = 0.7;
    private Boolean enableThinking = false; // Deepseek专属参数

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
        return new DeepseekAuthConfig(apiKey); // 关联认证配置
    }

    @Override
    public boolean isValid() {
        return apiUrl != null && apiKey != null; // 核心参数校验
    }
}
