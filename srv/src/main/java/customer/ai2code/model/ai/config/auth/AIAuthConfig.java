package customer.ai2code.model.ai.config.auth;

/**
 * AI服务认证配置接口
 */
public interface AIAuthConfig {
    
    /**
     * 获取认证类型
     */
    AuthType getAuthType();
    
    /**
     * 获取认证配置的Map表示
     */
    java.util.Map<String, String> getAuthProperties();
    
    enum AuthType {
        OAUTH2,
        API_KEY,
        BEARER_TOKEN,
        BASIC_AUTH
    }
}