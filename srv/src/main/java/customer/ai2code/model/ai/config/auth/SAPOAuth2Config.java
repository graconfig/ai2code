package customer.ai2code.model.ai.config.auth;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * SAP OAuth2 认证配置
 */
@Data
@AllArgsConstructor
public class SAPOAuth2Config implements AIAuthConfig {
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;

    @Override
    public AuthType getAuthType() {
        return AuthType.OAUTH2;
    }

    @Override
    public Map<String, String> getAuthProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("clientId", clientId);
        props.put("clientSecret", clientSecret);
        props.put("tokenUrl", tokenUrl);
        return props;
    }
}