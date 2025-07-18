package customer.ai2code.model.ai.config.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import customer.ai2code.model.ai.config.auth.AIAuthConfig;
import customer.ai2code.model.ai.config.auth.SAPOAuth2Config;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

}