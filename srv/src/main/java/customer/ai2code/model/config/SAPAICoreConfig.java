package customer.ai2code.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * SAP AI Core 服务配置实现
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SAPAICoreConfig implements AIServiceConfig {
    
    @JsonProperty("serviceurls")
    @Nonnull
    private ServiceUrls serviceUrls;
    
    @JsonProperty("appname")
    @Nullable
    private String appName;
    
    @JsonProperty("clientid")
    @Nonnull
    private String clientId;
    
    @JsonProperty("clientsecret")
    @Nonnull
    private String clientSecret;
    
    @JsonProperty("identityzone")
    @Nullable
    private String identityZone;
    
    @JsonProperty("identityzoneid")
    @Nullable
    private String identityZoneId;
    
    @JsonProperty("url")
    @Nonnull
    private String url;
    
    @JsonProperty("credential-type")
    @Nullable
    private String credentialType;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceUrls {
        @JsonProperty("AI_API_URL")
        @Nullable
        private String aiApiUrl;
        
        @Nonnull
        public String getAiApiUrl() {
            return aiApiUrl != null ? aiApiUrl : "";
        }
    }
    
    @Override
    public String getServiceType() {
        return "SAP_AI_CORE";
    }
    
    @Override
    public String getApiUrl() {
        return serviceUrls != null ? serviceUrls.getAiApiUrl() : "";
    }
    
    @Override
    public AIAuthConfig getAuthConfig() {
        return new SAPOAuth2Config(clientId, clientSecret, url);
    }
    
    @Override
    public boolean isValid() {
        return clientId != null && !clientId.isEmpty() &&
               clientSecret != null && !clientSecret.isEmpty() &&
               url != null && !url.isEmpty() &&
               getApiUrl() != null && !getApiUrl().isEmpty();
    }
    
    @Nonnull
    public String getTokenUrl() {
        return url != null ? url + "/oauth/token" : "";
    }
    
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
}