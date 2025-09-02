package customer.ai2code.model.ai.config.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import customer.ai2code.model.ai.config.auth.AIAuthConfig;
import customer.ai2code.model.ai.config.auth.SAPApiKeyConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAIDeepSeekConfig implements AIServiceConfig {
    
    @JsonProperty("apiurl")  
    @Nonnull
    private String apiUrl;  // DeepSeek API地址
    
    @JsonProperty("apikey")
    @Nonnull
    private String apiKey;  // 认证密钥
    
    @JsonProperty("model")
    @Nonnull
    private String model;   // 模型名称

    @Override
    public String getServiceType() {
        return "DEEPSEEK";
    }

    @Override
    public String getApiUrl() {
        return apiUrl != null ? apiUrl : "";
    }

    @Override
    public AIAuthConfig getAuthConfig() {
        return new SAPApiKeyConfig(apiKey);
    }

    @Override
    public boolean isValid() {
        return !getApiUrl().isEmpty() && 
               apiKey != null && !apiKey.isEmpty() && 
               model != null && !model.isEmpty();
    }
}
