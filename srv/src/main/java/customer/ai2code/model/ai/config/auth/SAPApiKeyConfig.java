package customer.ai2code.model.ai.config.auth;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DeepSeek API Key认证配置
 */
@Data
@AllArgsConstructor
public class SAPApiKeyConfig implements AIAuthConfig {
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