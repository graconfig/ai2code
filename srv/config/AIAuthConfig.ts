/**
 * AI服务认证配置接口
 */
export interface AIAuthConfig {
    /**
     * 获取认证类型
     */
    getAuthType(): AuthType;
    
    /**
     * 获取认证配置的Map表示
     */
    getAuthProperties(): Record<string, string>;
}

/**
 * 认证类型枚举
 */
export enum AuthType {
    OAUTH2 = 'OAUTH2',
    API_KEY = 'API_KEY',
    BEARER_TOKEN = 'BEARER_TOKEN',
    BASIC_AUTH = 'BASIC_AUTH'
}