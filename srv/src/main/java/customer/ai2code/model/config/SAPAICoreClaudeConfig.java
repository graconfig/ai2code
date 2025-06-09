package main.java.customer.ai2code.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.HashMap;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAICoreClaudeConfig implements AIServiceConfig {
    
    @JsonProperty("api_url")
    private String apiUrl;
    
    @JsonProperty("api_key")
    private String apiKey;
    
    @Override
    public String getServiceType() {
        return "SAP_AI_CORE_CLAUDE";
    }

    @Override
    public String getApiUrl() {
        return apiUrl != null ? apiUrl : "";
    }

    @Override
    public AIAuthConfig getAuthConfig() {
        return new ClaudeApiKeyAuthConfig(apiKey);
    }

    @Override
    public boolean isValid() {
        return apiKey != null && !apiKey.isEmpty() &&
               apiUrl != null && !apiUrl.isEmpty();
    }

    /**
     * Claude API Key认证配置
     */
    @Data
    @AllArgsConstructor
    public static class ClaudeApiKeyAuthConfig implements AIAuthConfig {
        private final String apiKey;

        @Override
        public AuthType getAuthType() {
            return AuthType.API_KEY;
        }

        @Override
        public Map<String, String> getAuthProperties() {
            Map<String, String> props = new HashMap<>();
            props.put("apiKey", apiKey);
            return props;
        }
    }
}