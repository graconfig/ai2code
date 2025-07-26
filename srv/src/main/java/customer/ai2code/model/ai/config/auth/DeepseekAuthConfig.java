package customer.ai2code.model.ai.config.auth;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

// Deepseek认证实现（仅保留必要认证逻辑）
@Data
@AllArgsConstructor
public class DeepseekAuthConfig implements AIAuthConfig {
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
